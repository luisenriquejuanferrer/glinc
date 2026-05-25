import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GlucosePoint, PatientReading } from '../models/glucose.model';

@Injectable({ providedIn: 'root' })
export class GlucoseService {

  private readonly apiBase = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getPatients(): Observable<PatientReading[]> {
    return this.http.get<PatientReading[]>(this.apiBase + '/patients');
  }

  // includeSynthetic=true incluye las lecturas del seeder demo del backend (source=SEED).
  getHistory(patientId: string, hours: number): Observable<GlucosePoint[]> {
    return this.http.get<GlucosePoint[]>(
      this.apiBase + '/patients/' + patientId + '/history',
      { params: { hours: hours, includeSynthetic: true } }
    );
  }
}
