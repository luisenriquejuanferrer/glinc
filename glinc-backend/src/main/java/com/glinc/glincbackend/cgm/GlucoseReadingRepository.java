package com.glinc.glincbackend.cgm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface GlucoseReadingRepository extends JpaRepository<GlucoseReading, Long> {

    boolean existsByPatientIdAndReadAt(String patientId, Instant readAt);

    GlucoseReading findFirstByPatientIdOrderByReadAtDesc(String patientId);

    List<GlucoseReading> findByPatientIdAndReadAtAfterOrderByReadAtAsc(
            String patientId, Instant desde);
}
