import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';
import { BeneficioSitioAdmin, IconoBeneficioSitio } from '../../../core/admin/admin.service';
import { SiteBenefitsFacade } from './site-benefits.facade';

@Component({
  selector: 'app-site-benefits',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule],
  providers: [SiteBenefitsFacade],
  templateUrl: './site-benefits.component.html',
  styleUrl: './site-benefits.component.css'
})
export class SiteBenefitsComponent implements OnInit {
  private readonly facade = inject(SiteBenefitsFacade);
  readonly beneficios = signal<BeneficioSitioAdmin[]>([]);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly iconos: Array<{ valor: IconoBeneficioSitio; etiqueta: string; clase: string }> = [
    { valor: 'ESTRELLA', etiqueta: 'Estrella', clase: 'bi bi-star' },
    { valor: 'CORAZON', etiqueta: 'Corazón', clase: 'bi bi-heart' },
    { valor: 'ESCUDO', etiqueta: 'Confianza', clase: 'bi bi-shield-check' },
    { valor: 'DESTELLO', etiqueta: 'Destello', clase: 'bi bi-stars' },
    { valor: 'RELOJ', etiqueta: 'Tiempo', clase: 'bi bi-clock' },
    { valor: 'PERSONA', etiqueta: 'Atención', clase: 'bi bi-person-heart' },
    { valor: 'HOJA', etiqueta: 'Bienestar', clase: 'bi bi-flower1' },
    { valor: 'PREMIO', etiqueta: 'Calidad', clase: 'bi bi-award' }
  ];

  ngOnInit(): void {
    this.facade.cargar().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: beneficios => this.beneficios.set(beneficios),
      error: () => this.error.set('No fue posible cargar los beneficios.')
    });
  }

  agregar(): void {
    if (this.beneficios().length >= 8) return;
    this.beneficios.update(items => [...items, {
      id: null, titulo: '', descripcion: '', icono: 'DESTELLO', activo: true, orden: items.length
    }]);
    this.limpiarEstado();
  }

  quitar(indice: number): void {
    this.beneficios.update(items => items.filter((_, i) => i !== indice).map((item, orden) => ({ ...item, orden })));
    this.limpiarEstado();
  }

  mover(indice: number, desplazamiento: -1 | 1): void {
    const items = [...this.beneficios()];
    const destino = indice + desplazamiento;
    if (destino < 0 || destino >= items.length) return;
    [items[indice], items[destino]] = [items[destino], items[indice]];
    this.beneficios.set(items.map((item, orden) => ({ ...item, orden })));
    this.limpiarEstado();
  }

  iconoClase(icono: IconoBeneficioSitio): string {
    return this.iconos.find(item => item.valor === icono)?.clase ?? 'bi bi-stars';
  }

  guardar(): void {
    const items = this.beneficios();
    if (this.guardando() || items.some(item => !item.titulo.trim() || !item.descripcion.trim())) return;
    this.guardando.set(true);
    this.limpiarEstado();
    this.facade.guardar({ beneficios: items.map((item, orden) => ({
      titulo: item.titulo.trim(), descripcion: item.descripcion.trim(), icono: item.icono, activo: item.activo, orden
    })) }).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: beneficios => {
        this.beneficios.set(beneficios);
        this.mensaje.set('Los beneficios quedaron actualizados.');
      },
      error: err => this.error.set(err?.error?.message || err?.error?.detail || 'No fue posible guardar los beneficios.')
    });
  }

  private limpiarEstado(): void {
    this.mensaje.set(null);
    this.error.set(null);
  }
}
