package com.glinc.glincbackend.appointments;

import com.glinc.glincbackend.appointments.dto.AppointmentDto;
import com.glinc.glincbackend.appointments.dto.SaveAppointmentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final String PATIENT_ID = "pat-1";

    @Mock
    private AppointmentRepository repository;

    @InjectMocks
    private AppointmentService service;

    @Test
    void list_mapeaCadaFilaADto() {
        Appointment a = appointment("Dra. Diaz", "Revision", Instant.parse("2026-06-15T10:00:00Z"));
        Appointment b = appointment("Dr. Lopez", null, Instant.parse("2026-05-10T11:00:00Z"));
        when(repository.findByPatientIdOrderByAppointmentAtDesc(PATIENT_ID))
                .thenReturn(List.of(a, b));

        List<AppointmentDto> resultado = service.list(PATIENT_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getProfessional()).isEqualTo("Dra. Diaz");
        assertThat(resultado.get(0).getReason()).isEqualTo("Revision");
        assertThat(resultado.get(1).getProfessional()).isEqualTo("Dr. Lopez");
        assertThat(resultado.get(1).getReason()).isNull();
    }

    @Test
    void create_recortaProfessionalYNormalizaReason() {
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveAppointmentRequest dto = new SaveAppointmentRequest();
        dto.setAppointmentAt(Instant.parse("2026-06-15T10:00:00Z"));
        dto.setProfessional("  Dra. Diaz  ");
        dto.setReason("  Revision anual  ");

        service.create(PATIENT_ID, dto);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(captor.capture());

        Appointment guardada = captor.getValue();
        assertThat(guardada.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(guardada.getProfessional()).isEqualTo("Dra. Diaz");
        assertThat(guardada.getReason()).isEqualTo("Revision anual");
        assertThat(guardada.getAppointmentAt()).isEqualTo(Instant.parse("2026-06-15T10:00:00Z"));
    }

    @Test
    void create_reasonVacioOWhitespace_seGuardaComoNull() {
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveAppointmentRequest dto = new SaveAppointmentRequest();
        dto.setAppointmentAt(Instant.parse("2026-06-15T10:00:00Z"));
        dto.setProfessional("Dr. X");
        dto.setReason("   ");

        service.create(PATIENT_ID, dto);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isNull();
    }

    @Test
    void update_existente_modificaCamposYDevuelveDto() {
        Appointment existente = appointment("Dr. Antiguo", "Antiguo",
                Instant.parse("2026-06-15T10:00:00Z"));
        when(repository.findById(42L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveAppointmentRequest dto = new SaveAppointmentRequest();
        dto.setAppointmentAt(Instant.parse("2026-07-01T09:00:00Z"));
        dto.setProfessional("  Dra. Nueva  ");
        dto.setReason("Cambio de fecha");

        AppointmentDto resultado = service.update(42L, dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getProfessional()).isEqualTo("Dra. Nueva");
        assertThat(resultado.getReason()).isEqualTo("Cambio de fecha");
        assertThat(resultado.getAppointmentAt()).isEqualTo(Instant.parse("2026-07-01T09:00:00Z"));
        assertThat(existente.getProfessional()).isEqualTo("Dra. Nueva");
    }

    @Test
    void update_noExistente_devuelveNullYNoGuarda() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        SaveAppointmentRequest dto = new SaveAppointmentRequest();
        dto.setAppointmentAt(Instant.now());
        dto.setProfessional("Dr. X");

        AppointmentDto resultado = service.update(99L, dto);

        assertThat(resultado).isNull();
        verify(repository, never()).save(any(Appointment.class));
    }

    @Test
    void delete_existente_borraYDevuelveTrue() {
        Appointment existente = appointment("Dr. X", null, Instant.now());
        when(repository.findById(42L)).thenReturn(Optional.of(existente));

        boolean resultado = service.delete(42L);

        assertThat(resultado).isTrue();
        verify(repository).delete(existente);
    }

    @Test
    void delete_noExistente_devuelveFalseYNoBorra() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        boolean resultado = service.delete(99L);

        assertThat(resultado).isFalse();
        verify(repository, never()).delete(any(Appointment.class));
    }

    private static Appointment appointment(String professional, String reason, Instant at) {
        return new Appointment(PATIENT_ID, at, professional, reason);
    }
}
