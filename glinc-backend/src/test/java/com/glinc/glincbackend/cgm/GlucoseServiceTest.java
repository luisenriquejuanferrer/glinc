package com.glinc.glincbackend.cgm;

import com.glinc.glincbackend.api.dto.PatientLatestDto;
import com.glinc.glincbackend.api.dto.ReadingDto;
import com.glinc.glincbackend.auth.AppSession;
import com.glinc.glincbackend.bridge.dto.BridgePatient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlucoseServiceTest {

    @Mock
    private GlucoseReadingRepository repository;

    @InjectMocks
    private GlucoseService service;

    @Test
    void obtenerDashboard_sesionSinPacientes_devuelveListaVacia() {
        AppSession sesion = new AppSession("user@test.com", "bridge-id", null,
                Instant.now().plusSeconds(3600));

        List<PatientLatestDto> resultado = service.obtenerDashboard(sesion);

        assertThat(resultado).isEmpty();
        verify(repository, never()).findFirstByPatientIdOrderByReadAtDesc(any());
    }

    @Test
    void obtenerDashboard_pacientesConLecturas_construyeDtos() {
        BridgePatient pacA = patient("pat-A", "Ana", "Garcia");
        BridgePatient pacB = patient("pat-B", "Bea", "Lopez");
        AppSession sesion = new AppSession("user@test.com", "bridge-id",
                List.of(pacA, pacB), Instant.now().plusSeconds(3600));

        Instant t = Instant.parse("2026-06-01T09:00:00Z");
        when(repository.findFirstByPatientIdOrderByReadAtDesc("pat-A"))
                .thenReturn(reading("pat-A", 120, "flat", t));
        when(repository.findFirstByPatientIdOrderByReadAtDesc("pat-B"))
                .thenReturn(reading("pat-B", 95, "rising", t));

        List<PatientLatestDto> resultado = service.obtenerDashboard(sesion);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getPatientId()).isEqualTo("pat-A");
        assertThat(resultado.get(0).getMgDl()).isEqualTo(120);
        assertThat(resultado.get(0).getFirstName()).isEqualTo("Ana");
        assertThat(resultado.get(0).getMmol()).isEqualTo(6.7);
        assertThat(resultado.get(1).getPatientId()).isEqualTo("pat-B");
        assertThat(resultado.get(1).getMgDl()).isEqualTo(95);
    }

    @Test
    void obtenerDashboard_pacienteSinLecturas_seSalta() {
        BridgePatient pacA = patient("pat-A", "Ana", "Garcia");
        BridgePatient pacB = patient("pat-B", "Bea", "Lopez");
        AppSession sesion = new AppSession("user@test.com", "bridge-id",
                List.of(pacA, pacB), Instant.now().plusSeconds(3600));

        when(repository.findFirstByPatientIdOrderByReadAtDesc("pat-A")).thenReturn(null);
        when(repository.findFirstByPatientIdOrderByReadAtDesc("pat-B"))
                .thenReturn(reading("pat-B", 95, "rising", Instant.now()));

        List<PatientLatestDto> resultado = service.obtenerDashboard(sesion);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPatientId()).isEqualTo("pat-B");
    }

    @Test
    void obtenerHistorico_devuelveDtosOrdenadosTalCualDelRepo() {
        Instant t = Instant.parse("2026-06-01T09:00:00Z");
        when(repository.findByPatientIdAndReadAtAfterOrderByReadAtAsc(
                eq("pat-1"), any(Instant.class)))
                .thenReturn(List.of(
                        reading("pat-1", 100, "flat", t),
                        reading("pat-1", 110, "rising", t.plusSeconds(900))));

        List<ReadingDto> resultado = service.obtenerHistorico("pat-1", 24);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getMgDl()).isEqualTo(100);
        assertThat(resultado.get(0).getTrend()).isEqualTo("flat");
        assertThat(resultado.get(1).getMgDl()).isEqualTo(110);
        assertThat(resultado.get(1).getTrend()).isEqualTo("rising");
    }

    @Test
    void obtenerHistorico_ventanaDeHorasSeRestaDelInstantAhora() {
        when(repository.findByPatientIdAndReadAtAfterOrderByReadAtAsc(
                eq("pat-1"), any(Instant.class)))
                .thenReturn(List.of());

        Instant antes = Instant.now();
        service.obtenerHistorico("pat-1", 168);
        Instant despues = Instant.now();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).findByPatientIdAndReadAtAfterOrderByReadAtAsc(
                eq("pat-1"), captor.capture());

        Instant desde = captor.getValue();
        long horasRestadas = (despues.toEpochMilli() - desde.toEpochMilli()) / 3_600_000L;
        assertThat(horasRestadas).isBetween(167L, 169L);
        assertThat(desde).isBefore(antes);
    }

    private static BridgePatient patient(String id, String first, String last) {
        BridgePatient p = new BridgePatient();
        p.setPatientId(id);
        p.setFirstName(first);
        p.setLastName(last);
        return p;
    }

    private static GlucoseReading reading(String patientId, int mgDl, String trend, Instant readAt) {
        return new GlucoseReading(patientId, mgDl, trend, readAt);
    }
}
