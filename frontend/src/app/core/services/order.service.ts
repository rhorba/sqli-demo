import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order, PlaceOrderRequest } from '../models/order.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.orderServiceUrl}/orders`;

  getAll(): Observable<Order[]> {
    return this.http.get<Order[]>(this.baseUrl);
  }

  getById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/${id}`);
  }

  place(request: PlaceOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.baseUrl, request);
  }

  confirm(id: string): Observable<Order> {
    return this.http.patch<Order>(`${this.baseUrl}/${id}/confirm`, {});
  }

  ship(id: string): Observable<Order> {
    return this.http.patch<Order>(`${this.baseUrl}/${id}/ship`, {});
  }
}
