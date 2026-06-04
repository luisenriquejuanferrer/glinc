package com.glinc.glincbackend.user;

// CAREGIVER = padres/tutores (dashboard ligero + inventario + citas).
// DOCTOR = medico (vista clinica con mas graficas, sin inventario ni citas).
// Se persiste como texto via @Enumerated(EnumType.STRING) en caregivers.role.
public enum CaregiverRole {
    CAREGIVER,
    DOCTOR
}
