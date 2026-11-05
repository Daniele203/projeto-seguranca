package com.gateway.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/** Demonstração acadêmica: consulta o backend SEM a chave compartilhada. */
@RestController
@RequestMapping("/api/demo")
public class BackendDemoController {
    private final String backendUrl;
    private final RestTemplate client = new RestTemplate();

    public BackendDemoController(@Value("${app.backend.url}") String backendUrl) {
        this.backendUrl = backendUrl;
    }

    @GetMapping("/backend-sem-confianca")
    public ResponseEntity<Map<String, Object>> backendSemConfianca() {
        try {
            client.getForEntity(backendUrl, String.class);
            // Nunca devolve conteúdo de backend que por acaso aceite acesso indevido.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500, "error", "Demonstration Failed",
                "message", "O backend aceitou uma chamada sem cabeçalhos de confiança."));
        } catch (HttpClientErrorException.Forbidden ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "timestamp", Instant.now().toString(), "status", 403,
                "error", "Forbidden", "message", "O backend rejeitou a chamada sem cabeçalhos de confiança."));
        }
    }
}
