package com.glinc.glincbackend.appointments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserEmailAndPatientIdOrderByAppointmentAtDesc(
            String userEmail, String patientId);

    // Filtra por dueño para que un cuidador no pueda tocar citas ajenas conociendo el id.
    Optional<Appointment> findByIdAndUserEmail(Long id, String userEmail);
}
