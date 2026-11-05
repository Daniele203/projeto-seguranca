package com.gateway.config;

import com.gateway.exception.ApiErrorWriter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final ApiErrorWriter errors;
    public RateLimitingFilter(ApiErrorWriter errors) { this.errors = errors; }

    private Bucket newBucket() {
        return Bucket4j.builder().addLimit(Bandwidth.classic(20,
            Refill.intervally(20, Duration.ofMinutes(1)))).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        // Não confiar em X-Forwarded-For enviado pelo cliente sem proxy confiável.
        String ip = req.getRemoteAddr();
        if (cache.computeIfAbsent(ip, k -> newBucket()).tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            res.setHeader("Retry-After", "60");
            errors.write(res, 429, "Too Many Requests", "Muitas requisições. Tente novamente mais tarde.");
        }
    }
}
