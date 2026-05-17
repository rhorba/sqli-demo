import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormArray } from '@angular/forms';
import { injectQuery, injectMutation, QueryClient } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';
import { OrderService } from '../../core/services/order.service';
import { ProductService } from '../../core/services/product.service';
import { Order } from '../../core/models/order.model';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div>
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-2xl font-bold text-gray-900">Orders</h2>
        <button class="btn-primary" (click)="showForm.set(true)">+ Place Order</button>
      </div>

      <!-- Place Order Form -->
      @if (showForm()) {
        <div class="card mb-6">
          <h3 class="font-semibold text-gray-700 mb-4">Place Order</h3>
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Customer ID *</label>
              <input formControlName="customerId" placeholder="UUID of customer"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm font-mono">
            </div>

            <!-- Items -->
            <div>
              <div class="flex items-center justify-between mb-2">
                <label class="text-sm font-medium text-gray-700">Items *</label>
                <button type="button" (click)="addItem()"
                  class="text-xs text-sqli-600 hover:text-sqli-700 font-medium">+ Add Item</button>
              </div>
              <div formArrayName="items" class="space-y-2">
                @for (item of itemsArray.controls; track $index) {
                  <div [formGroupName]="$index" class="grid grid-cols-4 gap-2 bg-gray-50 p-3 rounded-lg">
                    <div class="col-span-4 md:col-span-1">
                      <select formControlName="productId" (change)="onProductSelect($index)"
                        class="w-full border border-gray-300 rounded px-2 py-1 text-sm">
                        <option value="">Select product...</option>
                        @for (p of productsQuery.data() ?? []; track p.id) {
                          <option [value]="p.id">{{ p.name }}</option>
                        }
                      </select>
                    </div>
                    <input formControlName="productName" placeholder="Product name" readonly
                      class="border border-gray-300 rounded px-2 py-1 text-sm bg-gray-100">
                    <input formControlName="quantity" type="number" min="1" placeholder="Qty"
                      class="border border-gray-300 rounded px-2 py-1 text-sm">
                    <div class="flex gap-1">
                      <input formControlName="unitPrice" type="number" step="0.01" placeholder="Price"
                        class="flex-1 border border-gray-300 rounded px-2 py-1 text-sm">
                      <button type="button" (click)="removeItem($index)"
                        class="text-red-400 hover:text-red-600 px-1">✕</button>
                    </div>
                  </div>
                }
              </div>
            </div>

            <div class="flex gap-2 justify-end">
              <button type="button" class="btn-secondary" (click)="showForm.set(false)">Cancel</button>
              <button type="submit" class="btn-primary"
                [disabled]="form.invalid || placeMutation.isPending() || itemsArray.length === 0">
                {{ placeMutation.isPending() ? 'Placing...' : 'Place Order' }}
              </button>
            </div>
          </form>
        </div>
      }

      <!-- Orders Table -->
      <div class="card">
        @if (ordersQuery.isLoading()) {
          <p class="text-gray-400 text-sm py-4 text-center">Loading orders...</p>
        } @else if (ordersQuery.isError()) {
          <p class="text-red-500 text-sm py-4 text-center">Failed to load orders. Is order-service running?</p>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-gray-100 text-left">
                  <th class="pb-3 font-medium text-gray-500">Order ID</th>
                  <th class="pb-3 font-medium text-gray-500">Status</th>
                  <th class="pb-3 font-medium text-gray-500">Total</th>
                  <th class="pb-3 font-medium text-gray-500">Items</th>
                  <th class="pb-3 font-medium text-gray-500">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                @for (order of ordersQuery.data(); track order.id) {
                  <tr class="hover:bg-gray-50">
                    <td class="py-3 font-mono text-xs text-gray-500">{{ order.id.slice(0, 8) }}...</td>
                    <td class="py-3">
                      <span [class]="'badge-' + order.status.toLowerCase()">{{ order.status }}</span>
                    </td>
                    <td class="py-3 font-semibold">{{ order.totalAmount | currency:'EUR' }}</td>
                    <td class="py-3 text-gray-500">{{ order.items.length }} item(s)</td>
                    <td class="py-3">
                      <div class="flex gap-2">
                        @if (order.status === 'PENDING') {
                          <button class="text-xs text-blue-600 hover:underline"
                            (click)="confirm(order.id)">Confirm</button>
                        }
                        @if (order.status === 'CONFIRMED') {
                          <button class="text-xs text-purple-600 hover:underline"
                            (click)="ship(order.id)">Ship</button>
                        }
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="5" class="py-8 text-center text-gray-400">No orders yet.</td></tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>
  `,
})
export class OrderListComponent {
  private orderService = inject(OrderService);
  private productService = inject(ProductService);
  private queryClient = inject(QueryClient);
  private fb = inject(FormBuilder);

  showForm = signal(false);

  form = this.fb.group({
    customerId: ['', Validators.required],
    items: this.fb.array([]),
  });

  get itemsArray() { return this.form.get('items') as FormArray; }

  productsQuery = injectQuery(() => ({
    queryKey: ['products'],
    queryFn: () => lastValueFrom(this.productService.getAll()),
  }));

  ordersQuery = injectQuery(() => ({
    queryKey: ['orders'],
    queryFn: () => lastValueFrom(this.orderService.getAll()),
  }));

  placeMutation = injectMutation(() => ({
    mutationFn: (req: { customerId: string; items: any[] }) =>
      lastValueFrom(this.orderService.place(req)),
    onSuccess: () => {
      this.queryClient.invalidateQueries({ queryKey: ['orders'] });
      this.queryClient.invalidateQueries({ queryKey: ['stock'] });
      this.form.reset({ customerId: '' });
      while (this.itemsArray.length) this.itemsArray.removeAt(0);
      this.showForm.set(false);
    },
  }));

  confirmMutation = injectMutation(() => ({
    mutationFn: (id: string) => lastValueFrom(this.orderService.confirm(id)),
    onSuccess: () => this.queryClient.invalidateQueries({ queryKey: ['orders'] }),
  }));

  shipMutation = injectMutation(() => ({
    mutationFn: (id: string) => lastValueFrom(this.orderService.ship(id)),
    onSuccess: () => this.queryClient.invalidateQueries({ queryKey: ['orders'] }),
  }));

  addItem(): void {
    this.itemsArray.push(this.fb.group({
      productId: ['', Validators.required],
      productName: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unitPrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
    }));
  }

  removeItem(i: number): void { this.itemsArray.removeAt(i); }

  onProductSelect(i: number): void {
    const productId = this.itemsArray.at(i).get('productId')?.value;
    const product = (this.productsQuery.data() ?? []).find(p => p.id === productId);
    if (product) {
      this.itemsArray.at(i).patchValue({ productName: product.name, unitPrice: product.price });
    }
  }

  onSubmit(): void {
    if (this.form.valid) {
      this.placeMutation.mutate(this.form.getRawValue() as any);
    }
  }

  confirm(id: string): void { this.confirmMutation.mutate(id); }
  ship(id: string): void { this.shipMutation.mutate(id); }
}
