export type Trend =
  | 'rising_fast'
  | 'rising'
  | 'flat'
  | 'falling'
  | 'falling_fast'
  | 'unknown';

export interface PatientReading {
  patientId: string;
  firstName: string;
  lastName: string;
  mgDl: number;
  mmol: number;
  trend: Trend;
  readAt: string;
  age?: number;
  relation?: string;
}

export interface GlucosePoint {
  mgDl: number;
  mmol: number;
  trend: Trend;
  readAt: string;
}
