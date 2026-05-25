package com.glinc.glincbackend.api.dto;

import java.time.Instant;

public class ReadingDto {

    private final int mgDl;
    // mmol no se persiste; se calcula al construir el DTO para evitar fuentes de datos divergentes.
    private final double mmol;
    private final String trend;
    private final Instant readAt;

    public ReadingDto(int mgDl, String trend, Instant readAt) {
        this.mgDl = mgDl;
        this.mmol = Math.round((mgDl / 18.0) * 10) / 10.0;
        this.trend = trend;
        this.readAt = readAt;
    }

    public int getMgDl() {
        return mgDl;
    }

    public double getMmol() {
        return mmol;
    }

    public String getTrend() {
        return trend;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
