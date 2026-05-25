export interface AuthPatient {
  patientId: string;
  firstName: string;
  lastName: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  patients: AuthPatient[];
  expiresAt: string;
}

export interface MeResponse {
  email: string;
  patients: AuthPatient[];
  expiresAt: string;
}

export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  code?: string;
  detail?: string;
}
