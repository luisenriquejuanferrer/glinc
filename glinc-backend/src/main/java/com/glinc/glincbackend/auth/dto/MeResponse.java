package com.glinc.glincbackend.auth.dto;

import com.glinc.glincbackend.bridge.dto.BridgePatient;

import java.time.Instant;
import java.util.List;

public class MeResponse {

    private String email;
    private List<BridgePatient> patients;
    private Instant expiresAt;

    public MeResponse() {
    }

    public MeResponse(String email, List<BridgePatient> patients, Instant expiresAt) {
        this.email = email;
        this.patients = patients;
        this.expiresAt = expiresAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<BridgePatient> getPatients() {
        return patients;
    }

    public void setPatients(List<BridgePatient> patients) {
        this.patients = patients;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
