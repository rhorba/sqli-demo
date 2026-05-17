import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';
import { ProductService } from '../../core/services/product.service';
import { OrderService } from '../../core/services/order.service';
import { StockService } from '../../core/services/stock.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div>
      <h2 class="text-2xl font-bold text-gray-900 mb-6">Dashboard</h2>

      <!-- KPI Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div class="card">
          <p class="text-sm font-medium text-gray-500">Total Products</p>
          <p class="text-3xl font-bold text-sqli-600 mt-1">
            {{ productsQuery.isLoading() ? '...' : productsQuery.data()?.length ?? 0 }}
          </p>
        </div>
        <div class="card">
          <p class="text-sm font-medium text-gray-500">Total Orders</p>
          <p class="text-3xl font-bold text-sqli-600 mt-1">
            {{ ordersQuery.isLoading() ? '...' : ordersQuery.data()?.length ?? 0 }}
          </p>
        </div>
        <div class="card">
          <p class="text-sm font-medium text-gray-500">Pending Orders</p>
          <p class="text-3xl font-bold text-yellow-500 mt-1">{{ pendingCount() }}</p>
        </div>
      </div>

      <!-- Architecture badge -->
      <div class="card mb-8">
        <h3 class="font-semibold text-gray-700 mb-3">Architecture Stack</h3>
        <div class="flex flex-wrap gap-2">
          @for (tag of techStack; track tag) {
            <span class="bg-sqli-50 text-sqli-700 text-xs font-medium px-2.5 py-1 rounded-full border border-sqli-200">
              {{ tag }}
            </span>
          }
        </div>
      </div>

      <!-- Recent Orders -->
      <div class="card">
        <h3 class="font-semibold text-gray-700 mb-4">Recent Orders</h3>
        @if (ordersQuery.isLoading()) {
          <p class="text-gray-400 text-sm">Loading...</p>
        } @else if ((ordersQuery.data()?.length ?? 0) === 0) {
          <p class="text-gray-400 text-sm">No orders yet. Place your first order!</p>
        } @else {
          <div class="space-y-2">
            @for (order of (ordersQuery.data() ?? []).slice(0, 5); track order.id) {
              <div class="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                <span class="text-sm font-mono text-gray-500">{{ order.id.slice(0, 8) }}...</span>
                <span [class]="'badge-' + order.status.toLowerCase()">{{ order.status }}</span>
                <span class="text-sm font-semibold text-gray-700">{{ order.totalAmount | currency:'EUR' }}</span>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
})
export class DashboardComponent {
  private productService = inject(ProductService);
  private orderService = inject(OrderService);
  private stockService = inject(StockService);

  productsQuery = injectQuery(() => ({
    queryKey: ['products'],
    queryFn: () => lastValueFrom(this.productService.getAll()),
  }));

  ordersQuery = injectQuery(() => ({
    queryKey: ['orders'],
    queryFn: () => lastValueFrom(this.orderService.getAll()),
  }));

  pendingCount = computed(() =>
    (this.ordersQuery.data() ?? []).filter(o => o.status === 'PENDING').length
  );

  techStack = [
    'Java 21', 'Spring Boot 3.4', 'Hexagonal Architecture',
    'Apache Kafka', 'PostgreSQL', 'Flyway', 'MapStruct',
    'Angular 18', 'TanStack Query', 'Tailwind CSS', 'Docker'
  ];
}
