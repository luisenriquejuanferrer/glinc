package com.glinc.glincbackend.appointments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "patient_appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "patient_id", nullable = false, length = 255)
    private String patientId;

    @Column(name = "appointment_at", nullable = false)
    private Instant appointmentAt;

    @Column(name = "professional", nullable = false, length = 120)
    private String professional;

    @Column(name = "reason", length = 300)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Appointment() {
    }

    public Appointment(String userEmail, String patientId,
                       Instant appointmentAt, String professional, String reason) {
        this.userEmail = userEmail;
        this.patientId = patientId;
        this.appointmentAt = appointmentAt;
        this.professional = professional;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        Instant ahora = Instant.now();
        this.createdAt = ahora;
        this.updatedAt = ahora;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public String getPatientId() { return patientId; }
    public Instant getAppointmentAt() { return appointmentAt; }
    public String getProfessional() { return professional; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setAppointmentAt(Instant appointmentAt) { this.appointmentAt = appointmentAt; }
    public void setProfessional(String professional) { this.professional = professional; }
    public void setReason(String reason) { this.reason = reason; }
}
