package com.glinc.glincbackend.user;

import com.glinc.glincbackend.user.dto.UpdateUserProfileRequest;
import com.glinc.glincbackend.user.dto.UserProfileDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final String EMAIL = "ana.garcia@ejemplo.com";

    @Mock
    private UserProfileRepository repository;

    @InjectMocks
    private UserProfileService service;

    @Test
    void findOrCreate_perfilExistente_devuelveSinCrear() {
        UserProfile existente = new UserProfile(EMAIL);
        existente.setFirstName("Ana");
        when(repository.findById(EMAIL)).thenReturn(Optional.of(existente));

        UserProfile resultado = service.findOrCreate(EMAIL);

        assertThat(resultado).isSameAs(existente);
        verify(repository, never()).save(any());
    }

    @Test
    void findOrCreate_perfilNoExistente_creaConFirstNameDeLocalPartDelEmail() {
        when(repository.findById(EMAIL)).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile resultado = service.findOrCreate(EMAIL);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).save(captor.capture());

        UserProfile guardado = captor.getValue();
        assertThat(guardado.getEmail()).isEqualTo(EMAIL);
        assertThat(guardado.getFirstName()).isEqualTo("ana.garcia");
        assertThat(resultado).isSameAs(guardado);
    }

    @Test
    void findOrCreate_emailSinArroba_usaEmailEntero() {
        String raro = "sinArroba";
        when(repository.findById(raro)).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.findOrCreate(raro);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("sinArroba");
    }

    @Test
    void findOrCreate_localPartMuyLarga_truncaA100Caracteres() {
        String largo = "a".repeat(150) + "@dominio.com";
        when(repository.findById(largo)).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.findOrCreate(largo);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getFirstName()).hasSize(100);
    }

    @Test
    void getProfile_construyeDtoConTodosLosCampos() {
        UserProfile perfil = new UserProfile(EMAIL);
        perfil.setFirstName("Ana");
        perfil.setLastName("Garcia");
        perfil.setBirthDate(LocalDate.of(1990, 3, 14));
        perfil.setPhone("+34 600 000 000");
        when(repository.findById(EMAIL)).thenReturn(Optional.of(perfil));

        UserProfileDto dto = service.getProfile(EMAIL);

        assertThat(dto.getEmail()).isEqualTo(EMAIL);
        assertThat(dto.getFirstName()).isEqualTo("Ana");
        assertThat(dto.getLastName()).isEqualTo("Garcia");
        assertThat(dto.getBirthDate()).isEqualTo(LocalDate.of(1990, 3, 14));
        assertThat(dto.getPhone()).isEqualTo("+34 600 000 000");
    }

    @Test
    void update_actualizaCamposYRecortaWhitespace() {
        UserProfile existente = new UserProfile(EMAIL);
        existente.setFirstName("Antiguo");
        when(repository.findById(EMAIL)).thenReturn(Optional.of(existente));
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserProfileRequest req = new UpdateUserProfileRequest();
        req.setFirstName("  Ana  ");
        req.setLastName("  Garcia ");
        req.setPhone("  +34 600 000 000 ");
        req.setBirthDate(LocalDate.of(1990, 3, 14));

        UserProfileDto dto = service.update(EMAIL, req);

        assertThat(dto.getFirstName()).isEqualTo("Ana");
        assertThat(dto.getLastName()).isEqualTo("Garcia");
        assertThat(dto.getPhone()).isEqualTo("+34 600 000 000");
        assertThat(dto.getBirthDate()).isEqualTo(LocalDate.of(1990, 3, 14));
        verify(repository, times(1)).save(any(UserProfile.class));
    }

    @Test
    void update_camposVaciosOSoloWhitespace_seGuardanComoNull() {
        UserProfile existente = new UserProfile(EMAIL);
        existente.setFirstName("Antiguo");
        when(repository.findById(EMAIL)).thenReturn(Optional.of(existente));
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserProfileRequest req = new UpdateUserProfileRequest();
        req.setFirstName("   ");
        req.setLastName("");
        req.setPhone(null);

        UserProfileDto dto = service.update(EMAIL, req);

        assertThat(dto.getFirstName()).isNull();
        assertThat(dto.getLastName()).isNull();
        assertThat(dto.getPhone()).isNull();
    }

    @Test
    void getProfile_perfilSinRol_devuelveRoleNull() {
        UserProfile perfil = new UserProfile(EMAIL);
        perfil.setFirstName("Ana");
        when(repository.findById(EMAIL)).thenReturn(Optional.of(perfil));

        UserProfileDto dto = service.getProfile(EMAIL);

        assertThat(dto.getRole()).isNull();
    }

    @Test
    void updateRole_persisteRolYLoDevuelveComoTexto() {
        UserProfile existente = new UserProfile(EMAIL);
        when(repository.findById(EMAIL)).thenReturn(Optional.of(existente));
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileDto dto = service.updateRole(EMAIL, CaregiverRole.DOCTOR);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(CaregiverRole.DOCTOR);
        assertThat(dto.getRole()).isEqualTo("DOCTOR");
    }
}
