package com.glinc.glincbackend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private final AppSessionStore sessionStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthFilter(AppSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String ruta = request.getRequestURI();
        String metodo = request.getMethod();

        if (esPublica(ruta, metodo)) {
            chain.doFilter(request, response);
            return;
        }

        String token = extraerToken(request.getHeader("Authorization"));
        if (token == null) {
            escribirError(request, response, "MISSING_TOKEN",
                    "Falta la cabecera Authorization: Bearer <token>.");
            return;
        }

        AppSession sesion = sessionStore.buscar(token);
        if (sesion == null) {
            escribirError(request, response, "INVALID_TOKEN",
                    "Token invalido o sesion caducada.");
            return;
        }

        request.setAttribute("appSession", sesion);

        chain.doFilter(request, response);
    }

    private boolean esPublica(String ruta, String metodo) {
        if ("OPTIONS".equalsIgnoreCase(metodo)) {
            return true;
        }
        if (ruta == null) {
            return false;
        }
        if (ruta.equals("/api/auth/login")) {
            return true;
        }
        if (ruta.startsWith("/actuator")) {
            return true;
        }
        if (ruta.startsWith("/docs") || ruta.startsWith("/v3/api-docs")
                || ruta.startsWith("/swagger-ui")) {
            return true;
        }
        return !ruta.startsWith("/api/");
    }

    private String extraerToken(String cabecera) {
        if (cabecera == null) {
            return null;
        }
        String prefijo = "Bearer ";
        if (!cabecera.startsWith(prefijo)) {
            return null;
        }
        String token = cabecera.substring(prefijo.length()).trim();
        if (token.isEmpty()) {
            return null;
        }
        return token;
    }

    private void escribirError(HttpServletRequest request, HttpServletResponse response,
                               String code, String detail) throws IOException {
        // Sin CORS aqui el navegador bloquea el 401: este filtro corta antes de que Spring MVC lo aniada.
        anadirCorsSiProcede(request, response);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new HashMap<>();
        body.put("type", "about:blank");
        body.put("title", "Unauthorized");
        body.put("status", 401);
        body.put("code", code);
        body.put("detail", detail);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static final String[] ORIGENES_PERMITIDOS = {
            "http://localhost:8100",
            "http://localhost:4200"
    };

    private void anadirCorsSiProcede(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin == null) {
            return;
        }
        for (String permitido : ORIGENES_PERMITIDOS) {
            if (permitido.equals(origin)) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Vary", "Origin");
                return;
            }
        }
    }
}
