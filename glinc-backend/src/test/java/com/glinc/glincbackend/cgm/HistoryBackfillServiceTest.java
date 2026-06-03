package com.glinc.glincbackend.cgm;

import com.glinc.glincbackend.bridge.BridgeClient;
import com.glinc.glincbackend.bridge.BridgeException;
import com.glinc.glincbackend.bridge.dto.BridgePatient;
import com.glinc.glincbackend.bridge.dto.BridgeReading;
import com.glinc.glincbackend.bridge.dto.HistoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryBackfillServiceTest {

    private static final String BRIDGE_SESSION_ID = "bridge-session-xyz";
    private static final String PATIENT_ID = "pat-1";
    private static final String PATIENT_FIRST_NAME = "Ana";
    private static final String PATIENT_LAST_NAME = "Garcia";

    @Mock
    private BridgeClient bridgeClient;

    @Mock
    private GlucoseReadingRepository repository;

    @InjectMocks
    private HistoryBackfillService service;

    @BeforeEach
    void setUp() {
        // Sin estos a 0 el test esperaria 4s + backoff exponencial entre reintentos.
        service.setEsperasMs(0L, 0L);
    }

    @Test
    void backfillPaciente_siempreLlamaAlBridge_aunqueHayaLecturasRecientes() {
        // Caso: usuario relogeo tras 1h. Bridge debe llamarse igual para cerrar el gap.
        when(bridgeClient.fetchHistory(BRIDGE_SESSION_ID, PATIENT_ID))
                .thenReturn(historial());

        service.backfillPaciente(BRIDGE_SESSION_ID, patient(PATIENT_ID));

        verify(bridgeClient, times(1)).fetchHistory(BRIDGE_SESSION_ID, PATIENT_ID);
    }

    @Test
    void backfillPaciente_guardaTodasLasDelBridge() {
        when(repository.existsByPatientIdAndReadAt(eq(PATIENT_ID), any(Instant.class)))
                .thenReturn(false);

        Instant t1 = Instant.parse("2026-06-01T08:00:00Z");
        Instant t2 = Instant.parse("2026-06-01T08:15:00Z");
        Instant t3 = Instant.parse("2026-06-01T08:30:00Z");
        when(bridgeClient.fetchHistory(BRIDGE_SESSION_ID, PATIENT_ID))
                .thenReturn(historial(
                        bridgeReading(120, "flat", t1),
                        bridgeReading(135, "rising", t2),
                        bridgeReading(150, "rising", t3)));

        service.backfillPaciente(BRIDGE_SESSION_ID, patient(PATIENT_ID));

        ArgumentCaptor<GlucoseReading> captor = ArgumentCaptor.forClass(GlucoseReading.class);
        verify(repository, times(3)).save(captor.capture());

        List<GlucoseReading> guardadas = captor.getAllValues();
        assertThat(guardadas).extracting(GlucoseReading::getMgDl).containsExactly(120, 135, 150);
        assertThat(guardadas).extracting(GlucoseReading::getReadAt).containsExactly(t1, t2, t3);
        assertThat(guardadas).extracting(GlucoseReading::getPatientId).containsOnly(PATIENT_ID);
    }

    @Test
    void backfillPaciente_dedupSkipExistentes() {
        Instant t1 = Instant.parse("2026-06-01T08:00:00Z");
        Instant t2 = Instant.parse("2026-06-01T08:15:00Z");
        Instant t3 = Instant.parse("2026-06-01T08:30:00Z");
        when(bridgeClient.fetchHistory(BRIDGE_SESSION_ID, PATIENT_ID))
                .thenReturn(historial(
                        bridgeReading(120, "flat", t1),
                        bridgeReading(135, "rising", t2),
                        bridgeReading(150, "rising", t3)));
        when(repository.existsByPatientIdAndReadAt(PATIENT_ID, t1)).thenReturn(false);
        when(repository.existsByPatientIdAndReadAt(PATIENT_ID, t2)).thenReturn(true);
        when(repository.existsByPatientIdAndReadAt(PATIENT_ID, t3)).thenReturn(false);

        service.backfillPaciente(BRIDGE_SESSION_ID, patient(PATIENT_ID));

        ArgumentCaptor<GlucoseReading> captor = ArgumentCaptor.forClass(GlucoseReading.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(GlucoseReading::getReadAt).containsExactly(t1, t3);
    }

    @Test
    void backfillPaciente_ignoraLecturasSinTimestamp() {
        Instant t1 = Instant.parse("2026-06-01T08:00:00Z");
        when(bridgeClient.fetchHistory(BRIDGE_SESSION_ID, PATIENT_ID))
                .thenReturn(historial(
                        bridgeReading(120, "flat", t1),
                        bridgeReading(135, "rising", null)));
        when(repository.existsByPatientIdAndReadAt(PATIENT_ID, t1)).thenReturn(false);

        service.backfillPaciente(BRIDGE_SESSION_ID, patient(PATIENT_ID));

        verify(repository, times(1)).save(any(GlucoseReading.class));
    }

    @Test
    void backfillPaciente_historialVacioDelBridge_noGuardaNada() {
        when(bridgeClient.fetchHistory(BRIDGE_SESSION_ID, PATIENT_ID))
                .thenReturn(historial());

        service.backfillPaciente(BRIDGE_SESSION_ID, patient(PATIENT_ID));

        verify(repository, never()).save(any(GlucoseReading.class));
    }

    @Test
    void pedirHistorialConReintentos_exitoSegundoIntento() {
        HistoryResponse ok = historial(bridgeReading(120, "flat", Instant.now()));
        when(bridgeClient.fetchHistory(BRIDGE_SESSION_ID, PATIENT_ID))
                .thenThrow(new BridgeException("503 primer intento"))
                .thenReturn(ok);

        HistoryResponse resultado = service.pedirHistorialConReintentos(BRIDGE_SESSION_ID, PATIENT_ID);

        assertThat(resultado).isSameAs(ok);
        verify(bridgeClient, times(2)).fetchHistory(BRIDGE_SESSION_ID, PATIENT_ID);
    }

    @Test
    void pedirHistorialConReintentos_fallaTresVeces_devuelveNull() {
        when(bridgeClient.fetchHistory(BRIDGE_SESSION_ID, PATIENT_ID))
                .thenThrow(new BridgeException("503 fallo persistente"));

        HistoryResponse resultado = service.pedirHistorialConReintentos(BRIDGE_SESSION_ID, PATIENT_ID);

        assertThat(resultado).isNull();
        verify(bridgeClient, times(3)).fetchHistory(BRIDGE_SESSION_ID, PATIENT_ID);
    }

    @Test
    void backfillSesion_patientsNull_noHaceNada() {
        com.glinc.glincbackend.auth.AppSession sesion = new com.glinc.glincbackend.auth.AppSession(
                "user@test.com", BRIDGE_SESSION_ID, null, Instant.now().plusSeconds(3600));

        service.backfillSesion(sesion);

        verifyNoInteractions(bridgeClient);
        verifyNoInteractions(repository);
    }

    @Test
    void backfillSesion_unPacienteFalla_losDemasSiguen() {
        BridgePatient pacA = patient("pat-A");
        BridgePatient pacB = patient("pat-B");
        com.glinc.glincbackend.auth.AppSession sesion = new com.glinc.glincbackend.auth.AppSession(
                "user@test.com", BRIDGE_SESSION_ID, List.of(pacA, pacB),
                Instant.now().plusSeconds(3600));

        // pat-A: agota reintentos (bridge falla siempre).
        when(bridgeClient.fetchHistory(BRIDGE_SESSION_ID, "pat-A"))
                .thenThrow(new BridgeException("503"));

        // pat-B: tiene exito y guarda 1 lectura.
        Instant t = Instant.parse("2026-06-01T09:00:00Z");
        when(bridgeClient.fetchHistory(BRIDGE_SESSION_ID, "pat-B"))
                .thenReturn(historial(bridgeReading(140, "flat", t)));
        when(repository.existsByPatientIdAndReadAt("pat-B", t)).thenReturn(false);

        service.backfillSesion(sesion);

        verify(bridgeClient, times(3)).fetchHistory(BRIDGE_SESSION_ID, "pat-A");
        verify(bridgeClient, times(1)).fetchHistory(BRIDGE_SESSION_ID, "pat-B");
        ArgumentCaptor<GlucoseReading> captor = ArgumentCaptor.forClass(GlucoseReading.class);
        verify(repository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getPatientId()).isEqualTo("pat-B");
    }

    // Helpers ---------------------------------------------------------------

    private static BridgePatient patient(String patientId) {
        BridgePatient p = new BridgePatient();
        p.setPatientId(patientId);
        p.setFirstName(PATIENT_FIRST_NAME);
        p.setLastName(PATIENT_LAST_NAME);
        return p;
    }

    private static BridgeReading bridgeReading(int mgDl, String trend, Instant timestamp) {
        BridgeReading r = new BridgeReading();
        r.setMgDl(mgDl);
        r.setTrend(trend);
        r.setTimestamp(timestamp);
        return r;
    }

    private static HistoryResponse historial(BridgeReading... lecturas) {
        HistoryResponse h = new HistoryResponse();
        List<BridgeReading> lista = new ArrayList<>();
        for (BridgeReading r : lecturas) {
            lista.add(r);
        }
        h.setReadings(lista);
        h.setCount(lista.size());
        return h;
    }
}
