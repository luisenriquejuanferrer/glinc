package com.glinc.glincbackend.cgm;

import com.glinc.glincbackend.auth.AppSession;
import com.glinc.glincbackend.bridge.BridgeClient;
import com.glinc.glincbackend.bridge.BridgeException;
import com.glinc.glincbackend.bridge.dto.BridgePatient;
import com.glinc.glincbackend.bridge.dto.BridgeReading;
import com.glinc.glincbackend.bridge.dto.HistoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// En cada login, rellena los huecos del historico de glucosa pidiendo /history
// al bridge para cada paciente. Corre en background para no bloquear la respuesta
// del login; los fallos por paciente se loguean y no abortan el resto.
@Service
public class HistoryBackfillService {

    private static final Logger log = LoggerFactory.getLogger(HistoryBackfillService.class);

    private static final int REINTENTOS_MAX = 3;

    private final BridgeClient bridgeClient;
    private final GlucoseReadingRepository repository;

    // Configurables para los tests: tests llaman setEsperasMs(0, 0) para evitar las
    // esperas reales. En produccion mantenemos los 4s iniciales y 4s de backoff base
    // que necesita el bridge tras el POST /sessions.
    private long esperaInicialMs = 4000;
    private long backoffBaseMs = 4000;

    public HistoryBackfillService(BridgeClient bridgeClient,
                                  GlucoseReadingRepository repository) {
        this.bridgeClient = bridgeClient;
        this.repository = repository;
    }

    // Solo para tests: permite ejecutar el backfill sin sleeps de varios segundos.
    void setEsperasMs(long esperaInicialMs, long backoffBaseMs) {
        this.esperaInicialMs = esperaInicialMs;
        this.backoffBaseMs = backoffBaseMs;
    }

    public void backfillAsync(AppSession sesion) {
        CompletableFuture.runAsync(() -> backfillSesion(sesion));
    }

    void backfillSesion(AppSession sesion) {
        if (sesion.getPatients() == null) {
            return;
        }
        // Espera a que los runtimes del bridge terminen su login con LibreLink
        // antes de pedir historico. Sin esto el bridge responde 503 (jwt aun no listo).
        try {
            Thread.sleep(esperaInicialMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        for (BridgePatient paciente : sesion.getPatients()) {
            try {
                backfillPaciente(sesion.getBridgeSessionId(), paciente);
            } catch (Exception e) {
                log.warn("Backfill fallo para paciente {}: {}",
                        paciente.getPatientId(), e.getMessage());
            }
        }
    }

    void backfillPaciente(String bridgeSessionId, BridgePatient paciente) {
        String patientId = paciente.getPatientId();

        // Siempre se pide historico al bridge: cubre el hueco entre logout y re-login,
        // por corto que sea (logout 10:29 → re-login 11:33 deja gap de 1h sin lecturas
        // del poller). El bridge cachea /history 3 min y el dedup absorbe redundantes.
        HistoryResponse respuesta = pedirHistorialConReintentos(bridgeSessionId, patientId);
        if (respuesta == null) {
            return;
        }

        List<BridgeReading> lecturas = respuesta.getReadings();
        if (lecturas == null || lecturas.isEmpty()) {
            log.info("Backfill paciente {}: historico vacio en el bridge", patientId);
            return;
        }

        int guardadas = 0;
        for (BridgeReading lectura : lecturas) {
            if (lectura.getTimestamp() == null) {
                continue;
            }
            if (repository.existsByPatientIdAndReadAt(patientId, lectura.getTimestamp())) {
                continue;
            }
            repository.save(new GlucoseReading(
                    patientId,
                    lectura.getMgDl(),
                    lectura.getTrend(),
                    lectura.getTimestamp()));
            guardadas++;
        }

        log.info("Backfill paciente {}: {} lecturas nuevas guardadas (de {} recibidas)",
                patientId, guardadas, lecturas.size());
    }

    HistoryResponse pedirHistorialConReintentos(String bridgeSessionId, String patientId) {
        long delay = backoffBaseMs;
        for (int intento = 1; intento <= REINTENTOS_MAX; intento++) {
            try {
                return bridgeClient.fetchHistory(bridgeSessionId, patientId);
            } catch (BridgeException e) {
                if (intento == REINTENTOS_MAX) {
                    log.warn("Backfill: agotados reintentos para {}: {}", patientId, e.getMessage());
                    return null;
                }
                log.info("Backfill: intento {}/{} fallo para {}, reintentando en {}ms",
                        intento, REINTENTOS_MAX, patientId, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                delay *= 2;
            }
        }
        return null;
    }

}
