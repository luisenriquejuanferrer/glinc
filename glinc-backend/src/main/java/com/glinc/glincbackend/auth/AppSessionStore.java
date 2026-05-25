package com.glinc.glincbackend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Store de sesiones en memoria (se pierden al reiniciar la app).
@Service
public class AppSessionStore {

    private static final Logger log = LoggerFactory.getLogger(AppSessionStore.class);

    private final ConcurrentHashMap<String, AppSession> sesiones = new ConcurrentHashMap<>();

    public String guardar(AppSession sesion) {
        String token = UUID.randomUUID().toString();
        sesiones.put(token, sesion);
        log.info("Sesion app creada: token={}, email={}, expira={}",
                token, sesion.getEmail(), sesion.getExpiresAt());
        return token;
    }

    public AppSession buscar(String token) {
        if (token == null) {
            return null;
        }
        AppSession sesion = sesiones.get(token);
        if (sesion == null) {
            return null;
        }
        if (sesion.isExpired()) {
            sesiones.remove(token);
            log.info("Sesion app caducada eliminada: token={}, email={}",
                    token, sesion.getEmail());
            return null;
        }
        return sesion;
    }

    // Puede devolver sesiones caducadas; el consumidor debe filtrar por isExpired().
    public Collection<AppSession> listarSesiones() {
        return sesiones.values();
    }

    public AppSession eliminar(String token) {
        if (token == null) {
            return null;
        }
        AppSession eliminada = sesiones.remove(token);
        if (eliminada != null) {
            log.info("Sesion app eliminada: token={}, email={}",
                    token, eliminada.getEmail());
        }
        return eliminada;
    }
}
