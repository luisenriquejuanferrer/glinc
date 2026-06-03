package com.glinc.glincbackend.bridge.dto;

import java.util.List;

public class HistoryResponse {

    private String patientId;
    private int count;
    private List<BridgeReading> readings;
    private String traceId;

    public HistoryResponse() {
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<BridgeReading> getReadings() {
        return readings;
    }

    public void setReadings(List<BridgeReading> readings) {
        this.readings = readings;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
