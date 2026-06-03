package com.glinc.glincbackend.appointments;

import com.glinc.glincbackend.appointments.dto.AppointmentDto;
import com.glinc.glincbackend.appointments.dto.SaveAppointmentRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// Devuelve la lista entera ordenada por fecha desc; el frontend separa proximas/pasadas con la hora local.
// Desde V7 las citas son per-paciente (compartidas entre cuidadores que tengan acceso al paciente).
@Service
public class AppointmentService {

    private final AppointmentRepository repository;

    public AppointmentService(AppointmentRepository repository) {
        this.repository = repository;
    }

    public List<AppointmentDto> list(String patientId) {
        List<Appointment> filas = repository
                .findByPatientIdOrderByAppointmentAtDesc(patientId);
        List<AppointmentDto> resultado = new ArrayList<>();
        for (Appointment a : filas) {
            resultado.add(toDto(a));
        }
        return resultado;
    }

    public AppointmentDto create(String patientId, SaveAppointmentRequest dto) {
        Appointment nueva = new Appointment(
                patientId,
                dto.getAppointmentAt(),
                dto.getProfessional().trim(),
                normalizar(dto.getReason()));
        return toDto(repository.save(nueva));
    }

    public AppointmentDto update(Long id, SaveAppointmentRequest dto) {
        Appointment cita = repository.findById(id).orElse(null);
        if (cita == null) {
            return null;
        }
        cita.setAppointmentAt(dto.getAppointmentAt());
        cita.setProfessional(dto.getProfessional().trim());
        cita.setReason(normalizar(dto.getReason()));
        return toDto(repository.save(cita));
    }

    public boolean delete(Long id) {
        Appointment cita = repository.findById(id).orElse(null);
        if (cita == null) {
            return false;
        }
        repository.delete(cita);
        return true;
    }

    private AppointmentDto toDto(Appointment a) {
        return new AppointmentDto(
                a.getId(),
                a.getAppointmentAt(),
                a.getProfessional(),
                a.getReason(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
