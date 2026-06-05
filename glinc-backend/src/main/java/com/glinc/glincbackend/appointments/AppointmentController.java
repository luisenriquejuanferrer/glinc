package com.glinc.glincbackend.appointments;

import com.glinc.glincbackend.appointments.dto.AppointmentDto;
import com.glinc.glincbackend.appointments.dto.SaveAppointmentRequest;
import com.glinc.glincbackend.auth.AppSession;
import com.glinc.glincbackend.bridge.dto.BridgePatient;
import com.glinc.glincbackend.user.CaregiverRole;
import com.glinc.glincbackend.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request,
                                  @PathVariable String patientId) {
        AppSession sesion = (AppSession) request.getAttribute("appSession");
        if (!pertenece(sesion, patientId)) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "PATIENT_NOT_FOUND",
                    "El paciente no esta vinculado a tu cuenta.");
        }
        if (esMedico(sesion)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "ROLE_FORBIDDEN",
                    "El rol DOCTOR no gestiona citas medicas.");
        }
        List<AppointmentDto> lista = service.list(patientId);
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    public ResponseEntity<?> create(HttpServletRequest request,
                                    @PathVariable String patientId,
                                    @RequestBody SaveAppointmentRequest body) {
        AppSession sesion = (AppSession) request.getAttribute("appSession");
        if (!pertenece(sesion, patientId)) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "PATIENT_NOT_FOUND",
                    "El paciente no esta vinculado a tu cuenta.");
        }
        if (esMedico(sesion)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "ROLE_FORBIDDEN",
                    "El rol DOCTOR no gestiona citas medicas.");
        }
        String validacion = validar(body);
        if (validacion != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", validacion);
        }
        AppointmentDto creada = service.create(patientId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(HttpServletRequest request,
                                    @PathVariable String patientId,
                                    @PathVariable Long id,
                                    @RequestBody SaveAppointmentRequest body) {
        AppSession sesion = (AppSession) request.getAttribute("appSession");
        if (!pertenece(sesion, patientId)) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "PATIENT_NOT_FOUND",
                    "El paciente no esta vinculado a tu cuenta.");
        }
        if (esMedico(sesion)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "ROLE_FORBIDDEN",
                    "El rol DOCTOR no gestiona citas medicas.");
        }
        String validacion = validar(body);
        if (validacion != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", validacion);
        }
        AppointmentDto actualizada = service.update(id, body);
        if (actualizada == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "APPOINTMENT_NOT_FOUND",
                    "La cita no existe.");
        }
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(HttpServletRequest request,
                                    @PathVariable String patientId,
                                    @PathVariable Long id) {
        AppSession sesion = (AppSession) request.getAttribute("appSession");
        if (!pertenece(sesion, patientId)) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "PATIENT_NOT_FOUND",
                    "El paciente no esta vinculado a tu cuenta.");
        }
        if (esMedico(sesion)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "ROLE_FORBIDDEN",
                    "El rol DOCTOR no gestiona citas medicas.");
        }
        boolean borrada = service.delete(id);
        if (!borrada) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "APPOINTMENT_NOT_FOUND",
                    "La cita no existe.");
        }
        return ResponseEntity.noContent().build();
    }

    private String validar(SaveAppointmentRequest body) {
        if (body == null) {
            return "Falta el cuerpo de la peticion.";
        }
        if (body.getAppointmentAt() == null) {
            return "La fecha de la cita es obligatoria.";
        }
        String prof = body.getProfessional();
        if (prof == null || prof.trim().isEmpty()) {
            return "El profesional es obligatorio.";
        }
        if (prof.trim().length() > 120) {
            return "El profesional no puede superar 120 caracteres.";
        }
        if (body.getReason() != null && body.getReason().length() > 300) {
            return "El motivo no puede superar 300 caracteres.";
        }
        return null;
    }

    // Las citas las gestiona el cuidador; el medico tiene vista clinica sin gestion.
    private boolean esMedico(AppSession sesion) {
        return sesion != null && sesion.getRole() == CaregiverRole.DOCTOR;
    }

    private boolean pertenece(AppSession sesion, String patientId) {
        if (sesion == null || sesion.getPatients() == null) {
            return false;
        }
        for (BridgePatient p : sesion.getPatients()) {
            if (patientId.equals(p.getPatientId())) {
                return true;
            }
        }
        return false;
    }
}
