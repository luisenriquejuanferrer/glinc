package com.glinc.glincbackend.cgm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "glucose_readings")
public class GlucoseReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "mg_dl", nullable = false)
    private int mgDl;

    @Column(name = "trend", nullable = false, length = 20)
    private String trend;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private ReadingSource source;

    // Siempre en UTC; el frontend la pasa a hora local al mostrar.
    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    public GlucoseReading() {
    }

    public GlucoseReading(String patientId, String firstName, String lastName,
                          int mgDl, String trend, ReadingSource source, Instant readAt) {
        this.patientId = patientId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.mgDl = mgDl;
        this.trend = trend;
        this.source = source;
        this.readAt = readAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getMgDl() {
        return mgDl;
    }

    public void setMgDl(int mgDl) {
        this.mgDl = mgDl;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public ReadingSource getSource() {
        return source;
    }

    public void setSource(ReadingSource source) {
        this.source = source;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
