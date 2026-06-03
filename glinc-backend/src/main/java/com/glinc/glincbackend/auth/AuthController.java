package com.glinc.glincbackend.auth;

import com.glinc.glincbackend.auth.dto.LoginRequest;
import com.glinc.glincbackend.auth.dto.LoginResponse;
import com.glinc.glincbackend.auth.dto.MeResponse;
import com.glinc.glincbackend.bridge.BridgeClient;
import com.glinc.glincbackend.bridge.BridgeException;
import com.glinc.glincbackend.bridge.dto.SessionResponse;
import com.glinc.glincbackend.cgm.HistoryBackfillService;
import com.glinc.glincbackend.patient.CaregiverPatientService;
import com.glinc.glincbackend.patient.PatientService;
import com.glinc.glincbackend.user.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // Mismo TTL que el bridge para evitar tokens vivos contra sesiones ya invalidadas.
    private static final long DURACION_HORAS = 12;

    private final BridgeClient bridgeClient;
    private final AppSessionStore sessionStore;
    private final HistoryBackfillService historyBackfillService;
    private final PatientService patientService;
    private final CaregiverPatientService caregiverPatientService;
    private final UserProfileService userProfileService;

    public AuthController(BridgeClient bridgeClient, AppSessionStore sessionStore,
                          HistoryBackfillService historyBackfillService,
                          PatientService patientService,
                          CaregiverPatientService caregiverPatientService,
                          UserProfileService userProfileService) {
        this.bridgeClient = bridgeClient;
        this.sessionStore = sessionStore;
        this.historyBackfillService = historyBackfillService;
        this.patientService = patientService;
        this.caregiverPatientService = caregiverPatientService;
        this.userProfileService = userProfileService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest peticion) {

        if (peticion == null
                || peticion.getEmail() == null
                || peticion.getEmail().isBlank()
                || peticion.getPassword() == null
                || peticion.getPassword().isBlank()) {
            return errorProblem(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Email y password son obligatorios.");
        }

        try {
            SessionResponse respuestaBridge = bridgeClient.createSession(
                    peticion.getEmail(), peticion.getPassword());

            Instant expiresAt = Instant.now().plus(DURACION_HORAS, ChronoUnit.HOURS);
            AppSession sesion = new AppSession(
                    peticion.getEmail(),
                    respuestaBridge.getSessionId(),
                    respuestaBridge.getPatients(),
                    expiresAt);
            String token = sessionStore.guardar(sesion);

            // Upsert sincrono antes del backfill: las lecturas que insertara el backfill
            // necesitan que el patient_id exista en la tabla `patients` (FK).
            patientService.upsertAll(sesion.getPatients());

            // Tambien antes del backfill: caregiver_patients require que `caregivers.email` exista,
            // asi que primero creamos/encontramos el perfil del cuidador.
            userProfileService.findOrCreate(peticion.getEmail());
            caregiverPatientService.linkAll(peticion.getEmail(), sesion.getPatients());

            historyBackfillService.backfillAsync(sesion);

            LoginResponse respuesta = new LoginResponse(
                    token,
                    sesion.getEmail(),
                    sesion.getPatients(),
                    sesion.getExpiresAt());
            return ResponseEntity.ok(respuesta);

        } catch (BridgeException e) {
            log.warn("Login fallido para {}: {}", peticion.getEmail(), e.getMessage());
            return errorProblem(HttpStatus.UNAUTHORIZED,
                    "LOGIN_FAILED",
                    "No se pudo iniciar sesion. Revisa tu email y password de LibreLinkUp.");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = leerToken(request);
        if (token != null) {
            AppSession sesion = sessionStore.eliminar(token);
            if (sesion != null) {
                // Best effort: si el bridge falla, la sesion local ya esta cerrada.
                try {
                    bridgeClient.revokeSession(sesion.getBridgeSessionId());
                } catch (BridgeException e) {
                    log.warn("No se pudo revocar sesion bridge en logout: {}", e.getMessage());
                }
            }
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(HttpServletRequest request) {
        AppSession sesion = (AppSession) request.getAttribute("appSession");
        MeResponse respuesta = new MeResponse(
                sesion.getEmail(),
                sesion.getPatients(),
                sesion.getExpiresAt());
        return ResponseEntity.ok(respuesta);
    }

    private String leerToken(HttpServletRequest request) {
        String cabecera = request.getHeader("Authorization");
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

    private ResponseEntity<Map<String, Object>> errorProblem(
            HttpStatus status, String code, String detail) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "about:blank");
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("code", code);
        body.put("detail", detail);
        return ResponseEntity.status(status).body(body);
    }
}
