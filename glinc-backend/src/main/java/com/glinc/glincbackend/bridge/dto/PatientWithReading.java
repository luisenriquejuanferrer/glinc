package com.glinc.glincbackend.bridge.dto;

public class PatientWithReading {

    private String patientId;
    private String firstName;
    private String lastName;
    private BridgeReading reading;

    public PatientWithReading() {
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public BridgeReading getReading() {
        return reading;
    }

    public void setReading(BridgeReading reading) {
        this.reading = reading;
    }
}
