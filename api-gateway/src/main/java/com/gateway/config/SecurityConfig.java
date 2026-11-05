package com.gateway.config;

import com.gateway.exception.ApiErrorWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwt;
    private final RateLimitingFilter rate;
    private final ApiErrorWriter errors;

    public SecurityConfig(JwtAuthenticationFilter jwt, RateLimitingFilter rate, ApiErrorWriter errors) {
        this.jwt = jwt;
        this.rate = rate;
        this.errors = errors;
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService users(PasswordEncoder encoder,
                             @Value("${app.demo.user-password}") String userPassword,
                             @Value("${app.demo.admin-password}") String adminPassword) {
        return new InMemoryUserDetailsManager(
            User.withUsername("usuario").password(encoder.encode(userPassword)).roles("USER").build(),
            User.withUsername("admin").password(encoder.encode(adminPassword)).roles("ADMIN", "USER").build()
        );
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    // Evita execução duplicada fora da cadeia do Spring Security.
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtServletRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RateLimitingFilter> rateServletRegistration(RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requiresChannel(channel -> channel.anyRequest().requiresSecure())
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'"))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                    errors.write(res, 401, "Unauthorized", "Autenticação necessária ou inválida."))
                .accessDeniedHandler((req, res, e) ->
                    errors.write(res, 403, "Forbidden", "Acesso negado.")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(rate, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwt, RateLimitingFilter.class);
        return http.build();
    }
}
