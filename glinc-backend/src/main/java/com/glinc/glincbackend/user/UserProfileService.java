package com.glinc.glincbackend.user;

import com.glinc.glincbackend.user.dto.UpdateUserProfileRequest;
import com.glinc.glincbackend.user.dto.UserProfileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    // Inicializa firstName con la parte local del email para que la UI no muestre un perfil vacio.
    public UserProfile findOrCreate(String email) {
        return repository.findById(email).orElseGet(() -> {
            log.info("Creando perfil para nuevo usuario: {}", email);
            UserProfile nuevo = new UserProfile(email);
            nuevo.setFirstName(extraerNombreDelEmail(email));
            return repository.save(nuevo);
        });
    }

    private String extraerNombreDelEmail(String email) {
        if (email == null) {
            return null;
        }
        int arroba = email.indexOf('@');
        String local = (arroba > 0) ? email.substring(0, arroba) : email;
        if (local.length() > 100) {
            local = local.substring(0, 100);
        }
        return local;
    }

    public UserProfileDto getProfile(String email) {
        UserProfile perfil = findOrCreate(email);
        return toDto(perfil);
    }

    public UserProfileDto update(String email, UpdateUserProfileRequest request) {
        UserProfile perfil = findOrCreate(email);

        perfil.setFirstName(normalizar(request.getFirstName()));
        perfil.setLastName(normalizar(request.getLastName()));
        perfil.setPhone(normalizar(request.getPhone()));
        perfil.setBirthDate(request.getBirthDate());

        UserProfile guardado = repository.save(perfil);
        log.info("Perfil actualizado: {}", email);
        return toDto(guardado);
    }

    // Lo usa el modal de primera sesion y el selector de Settings.
    public UserProfileDto updateRole(String email, CaregiverRole role) {
        UserProfile perfil = findOrCreate(email);
        perfil.setRole(role);
        UserProfile guardado = repository.save(perfil);
        log.info("Rol actualizado: {} -> {}", email, role);
        return toDto(guardado);
    }

    private UserProfileDto toDto(UserProfile perfil) {
        return new UserProfileDto(
                perfil.getEmail(),
                perfil.getFirstName(),
                perfil.getLastName(),
                perfil.getBirthDate(),
                perfil.getPhone(),
                perfil.getRole() == null ? null : perfil.getRole().name(),
                perfil.getUpdatedAt());
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        if (recortado.isEmpty()) {
            return null;
        }
        return recortado;
    }
}
