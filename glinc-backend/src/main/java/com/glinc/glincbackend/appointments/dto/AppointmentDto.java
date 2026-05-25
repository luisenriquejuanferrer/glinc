package com.glinc.glincbackend.appointments.dto;

import java.time.Instant;

public class AppointmentDto {

    private Long id;
    private Instant appointmentAt;
    private String professional;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;

    public AppointmentDto() {
    }

    public AppointmentDto(Long id, Instant appointmentAt, String professional,
                          String reason, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.appointmentAt = appointmentAt;
        this.professional = professional;
        this.reason = reason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getAppointmentAt() { return appointmentAt; }
    public void setAppointmentAt(Instant appointmentAt) { this.appointmentAt = appointmentAt; }
    public String getProfessional() { return professional; }
    public void setProfessional(String professional) { this.professional = professional; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
