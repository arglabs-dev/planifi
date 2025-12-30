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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
        RateLimitingFilter rateLimitingFilter =
                new RateLimitingFilter(securityProperties, objectMapper, tracer);

        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> configureHeaders(headers, securityProperties))
                .authorizeHttpRequests(registry -> {
                    registry.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                    registry.requestMatchers("/v3/api-docs/**", "/api/v1/openapi/**",
                            "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    registry.requestMatchers("/api/v1/auth/**").permitAll();

                    if (securityProperties.isEnabled()) {
                        registry.requestMatchers(HttpMethod.POST, "/api/v1/expenses").authenticated();
                        registry.requestMatchers(HttpMethod.GET, "/api/v1/expenses").authenticated();
                        registry.requestMatchers(HttpMethod.POST, "/api/v1/accounts").authenticated();
                        registry.requestMatchers(HttpMethod.GET, "/api/v1/accounts").authenticated();
                        registry.requestMatchers(HttpMethod.POST, "/api/v1/accounts/*/disable")
                                .authenticated();
                        registry.requestMatchers(HttpMethod.POST, "/api/v1/transactions")
                                .authenticated();
                        registry.requestMatchers(HttpMethod.GET, "/api/v1/transactions")
                                .authenticated();
                        registry.requestMatchers("/api/v1/api-keys/**").authenticated();
                        registry.anyRequest().denyAll();
                    } else {
                        registry.anyRequest().permitAll();
                    }
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(apiKeyAuthenticationFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(rateLimitingFilter, ApiKeyAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults())
                .formLogin(login -> login.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityProperties securityProperties) {
        SecurityProperties.CorsProperties cors = securityProperties.getCors();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(cors.getAllowedOrigins());
        configuration.setAllowedMethods(cors.getAllowedMethods());
        configuration.setAllowedHeaders(cors.getAllowedHeaders());
        configuration.setExposedHeaders(cors.getExposedHeaders());
        configuration.setAllowCredentials(cors.isAllowCredentials());
        configuration.setMaxAge(cors.getMaxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void configureHeaders(
            org.springframework.security.config.annotation.web.configurers.HeadersConfigurer<
                    HttpSecurity> headersConfigurer,
            SecurityProperties securityProperties
    ) {
        SecurityProperties.HeadersProperties headers = securityProperties.getHeaders();
        if (headers == null || !headers.isEnabled()) {
            headersConfigurer.disable();
            return;
        }
        headersConfigurer.contentTypeOptions(Customizer.withDefaults());
        if (headers.isFrameOptionsDeny()) {
            headersConfigurer.frameOptions(frame -> frame.deny());
        }
        headersConfigurer.referrerPolicy(referrer ->
                referrer.policy(headers.getReferrerPolicy()));
        headersConfigurer.contentSecurityPolicy(csp ->
                csp.policyDirectives(headers.getContentSecurityPolicy()));
        if (headers.isHstsEnabled()) {
            headersConfigurer.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(headers.isHstsIncludeSubDomains())
                    .maxAgeInSeconds(headers.getHstsMaxAgeSeconds())
                    .preload(headers.isHstsPreload()));
        } else {
            headersConfigurer.httpStrictTransportSecurity(hsts -> hsts.disable());
        }
    }
}
