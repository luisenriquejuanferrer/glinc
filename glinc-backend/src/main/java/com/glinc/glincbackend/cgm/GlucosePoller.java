package com.glinc.glincbackend.cgm;

import com.glinc.glincbackend.auth.AppSession;
import com.glinc.glincbackend.auth.AppSessionStore;
import com.glinc.glincbackend.bridge.BridgeClient;
import com.glinc.glincbackend.bridge.BridgeException;
import com.glinc.glincbackend.bridge.dto.BridgeReading;
import com.glinc.glincbackend.bridge.dto.PatientWithReading;
import com.glinc.glincbackend.bridge.dto.PatientsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GlucosePoller {

    private static final Logger log = LoggerFactory.getLogger(GlucosePoller.class);

    private final AppSessionStore sessionStore;
    private final BridgeClient bridgeClient;
    private final GlucoseReadingRepository repository;

    public GlucosePoller(AppSessionStore sessionStore,
                         BridgeClient bridgeClient,
                         GlucoseReadingRepository repository) {
        this.sessionStore = sessionStore;
        this.bridgeClient = bridgeClient;
        this.repository = repository;
    }

    // fixedDelayString = intervalo desde que TERMINA la ejecucion anterior, evita solapamientos.
    @Scheduled(fixedDelayString = "${cgm.poll-interval-ms}")
    public void recogerLecturas() {
        int nuevas = 0;

        for (AppSession sesion : sessionStore.listarSesiones()) {
            if (sesion.isExpired()) {
                continue;
            }
            nuevas += procesarSesion(sesion);
        }

        if (nuevas > 0) {
            log.info("Poller: {} lecturas reales nuevas guardadas", nuevas);
        }
    }

    private int procesarSesion(AppSession sesion) {
        PatientsResponse respuesta;
        try {
            respuesta = bridgeClient.fetchPatients(sesion.getBridgeSessionId());
        } catch (BridgeException e) {
            log.warn("Poller: no se pudieron traer lecturas de la sesion bridge {}: {}",
                    sesion.getBridgeSessionId(), e.getMessage());
            return 0;
        }

        if (respuesta.getPatients() == null) {
            return 0;
        }

        int nuevas = 0;
        for (PatientWithReading paciente : respuesta.getPatients()) {
            BridgeReading lectura = paciente.getReading();
            if (lectura == null || lectura.getTimestamp() == null) {
                continue;
            }
            if (guardarSiEsNueva(paciente, lectura)) {
                nuevas++;
            }
        }
        return nuevas;
    }

    private boolean guardarSiEsNueva(PatientWithReading paciente, BridgeReading lectura) {
        boolean yaExiste = repository.existsByPatientIdAndReadAt(
                paciente.getPatientId(), lectura.getTimestamp());
        if (yaExiste) {
            return false;
        }

        GlucoseReading fila = new GlucoseReading(
                paciente.getPatientId(),
                lectura.getMgDl(),
                lectura.getTrend(),
                lectura.getTimestamp());
        repository.save(fila);
        return true;
    }
}
