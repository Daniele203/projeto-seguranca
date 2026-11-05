package com.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @GetMapping("/status")
    public Map<String, String> status() { return Map.of("status", "Acesso ADMIN autorizado"); }
}
