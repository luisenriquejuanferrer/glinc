package com.glinc.glincbackend.bridge.dto;

import java.time.Instant;

public class BridgeReading {

    private String patientId;
    private int mgDl;
    private double mmol;
    private String trend;
    private Instant timestamp;

    public BridgeReading() {
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public int getMgDl() {
        return mgDl;
    }

    public void setMgDl(int mgDl) {
        this.mgDl = mgDl;
    }

    public double getMmol() {
        return mmol;
    }

    public void setMmol(double mmol) {
        this.mmol = mmol;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
