package com.glinc.glincbackend.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

// Relacion N:N entre caregivers y patients. Unicidad (caregiver_email, patient_id)
// se impone con UNIQUE constraint en BD. Se popula en cada login.
@Entity
@Table(name = "caregiver_patients")
public class CaregiverPatient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "caregiver_email", nullable = false, length = 255)
    private String caregiverEmail;

    @Column(name = "patient_id", nullable = false, length = 255)
    private String patientId;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    public CaregiverPatient() {
    }

    public CaregiverPatient(String caregiverEmail, String patientId) {
        this.caregiverEmail = caregiverEmail;
        this.patientId = patientId;
    }

    @PrePersist
    void onCreate() {
        this.linkedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCaregiverEmail() {
        return caregiverEmail;
    }

    public String getPatientId() {
        return patientId;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}
