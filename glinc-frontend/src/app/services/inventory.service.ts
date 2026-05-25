import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  InventoryItem,
  InventoryItemType,
  UpdateInventoryRequest,
} from '../models/inventory.model';

@Injectable({ providedIn: 'root' })
export class InventoryService {

  private readonly apiBase = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  list(patientId: string): Observable<InventoryItem[]> {
    return this.http.get<InventoryItem[]>(
      this.apiBase + '/patients/' + encodeURIComponent(patientId) + '/inventory',
    );
  }

  update(
    patientId: string,
    type: InventoryItemType,
    cambios: UpdateInventoryRequest,
  ): Observable<InventoryItem> {
    return this.http.put<InventoryItem>(
      this.apiBase + '/patients/' + encodeURIComponent(patientId)
        + '/inventory/' + type,
      cambios,
    );
  }
}
