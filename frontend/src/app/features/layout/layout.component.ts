import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="flex min-h-screen">
      <!-- Sidebar -->
      <aside class="w-64 bg-sqli-900 text-white flex flex-col">
        <div class="p-6 border-b border-sqli-700">
          <h1 class="text-xl font-bold">RetailFlow</h1>
          <p class="text-xs text-sqli-300 mt-1">Event-Driven Platform</p>
        </div>
        <nav class="flex-1 p-4 space-y-1">
          <a routerLink="/dashboard" routerLinkActive="bg-sqli-700"
             class="flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-sqli-700 transition-colors text-sm">
            <span>📊</span> Dashboard
          </a>
          <a routerLink="/products" routerLinkActive="bg-sqli-700"
             class="flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-sqli-700 transition-colors text-sm">
            <span>📦</span> Products
          </a>
          <a routerLink="/orders" routerLinkActive="bg-sqli-700"
             class="flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-sqli-700 transition-colors text-sm">
            <span>🛒</span> Orders
          </a>
        </nav>
        <div class="p-4 border-t border-sqli-700 text-xs text-sqli-400">
          <p>Java 21 · Spring Boot 3.4</p>
          <p>Kafka · Angular 18</p>
        </div>
      </aside>

      <!-- Main content -->
      <main class="flex-1 p-8 overflow-auto">
        <router-outlet />
      </main>
    </div>
  `,
})
export class LayoutComponent {}
