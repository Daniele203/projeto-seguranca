package com.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/pedidos")
public class GatewayController {
    private final RestTemplate client = new RestTemplate();
    private final String backendUrl;
    private final String backendKey;

    public GatewayController(@Value("${app.backend.url}") String backendUrl,
                             @Value("${app.backend.shared-key}") String backendKey) {
        this.backendUrl = backendUrl;
        this.backendKey = backendKey;
    }

    @GetMapping
    public ResponseEntity<String> pedidos(Authentication authentication) {
        HttpHeaders headers = new HttpHeaders();
        // Cria novos headers, sem encaminhar os headers de confiança do cliente.
        headers.set("X-Gateway-Authenticated", "true");
        headers.set("X-Gateway-Key", backendKey);
        headers.set("X-User-Id", authentication.getName());
        return client.exchange(backendUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
