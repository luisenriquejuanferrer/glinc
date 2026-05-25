package com.glinc.glincbackend.bridge.dto;

import java.util.List;

public class PatientsResponse {

    private int count;
    private List<PatientWithReading> patients;
    private String traceId;

    public PatientsResponse() {
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<PatientWithReading> getPatients() {
        return patients;
    }

    public void setPatients(List<PatientWithReading> patients) {
        this.patients = patients;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
