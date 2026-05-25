package com.glinc.glincbackend.bridge.dto;

import java.util.List;

public class SessionResponse {

    private String sessionId;
    private String email;
    private List<BridgePatient> patients;
    private String traceId;

    public SessionResponse() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
