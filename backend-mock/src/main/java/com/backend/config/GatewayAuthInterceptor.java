package com.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;

@Component
public class GatewayAuthInterceptor implements HandlerInterceptor {
    private final byte[] sharedKey;
    private final ObjectMapper mapper;
    public GatewayAuthInterceptor(@Value("${app.gateway.shared-key}") String sharedKey, ObjectMapper mapper) {
        this.sharedKey = sharedKey.getBytes(StandardCharsets.UTF_8);
        this.mapper = mapper;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String supplied = request.getHeader("X-Gateway-Key");
        boolean accepted = "true".equals(request.getHeader("X-Gateway-Authenticated"))
            && supplied != null
            && MessageDigest.isEqual(sharedKey, supplied.getBytes(StandardCharsets.UTF_8));
        if (!accepted) {
            response.setStatus(403);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            mapper.writeValue(response.getWriter(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", 403, "error", "Forbidden", "message", "Acesso direto negado."));
        }
        return accepted;
    }
}
