import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { PlatformAdminService, ResumenPlataforma, TenantPlataforma } from '../../core/platform/platform-admin.service';
import { PlatformAuthService } from '../../core/platform/platform-auth.service';

@Component({
  selector: 'app-platform-dashboard', standalone: true, imports: [CommonModule, FormsModule],
  templateUrl: './platform-dashboard.component.html', styleUrl: './platform-dashboard.component.css'
})
export class PlatformDashboardComponent implements OnInit {
  private readonly api = inject(PlatformAdminService);
  readonly auth = inject(PlatformAuthService);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly resumen = signal<ResumenPlataforma | null>(null);
  readonly tenants = signal<TenantPlataforma[]>([]);
  readonly menuAbierto = signal(false);
  readonly busqueda = signal('');
  readonly filtrados = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    return termino ? this.tenants().filter(item => `${item.nombre} ${item.slug} ${item.estado}`.toLowerCase().includes(termino)) : this.tenants();
  });

  ngOnInit(): void { this.cargar(); }
  cargar(): void {
    this.cargando.set(true); this.error.set(null);
    forkJoin({ resumen: this.api.resumen(), tenants: this.api.tenants() }).pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: datos => { this.resumen.set(datos.resumen); this.tenants.set(datos.tenants); },
      error: error => this.error.set(error?.error?.message || error?.error?.detail || 'No fue posible cargar Fluora Plataforma.')
    });
  }
  moneda(valor: number, codigo: string | null): string {
    if (!codigo) return valor === 0 ? '$0.00' : 'Varias monedas';
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency: codigo }).format(valor);
  }
}
