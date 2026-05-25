package com.glinc.glincbackend.api.dto;

import java.time.Instant;

public class PatientLatestDto {

    private final String patientId;
    private final String firstName;
    private final String lastName;
    private final int mgDl;
    // mmol no se persiste; se calcula al construir el DTO para evitar fuentes de datos divergentes.
    private final double mmol;
    private final String trend;
    private final Instant readAt;

    public PatientLatestDto(String patientId, String firstName, String lastName,
                            int mgDl, String trend, Instant readAt) {
        this.patientId = patientId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.mgDl = mgDl;
        this.mmol = Math.round((mgDl / 18.0) * 10) / 10.0;
        this.trend = trend;
        this.readAt = readAt;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getMgDl() {
        return mgDl;
    }

    public double getMmol() {
        return mmol;
    }

    public String getTrend() {
        return trend;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
