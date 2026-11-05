package com.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ApiErrorWriter {
    private final ObjectMapper mapper;
    public ApiErrorWriter(ObjectMapper mapper) { this.mapper = mapper; }
    public void write(HttpServletResponse response, int status, String error, String message) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        mapper.writeValue(response.getWriter(), body);
    }
}
