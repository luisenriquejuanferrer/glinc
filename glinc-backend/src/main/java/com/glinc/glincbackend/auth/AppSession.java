package com.glinc.glincbackend.auth;

import com.glinc.glincbackend.bridge.dto.BridgePatient;

import java.time.Instant;
import java.util.List;

public class AppSession {

    private final String email;
    private final String bridgeSessionId;
    private final List<BridgePatient> patients;
    private final Instant expiresAt;

    public AppSession(String email, String bridgeSessionId,
                      List<BridgePatient> patients, Instant expiresAt) {
        this.email = email;
        this.bridgeSessionId = bridgeSessionId;
        this.patients = patients;
        this.expiresAt = expiresAt;
    }

    public String getEmail() {
        return email;
    }

    public String getBridgeSessionId() {
        return bridgeSessionId;
    }

    public List<BridgePatient> getPatients() {
        return patients;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
