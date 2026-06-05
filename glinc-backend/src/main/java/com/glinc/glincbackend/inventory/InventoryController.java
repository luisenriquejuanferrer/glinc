package com.glinc.glincbackend.inventory;

import com.glinc.glincbackend.auth.AppSession;
import com.glinc.glincbackend.bridge.dto.BridgePatient;
import com.glinc.glincbackend.user.CaregiverRole;
import com.glinc.glincbackend.inventory.dto.InventoryItemDto;
import com.glinc.glincbackend.inventory.dto.UpdateInventoryRequest;
import com.glinc.glincbackend.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "PATIENT_NOT_FOUND",
                    "El paciente no esta vinculado a tu cuenta.");
        }
        if (esMedico(sesion)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "ROLE_FORBIDDEN",
                    "El rol DOCTOR no gestiona inventario.");
        }

        List<InventoryItemDto> lista = service.list(patientId);
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{type}")
    public ResponseEntity<?> update(HttpServletRequest request,
                                    @PathVariable String patientId,
                                    @PathVariable String type,
                                    @RequestBody UpdateInventoryRequest body) {
        AppSession sesion = (AppSession) request.getAttribute("appSession");
        if (!pertenece(sesion, patientId)) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "PATIENT_NOT_FOUND",
                    "El paciente no esta vinculado a tu cuenta.");
        }
        if (esMedico(sesion)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "ROLE_FORBIDDEN",
                    "El rol DOCTOR no gestiona inventario.");
        }

        InventoryItemType tipo = parseTipo(type);
        if (tipo == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Tipo de inventario invalido: " + type);
        }

        if (body == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Falta el cuerpo de la peticion.");
        }
        if (body.getQuantity() != null && body.getQuantity().length() > 60) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "La cantidad no puede superar 60 caracteres.");
        }

        InventoryItemDto guardada = service.update(patientId, tipo, body);
        return ResponseEntity.ok(guardada);
    }

    // El inventario es competencia del cuidador; el medico tiene vista clinica sin gestion.
    private boolean esMedico(AppSession sesion) {
        return sesion != null && sesion.getRole() == CaregiverRole.DOCTOR;
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
}
