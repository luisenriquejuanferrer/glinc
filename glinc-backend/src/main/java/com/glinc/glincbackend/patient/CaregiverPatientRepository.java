package com.glinc.glincbackend.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaregiverPatientRepository extends JpaRepository<CaregiverPatient, Long> {

    boolean existsByCaregiverEmailAndPatientId(String caregiverEmail, String patientId);
}
