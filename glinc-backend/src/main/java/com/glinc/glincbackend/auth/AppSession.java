package com.glinc.glincbackend.auth;

import com.glinc.glincbackend.bridge.dto.BridgePatient;
import com.glinc.glincbackend.user.CaregiverRole;

import java.time.Instant;
import java.util.List;

public class AppSession {

    private final String email;
    private final String bridgeSessionId;
    private final List<BridgePatient> patients;
    private final Instant expiresAt;

    // Mutable: se fija en el login y se actualiza si el usuario cambia de rol
    // (PUT /api/user/role) para que los guards 403 sean correctos sin re-login.
    // null = el usuario aun no ha elegido rol.
    private CaregiverRole role;

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

    public CaregiverRole getRole() {
        return role;
    }

    public void setRole(CaregiverRole role) {
        this.role = role;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
