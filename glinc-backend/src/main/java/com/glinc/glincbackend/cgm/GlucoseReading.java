package com.glinc.glincbackend.cgm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// El nombre del paciente vive en la tabla `patients` desde V6;
// aqui solo queda la serie temporal (patient_id + lectura + timestamp).
@Entity
@Table(name = "glucose_readings")
public class GlucoseReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "mg_dl", nullable = false)
    private int mgDl;

    @Column(name = "trend", nullable = false, length = 20)
    private String trend;

    // Siempre en UTC; el frontend la pasa a hora local al mostrar.
    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    public GlucoseReading() {
    }

    public GlucoseReading(String patientId, int mgDl, String trend, Instant readAt) {
        this.patientId = patientId;
        this.mgDl = mgDl;
        this.trend = trend;
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

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
