package com.gateway.config;

import com.gateway.exception.ApiErrorWriter;
import com.gateway.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ApiErrorWriter errors;

    public JwtAuthenticationFilter(JwtService jwtService, ApiErrorWriter errors) {
        this.jwtService = jwtService;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null) {
            chain.doFilter(request, response);
            return;
        }
        if (!header.startsWith("Bearer ") || header.substring(7).isBlank()) {
            errors.write(response, 401, "Unauthorized", "Token ausente ou inválido.");
            return;
        }
        try {
            Claims claims = jwtService.validateToken(header.substring(7));
            Object rolesClaim = claims.get("roles");
            if (!(rolesClaim instanceof List<?> rawRoles)) throw new IllegalArgumentException("roles ausentes");
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            for (Object role : rawRoles) {
                if (!(role instanceof String value) || !value.matches("ROLE_[A-Z_]+")) {
                    throw new IllegalArgumentException("role inválida");
                }
                authorities.add(new SimpleGrantedAuthority(value));
            }
            var authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            errors.write(response, 401, "Unauthorized", "Token ausente, inválido ou expirado.");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
