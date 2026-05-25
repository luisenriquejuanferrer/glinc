package com.glinc.glincbackend.inventory;

import com.glinc.glincbackend.auth.AppSession;
import com.glinc.glincbackend.bridge.dto.BridgePatient;
import com.glinc.glincbackend.inventory.dto.InventoryItemDto;
import com.glinc.glincbackend.inventory.dto.UpdateInventoryRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients/{patientId}/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request,
                                  @PathVariable String patientId) {
        AppSession sesion = (AppSession) request.getAttribute("appSession");
        if (!pertenece(sesion, patientId)) {
            return errorProblem(HttpStatus.NOT_FOUND,
                    "PATIENT_NOT_FOUND",
                    "El paciente no esta vinculado a tu cuenta.");
        }

        List<InventoryItemDto> lista = service.list(sesion.getEmail(), patientId);
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{type}")
    public ResponseEntity<?> update(HttpServletRequest request,
                                    @PathVariable String patientId,
                                    @PathVariable String type,
                                    @RequestBody UpdateInventoryRequest body) {
        AppSession sesion = (AppSession) request.getAttribute("appSession");
        if (!pertenece(sesion, patientId)) {
            return errorProblem(HttpStatus.NOT_FOUND,
                    "PATIENT_NOT_FOUND",
                    "El paciente no esta vinculado a tu cuenta.");
        }

        InventoryItemType tipo = parseTipo(type);
        if (tipo == null) {
            return errorProblem(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Tipo de inventario invalido: " + type);
        }

        if (body == null) {
            return errorProblem(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Falta el cuerpo de la peticion.");
        }
        if (body.getQuantity() != null && body.getQuantity().length() > 60) {
            return errorProblem(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "La cantidad no puede superar 60 caracteres.");
        }

        InventoryItemDto guardada = service.update(
                sesion.getEmail(), patientId, tipo, body);
        return ResponseEntity.ok(guardada);
    }

    // Impide leer/escribir inventario de pacientes no vinculados a la sesion del cuidador.
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

    private InventoryItemType parseTipo(String texto) {
        if (texto == null) {
            return null;
        }
        for (InventoryItemType t : InventoryItemType.values()) {
            if (t.name().equalsIgnoreCase(texto)) {
                return t;
            }
        }
        return null;
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
