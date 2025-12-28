package com.planifi.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.api.dto.ErrorResponse;
import com.planifi.backend.application.JwtService;
import com.planifi.backend.application.JwtUserClaims;
import io.jsonwebtoken.JwtException;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper, Tracer tracer) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring("Bearer ".length());
        try {
            JwtUserClaims claims = jwtService.parseToken(token);
            AuthenticatedUser principal = new AuthenticatedUser(claims.userId(), claims.email());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            token,
                            AuthorityUtils.createAuthorityList("ROLE_USER")
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException ex) {
            writeUnauthorized(response);
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = new ErrorResponse(
                "AUTH_INVALID_TOKEN",
                "Token JWT inválido o expirado",
                traceId()
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String traceId() {
        if (tracer.currentSpan() == null) {
            return "unknown";
        }
        return tracer.currentSpan().context().traceId();
    }
}
