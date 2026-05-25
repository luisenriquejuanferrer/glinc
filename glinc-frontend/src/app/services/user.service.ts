import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import {
  UpdateUserProfileRequest,
  UserProfile,
} from '../models/user.model';

// BehaviorSubject mantiene el ultimo perfil emitido; sidebar y Settings se mantienen sincronizados sin recargar.
@Injectable({ providedIn: 'root' })
export class UserService {

  private readonly apiBase = 'http://localhost:8080/api';

  private profileSubject = new BehaviorSubject<UserProfile | null>(null);
  profile$: Observable<UserProfile | null> = this.profileSubject.asObservable();

  constructor(private http: HttpClient) {}

  refresh(): Observable<UserProfile> {
    return this.http
      .get<UserProfile>(this.apiBase + '/user/profile')
      .pipe(tap((perfil) => this.profileSubject.next(perfil)));
  }

  update(cambios: UpdateUserProfileRequest): Observable<UserProfile> {
    return this.http
      .put<UserProfile>(this.apiBase + '/user/profile', cambios)
      .pipe(tap((perfil) => this.profileSubject.next(perfil)));
  }

  snapshot(): UserProfile | null {
    return this.profileSubject.value;
  }

  clear(): void {
    this.profileSubject.next(null);
  }
}
