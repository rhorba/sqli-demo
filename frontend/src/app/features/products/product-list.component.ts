import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { injectQuery, injectMutation, QueryClient } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';
import { ProductService } from '../../core/services/product.service';
import { ProductCategory } from '../../core/models/product.model';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div>
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-2xl font-bold text-gray-900">Products</h2>
        <button class="btn-primary" (click)="showForm.set(true)">+ New Product</button>
      </div>

      <!-- Create Product Form -->
      @if (showForm()) {
        <div class="card mb-6">
          <h3 class="font-semibold text-gray-700 mb-4">Create Product</h3>
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="grid grid-cols-2 gap-4">
            <div class="col-span-2 md:col-span-1">
              <label class="block text-sm font-medium text-gray-700 mb-1">Name *</label>
              <input formControlName="name" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm" placeholder="Product name">
            </div>
            <div class="col-span-2 md:col-span-1">
              <label class="block text-sm font-medium text-gray-700 mb-1">Category *</label>
              <select formControlName="category" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                @for (cat of categories; track cat) {
                  <option [value]="cat">{{ cat }}</option>
                }
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Price (€) *</label>
              <input formControlName="price" type="number" step="0.01" min="0.01"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Stock Quantity *</label>
              <input formControlName="stockQuantity" type="number" min="0"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
            </div>
            <div class="col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea formControlName="description" rows="2"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></textarea>
            </div>
            <div class="col-span-2 flex gap-2 justify-end">
              <button type="button" class="btn-secondary" (click)="showForm.set(false)">Cancel</button>
              <button type="submit" class="btn-primary" [disabled]="form.invalid || createMutation.isPending()">
                {{ createMutation.isPending() ? 'Saving...' : 'Create' }}
              </button>
            </div>
          </form>
        </div>
      }

      <!-- Products Table -->
      <div class="card">
        @if (productsQuery.isLoading()) {
          <p class="text-gray-400 text-sm py-4 text-center">Loading products...</p>
        } @else if (productsQuery.isError()) {
          <p class="text-red-500 text-sm py-4 text-center">Failed to load products. Is product-service running?</p>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-gray-100 text-left">
                  <th class="pb-3 font-medium text-gray-500">Name</th>
                  <th class="pb-3 font-medium text-gray-500">Category</th>
                  <th class="pb-3 font-medium text-gray-500">Price</th>
                  <th class="pb-3 font-medium text-gray-500">Stock</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                @for (product of productsQuery.data(); track product.id) {
                  <tr class="hover:bg-gray-50">
                    <td class="py-3 font-medium text-gray-900">{{ product.name }}</td>
                    <td class="py-3">
                      <span class="badge bg-gray-100 text-gray-600">{{ product.category }}</span>
                    </td>
                    <td class="py-3 font-mono">{{ product.price | currency:'EUR' }}</td>
                    <td class="py-3">
                      <span [class]="product.stockQuantity < 5 ? 'text-red-600 font-medium' : 'text-gray-700'">
                        {{ product.stockQuantity }}
                      </span>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="4" class="py-8 text-center text-gray-400">No products yet.</td></tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>
  `,
})
export class ProductListComponent {
  private productService = inject(ProductService);
  private queryClient = inject(QueryClient);
  private fb = inject(FormBuilder);

  showForm = signal(false);
  categories: ProductCategory[] = ['ELECTRONICS', 'CLOTHING', 'FOOD', 'HOME', 'SPORTS', 'OTHER'];

  form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    price: [null as number | null, [Validators.required, Validators.min(0.01)]],
    stockQuantity: [0, [Validators.required, Validators.min(0)]],
    category: ['ELECTRONICS' as ProductCategory, Validators.required],
  });

  productsQuery = injectQuery(() => ({
    queryKey: ['products'],
    queryFn: () => lastValueFrom(this.productService.getAll()),
  }));

  createMutation = injectMutation(() => ({
    mutationFn: (req: ReturnType<typeof this.form.getRawValue>) =>
      lastValueFrom(this.productService.create({
        name: req.name!,
        description: req.description ?? '',
        price: req.price!,
        stockQuantity: req.stockQuantity!,
        category: req.category!,
      })),
    onSuccess: () => {
      this.queryClient.invalidateQueries({ queryKey: ['products'] });
      this.form.reset({ category: 'ELECTRONICS', stockQuantity: 0 });
      this.showForm.set(false);
    },
  }));

  onSubmit(): void {
    if (this.form.valid) {
      this.createMutation.mutate(this.form.getRawValue());
    }
  }
}
