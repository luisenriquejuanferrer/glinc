package com.glinc.glincbackend.cgm;

import com.glinc.glincbackend.api.dto.PatientLatestDto;
import com.glinc.glincbackend.api.dto.ReadingDto;
import com.glinc.glincbackend.auth.AppSession;
import com.glinc.glincbackend.bridge.dto.BridgePatient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class GlucoseService {

    private final GlucoseReadingRepository repository;

    public GlucoseService(GlucoseReadingRepository repository) {
        this.repository = repository;
    }

    public List<PatientLatestDto> obtenerDashboard(AppSession sesion) {
        List<PatientLatestDto> resultado = new ArrayList<>();
        if (sesion.getPatients() == null) {
            return resultado;
        }

        for (BridgePatient paciente : sesion.getPatients()) {
            GlucoseReading ultima = repository.findFirstByPatientIdOrderByReadAtDesc(
                    paciente.getPatientId());
            if (ultima == null) {
                continue;
            }

            resultado.add(new PatientLatestDto(
                    paciente.getPatientId(),
                    paciente.getFirstName(),
                    paciente.getLastName(),
                    ultima.getMgDl(),
                    ultima.getTrend(),
                    ultima.getReadAt()));
        }
        return resultado;
    }

    // includeSynthetic=true incluye lecturas SEED ademas de REAL.
    public List<ReadingDto> obtenerHistorico(String patientId, int hours,
                                             boolean includeSynthetic) {
        Instant desde = Instant.now().minus(hours, ChronoUnit.HOURS);

        List<GlucoseReading> lecturas;
        if (includeSynthetic) {
            lecturas = repository.findByPatientIdAndReadAtAfterOrderByReadAtAsc(
                    patientId, desde);
        } else {
            lecturas = repository.findByPatientIdAndSourceAndReadAtAfterOrderByReadAtAsc(
                    patientId, ReadingSource.REAL, desde);
        }

        List<ReadingDto> dtos = new ArrayList<>();
        for (GlucoseReading lectura : lecturas) {
            dtos.add(new ReadingDto(
                    lectura.getMgDl(),
                    lectura.getTrend(),
                    lectura.getReadAt()));
        }
        return dtos;
    }
}
