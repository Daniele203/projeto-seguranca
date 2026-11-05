package com.gateway.controller;

import com.gateway.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    private final AuthenticationManager manager;
    private final JwtService tokens;
    public AuthController(AuthenticationManager manager, JwtService tokens) {
        this.manager = manager; this.tokens = tokens;
    }
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest body) {
        Authentication auth = manager.authenticate(
            new UsernamePasswordAuthenticationToken(body.username(), body.password()));
        String jwt = tokens.generateToken(auth.getName(), auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority).toList());
        return ResponseEntity.ok().header("Cache-Control", "no-store")
            .body(Map.of("tokenType", "Bearer", "accessToken", jwt));
    }
}
