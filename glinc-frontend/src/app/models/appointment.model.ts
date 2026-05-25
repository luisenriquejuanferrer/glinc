export interface Appointment {
  id: number;
  appointmentAt: string;
  professional: string;
  reason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SaveAppointmentRequest {
  appointmentAt: string;
  professional: string;
  reason: string | null;
}
