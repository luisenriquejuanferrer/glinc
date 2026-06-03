package com.glinc.glincbackend.patient;

import com.glinc.glincbackend.bridge.dto.BridgePatient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

// Mantiene la tabla `caregiver_patients` actualizada en cada login: enlaza al
// cuidador con cada paciente que el bridge le devuelve. Idempotente (skip si ya existe).
@Service
public class CaregiverPatientService {

    private static final Logger log = LoggerFactory.getLogger(CaregiverPatientService.class);

    private final CaregiverPatientRepository repository;

    public CaregiverPatientService(CaregiverPatientRepository repository) {
        this.repository = repository;
    }

    public void linkAll(String caregiverEmail, List<BridgePatient> pacientes) {
        if (caregiverEmail == null || caregiverEmail.isBlank() || pacientes == null) {
            return;
        }
        for (BridgePatient bp : pacientes) {
            link(caregiverEmail, bp);
        }
    }

    private void link(String caregiverEmail, BridgePatient bp) {
        if (bp.getPatientId() == null || bp.getPatientId().isBlank()) {
            return;
        }
        if (repository.existsByCaregiverEmailAndPatientId(caregiverEmail, bp.getPatientId())) {
            return;
        }
        repository.save(new CaregiverPatient(caregiverEmail, bp.getPatientId()));
        log.info("CaregiverPatient enlace: {} <-> {}", caregiverEmail, bp.getPatientId());
    }
}
