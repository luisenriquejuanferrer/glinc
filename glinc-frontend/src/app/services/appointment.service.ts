import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Appointment, SaveAppointmentRequest } from '../models/appointment.model';

@Injectable({ providedIn: 'root' })
export class AppointmentService {

  private readonly apiBase = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  list(patientId: string): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(this.base(patientId));
  }

  create(patientId: string, body: SaveAppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(this.base(patientId), body);
  }

  update(patientId: string, id: number, body: SaveAppointmentRequest): Observable<Appointment> {
    return this.http.put<Appointment>(this.base(patientId) + '/' + id, body);
  }

  remove(patientId: string, id: number): Observable<void> {
    return this.http.delete<void>(this.base(patientId) + '/' + id);
  }

  private base(patientId: string): string {
    return this.apiBase + '/patients/'
      + encodeURIComponent(patientId) + '/appointments';
  }
}
