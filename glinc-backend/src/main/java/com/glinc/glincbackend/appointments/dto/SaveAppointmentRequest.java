package com.glinc.glincbackend.appointments.dto;

import java.time.Instant;

public class SaveAppointmentRequest {

    private Instant appointmentAt;
    private String professional;
    private String reason;

    public SaveAppointmentRequest() {
    }

    public Instant getAppointmentAt() { return appointmentAt; }
    public void setAppointmentAt(Instant appointmentAt) { this.appointmentAt = appointmentAt; }

    public String getProfessional() { return professional; }
    public void setProfessional(String professional) { this.professional = professional; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
