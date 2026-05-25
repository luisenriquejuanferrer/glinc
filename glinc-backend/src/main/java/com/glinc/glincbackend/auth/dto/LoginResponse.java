package com.glinc.glincbackend.auth.dto;

import com.glinc.glincbackend.bridge.dto.BridgePatient;

import java.time.Instant;
import java.util.List;

public class LoginResponse {

    private String token;
    private String email;
    private List<BridgePatient> patients;
    private Instant expiresAt;

    public LoginResponse() {
    }

    public LoginResponse(String token, String email,
                         List<BridgePatient> patients, Instant expiresAt) {
        this.token = token;
        this.email = email;
        this.patients = patients;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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
