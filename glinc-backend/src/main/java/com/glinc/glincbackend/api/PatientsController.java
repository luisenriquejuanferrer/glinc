package com.glinc.glincbackend.api;

import com.glinc.glincbackend.auth.AppSession;
import com.glinc.glincbackend.bridge.dto.BridgePatient;
import com.glinc.glincbackend.cgm.GlucoseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class PatientsController {

    private final GlucoseService glucoseService;

    public PatientsController(GlucoseService glucoseService) {
        this.glucoseService = glucoseService;
    }

    @GetMapping
    public ResponseEntity<?> listarPacientes(HttpServletRequest request) {
        AppSession sesion = (AppSession) request.getAttribute("appSession");
        return ResponseEntity.ok(glucoseService.obtenerDashboard(sesion));
    }

    // hours: 12/24/168/336/720/2160. El 2160 (3 meses) cubre el rango clinico de la HbA1c.
    @GetMapping("/{patientId}/history")
    public ResponseEntity<?> historico(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "168") int hours,
            HttpServletRequest request) {

        AppSession sesion = (AppSession) request.getAttribute("appSession");

        if (!pacienteEnSesion(sesion, patientId)) {
            return errorProblem(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND",
                    "El paciente no existe o no pertenece a tu cuenta.");
        }

        if (hours != 12 && hours != 24 && hours != 168 && hours != 336
                && hours != 720 && hours != 2160) {
            return errorProblem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "El parametro hours solo admite 12, 24, 168, 336, 720 o 2160.");
        }

        return ResponseEntity.ok(glucoseService.obtenerHistorico(patientId, hours));
    }

    private boolean pacienteEnSesion(AppSession sesion, String patientId) {
        if (sesion.getPatients() == null) {
            return false;
        }
        for (BridgePatient paciente : sesion.getPatients()) {
            if (paciente.getPatientId().equals(patientId)) {
                return true;
            }
        }
        return false;
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
