package com.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final GatewayAuthInterceptor gatewayAuthInterceptor;

    public WebConfig(GatewayAuthInterceptor gatewayAuthInterceptor) {
        this.gatewayAuthInterceptor = gatewayAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gatewayAuthInterceptor)
                .addPathPatterns("/internal/**"); // Bloqueia apenas as rotas da API interna
    }
}
