package com.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/pedidos")
public class PedidoController {

    @GetMapping
    public String getPedidos() {
        return "[{\"id\": 1, \"item\": \"Notebook\", \"status\": \"ENVIADO\"}]";
    }
}