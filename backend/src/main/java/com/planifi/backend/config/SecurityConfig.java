package com.planifi.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.application.ApiKeyService;
import com.planifi.backend.application.JwtService;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({SecurityProperties.class, JwtProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           SecurityProperties securityProperties,
                                           ApiKeyService apiKeyService,
                                           JwtService jwtService,
                                           ObjectMapper objectMapper,
                                           Tracer tracer)
            throws Exception {
        ApiKeyAuthenticationFilter apiKeyAuthenticationFilter =
                new ApiKeyAuthenticationFilter(securityProperties, apiKeyService, objectMapper, tracer);
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService, objectMapper, tracer);

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(registry -> {
                    registry.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                    registry.requestMatchers("/v3/api-docs/**", "/api/v1/openapi/**",
                            "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    registry.requestMatchers("/api/v1/auth/**").permitAll();

                    if (securityProperties.isEnabled()) {
                        registry.requestMatchers(HttpMethod.POST, "/api/v1/expenses").authenticated();
                        registry.requestMatchers(HttpMethod.GET, "/api/v1/expenses").authenticated();
                        registry.requestMatchers("/api/v1/api-keys/**").authenticated();
                        registry.anyRequest().denyAll();
                    } else {
                        registry.anyRequest().permitAll();
                    }
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults())
                .formLogin(login -> login.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
