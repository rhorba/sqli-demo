import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Stock } from '../models/stock.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class StockService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.inventoryServiceUrl}/stock`;

  getAll(): Observable<Stock[]> {
    return this.http.get<Stock[]>(this.baseUrl);
  }
}
