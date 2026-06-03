package com.glinc.glincbackend.patient;

import com.glinc.glincbackend.bridge.dto.BridgePatient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository repository;

    @InjectMocks
    private PatientService service;

    @Test
    void upsertAll_listaNull_noHaceNada() {
        service.upsertAll(null);

        verifyNoInteractions(repository);
    }

    @Test
    void upsertAll_pacienteNuevo_creaConNombres() {
        BridgePatient bp = bridgePatient("pat-1", "Ana", "Garcia");
        when(repository.findById("pat-1")).thenReturn(Optional.empty());

        service.upsertAll(List.of(bp));

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(repository).save(captor.capture());

        Patient guardado = captor.getValue();
        assertThat(guardado.getPatientId()).isEqualTo("pat-1");
        assertThat(guardado.getFirstName()).isEqualTo("Ana");
        assertThat(guardado.getLastName()).isEqualTo("Garcia");
    }

    @Test
    void upsertAll_pacienteExistenteMismoNombre_noLlamaASave() {
        BridgePatient bp = bridgePatient("pat-1", "Ana", "Garcia");
        Patient existente = new Patient("pat-1", "Ana", "Garcia");
        when(repository.findById("pat-1")).thenReturn(Optional.of(existente));

        service.upsertAll(List.of(bp));

        verify(repository, never()).save(any(Patient.class));
    }

    @Test
    void upsertAll_pacienteExistenteNombreCambio_actualiza() {
        BridgePatient bp = bridgePatient("pat-1", "Ana Maria", "Garcia Lopez");
        Patient existente = new Patient("pat-1", "Ana", "Garcia");
        when(repository.findById("pat-1")).thenReturn(Optional.of(existente));

        service.upsertAll(List.of(bp));

        verify(repository, times(1)).save(existente);
        assertThat(existente.getFirstName()).isEqualTo("Ana Maria");
        assertThat(existente.getLastName()).isEqualTo("Garcia Lopez");
    }

    @Test
    void upsertAll_pacienteExistenteSoloLastNameCambio_actualiza() {
        BridgePatient bp = bridgePatient("pat-1", "Ana", "Lopez");
        Patient existente = new Patient("pat-1", "Ana", "Garcia");
        when(repository.findById("pat-1")).thenReturn(Optional.of(existente));

        service.upsertAll(List.of(bp));

        verify(repository, times(1)).save(existente);
        assertThat(existente.getLastName()).isEqualTo("Lopez");
    }

    @Test
    void upsertAll_patientIdNullOBlank_seSalta() {
        BridgePatient sinId = bridgePatient(null, "Ana", "Garcia");
        BridgePatient blank = bridgePatient("   ", "Bea", "Lopez");
        BridgePatient ok = bridgePatient("pat-1", "Cris", "Diaz");
        when(repository.findById("pat-1")).thenReturn(Optional.empty());

        service.upsertAll(List.of(sinId, blank, ok));

        verify(repository, times(1)).save(any(Patient.class));
    }

    @Test
    void upsertAll_variosPacientes_procesaTodos() {
        BridgePatient a = bridgePatient("pat-A", "Ana", "Garcia");
        BridgePatient b = bridgePatient("pat-B", "Bea", "Lopez");
        when(repository.findById("pat-A")).thenReturn(Optional.empty());
        when(repository.findById("pat-B")).thenReturn(Optional.empty());

        service.upsertAll(List.of(a, b));

        verify(repository, times(2)).save(any(Patient.class));
    }

    private static BridgePatient bridgePatient(String id, String first, String last) {
        BridgePatient p = new BridgePatient();
        p.setPatientId(id);
        p.setFirstName(first);
        p.setLastName(last);
        return p;
    }
}
