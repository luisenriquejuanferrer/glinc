// CAREGIVER = padres/tutores (dashboard ligero + inventario + citas).
// DOCTOR = médico (vista clínica con más gráficas, sin inventario ni citas).
export type CaregiverRole = 'CAREGIVER' | 'DOCTOR';

export interface UserProfile {
  email: string;
  firstName: string | null;
  lastName: string | null;
  birthDate: string | null;
  phone: string | null;
  // null = el usuario aún no ha elegido rol → se muestra el modal de selección.
  role: CaregiverRole | null;
  updatedAt: string;
}

export interface UpdateUserProfileRequest {
  firstName: string | null;
  lastName: string | null;
  birthDate: string | null;
  phone: string | null;
}
