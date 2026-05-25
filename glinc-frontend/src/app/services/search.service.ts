import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SearchService {

  private termSubject = new BehaviorSubject<string>('');
  term$: Observable<string> = this.termSubject.asObservable();

  setTerm(valor: string): void {
    this.termSubject.next(valor ?? '');
  }

  clear(): void {
    this.termSubject.next('');
  }
}
