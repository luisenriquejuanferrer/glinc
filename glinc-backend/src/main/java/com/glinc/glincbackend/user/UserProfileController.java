package com.glinc.glincbackend.user;

import com.glinc.glincbackend.auth.AppSession;
import com.glinc.glincbackend.user.dto.UpdateRoleRequest;
import com.glinc.glincbackend.user.dto.UpdateUserProfileRequest;
import com.glinc.glincbackend.user.dto.UserProfileDto;
import com.glinc.glincbackend.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// El email se saca SIEMPRE de la sesion (AppSession), nunca del body, para impedir editar perfil ajeno.
@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(HttpServletRequest request) {
        AppSession sesion = (AppSession) request.getAttribute("appSession");
        UserProfileDto perfil = service.getProfile(sesion.getEmail());
        return ResponseEntity.ok(perfil);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(HttpServletRequest request,
                                           @RequestBody UpdateUserProfileRequest body) {
        if (body == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Falta el cuerpo de la peticion.");
        }

        if (excedeLimite(body.getFirstName(), 100)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "El nombre no puede superar 100 caracteres.");
        }
        if (excedeLimite(body.getLastName(), 100)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "El apellido no puede superar 100 caracteres.");
        }
        if (excedeLimite(body.getPhone(), 30)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "El telefono no puede superar 30 caracteres.");
        }
        LocalDate nacimiento = body.getBirthDate();
        if (nacimiento != null && nacimiento.isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "La fecha de nacimiento no puede ser futura.");
        }

        AppSession sesion = (AppSession) request.getAttribute("appSession");
        UserProfileDto actualizado = service.update(sesion.getEmail(), body);
        return ResponseEntity.ok(actualizado);
    }

    // Lo invoca el modal de primera sesion y el selector de Settings.
    @PutMapping("/role")
    public ResponseEntity<?> updateRole(HttpServletRequest request,
                                        @RequestBody UpdateRoleRequest body) {
        if (body == null || body.getRole() == null || body.getRole().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "El rol es obligatorio.");
        }

        CaregiverRole rol = parseRol(body.getRole());
        if (rol == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Rol invalido: debe ser CAREGIVER o DOCTOR.");
        }

        AppSession sesion = (AppSession) request.getAttribute("appSession");
        UserProfileDto actualizado = service.updateRole(sesion.getEmail(), rol);
        // La sesion en memoria es la fuente para los guards 403: la actualizamos
        // aqui mismo para que el cambio surta efecto sin re-login.
        sesion.setRole(rol);
        return ResponseEntity.ok(actualizado);
    }

    private CaregiverRole parseRol(String texto) {
        for (CaregiverRole r : CaregiverRole.values()) {
            if (r.name().equalsIgnoreCase(texto.trim())) {
                return r;
            }
        }
        return null;
    }

    private boolean excedeLimite(String valor, int max) {
        if (valor == null) {
            return false;
        }
        return valor.trim().length() > max;
    }
}
