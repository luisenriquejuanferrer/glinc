package com.glinc.glincbackend.patient;

import com.glinc.glincbackend.bridge.dto.BridgePatient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

// Mantiene sincronizada la tabla maestra `patients` con los pacientes que el
// bridge descubre en cada login. Solo inserta nuevos o actualiza nombres si
// LibreLink los cambio (evita updated_at innecesarios).
@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository repository;

    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    public void upsertAll(List<BridgePatient> pacientes) {
        if (pacientes == null) {
            return;
        }
        for (BridgePatient bp : pacientes) {
            upsert(bp);
        }
    }

    private void upsert(BridgePatient bp) {
        if (bp.getPatientId() == null || bp.getPatientId().isBlank()) {
            return;
        }
        Patient existente = repository.findById(bp.getPatientId()).orElse(null);
        if (existente == null) {
            Patient nuevo = new Patient(bp.getPatientId(), bp.getFirstName(), bp.getLastName());
            repository.save(nuevo);
            log.info("Patient nuevo: {} ({} {})", bp.getPatientId(),
                    bp.getFirstName(), bp.getLastName());
            return;
        }
        boolean cambio = false;
        if (!Objects.equals(existente.getFirstName(), bp.getFirstName())) {
            existente.setFirstName(bp.getFirstName());
            cambio = true;
        }
        if (!Objects.equals(existente.getLastName(), bp.getLastName())) {
            existente.setLastName(bp.getLastName());
            cambio = true;
        }
        if (cambio) {
            repository.save(existente);
            log.info("Patient actualizado: {} ({} {})", bp.getPatientId(),
                    bp.getFirstName(), bp.getLastName());
        }
    }
}
