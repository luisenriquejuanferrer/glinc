package com.glinc.glincbackend.patient;

import com.glinc.glincbackend.bridge.dto.BridgePatient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaregiverPatientServiceTest {

    private static final String CAREGIVER = "ana@ejemplo.com";

    @Mock
    private CaregiverPatientRepository repository;

    @InjectMocks
    private CaregiverPatientService service;

    @Test
    void linkAll_caregiverNull_noHaceNada() {
        service.linkAll(null, List.of(bridgePatient("pat-1")));

        verifyNoInteractions(repository);
    }

    @Test
    void linkAll_caregiverBlank_noHaceNada() {
        service.linkAll("   ", List.of(bridgePatient("pat-1")));

        verifyNoInteractions(repository);
    }

    @Test
    void linkAll_pacientesNull_noHaceNada() {
        service.linkAll(CAREGIVER, null);

        verifyNoInteractions(repository);
    }

    @Test
    void linkAll_relacionNueva_inserta() {
        when(repository.existsByCaregiverEmailAndPatientId(CAREGIVER, "pat-1")).thenReturn(false);

        service.linkAll(CAREGIVER, List.of(bridgePatient("pat-1")));

        ArgumentCaptor<CaregiverPatient> captor = ArgumentCaptor.forClass(CaregiverPatient.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCaregiverEmail()).isEqualTo(CAREGIVER);
        assertThat(captor.getValue().getPatientId()).isEqualTo("pat-1");
    }

    @Test
    void linkAll_relacionExistente_noInserta() {
        when(repository.existsByCaregiverEmailAndPatientId(CAREGIVER, "pat-1")).thenReturn(true);

        service.linkAll(CAREGIVER, List.of(bridgePatient("pat-1")));

        verify(repository, never()).save(any(CaregiverPatient.class));
    }

    @Test
    void linkAll_pacienteSinId_seSalta() {
        service.linkAll(CAREGIVER, List.of(bridgePatient(null), bridgePatient("   ")));

        verify(repository, never()).save(any(CaregiverPatient.class));
    }

    @Test
    void linkAll_variosPacientes_procesaTodos() {
        when(repository.existsByCaregiverEmailAndPatientId(CAREGIVER, "pat-A")).thenReturn(false);
        when(repository.existsByCaregiverEmailAndPatientId(CAREGIVER, "pat-B")).thenReturn(true);
        when(repository.existsByCaregiverEmailAndPatientId(CAREGIVER, "pat-C")).thenReturn(false);

        service.linkAll(CAREGIVER, List.of(
                bridgePatient("pat-A"),
                bridgePatient("pat-B"),
                bridgePatient("pat-C")));

        verify(repository, times(2)).save(any(CaregiverPatient.class));
    }

    private static BridgePatient bridgePatient(String id) {
        BridgePatient p = new BridgePatient();
        p.setPatientId(id);
        p.setFirstName("Ana");
        p.setLastName("Garcia");
        return p;
    }
}
