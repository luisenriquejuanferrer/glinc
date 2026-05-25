import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import {
  PREFERENCES_DEFAULT,
  UserPreferences,
} from '../models/preferences.model';

@Injectable({ providedIn: 'root' })
export class PreferencesService {

  private readonly storageKey = 'glinc.preferences';

  private subject = new BehaviorSubject<UserPreferences>(this.leerDeStorage());
  preferences$: Observable<UserPreferences> = this.subject.asObservable();

  snapshot(): UserPreferences {
    return this.subject.value;
  }

  update(parcial: Partial<UserPreferences>): void {
    const nuevas: UserPreferences = { ...this.subject.value, ...parcial };
    if (nuevas.umbralBajo >= nuevas.umbralAlto) {
      console.warn('Umbrales invalidos (bajo >= alto), se guardan igualmente.');
    }
    localStorage.setItem(this.storageKey, JSON.stringify(nuevas));
    this.subject.next(nuevas);
  }

  // Merge con defaults para que un localStorage antiguo sin campos nuevos no rompa el contrato.
  private leerDeStorage(): UserPreferences {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      return { ...PREFERENCES_DEFAULT };
    }
    try {
      const parseado = JSON.parse(raw) as Partial<UserPreferences>;
      return { ...PREFERENCES_DEFAULT, ...parseado };
    } catch {
      return { ...PREFERENCES_DEFAULT };
    }
  }
}
