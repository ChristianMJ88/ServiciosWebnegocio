import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';
import { PromocionSitioAdmin, ServicioAdmin, SucursalAdmin } from '../../../core/admin/admin.service';
import { SitePromotionsFacade } from './site-promotions.facade';

@Component({
  selector: 'app-site-promotions',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule],
  providers: [SitePromotionsFacade],
  templateUrl: './site-promotions.component.html',
  styleUrl: './site-promotions.component.css'
})
export class SitePromotionsComponent implements OnInit {
  private readonly facade = inject(SitePromotionsFacade);
  readonly promociones = signal<PromocionSitioAdmin[]>([]);
  readonly servicios = signal<ServicioAdmin[]>([]);
  readonly sucursales = signal<SucursalAdmin[]>([]);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.facade.cargar().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: respuesta => {
        this.promociones.set(respuesta.promociones);
        this.servicios.set(respuesta.servicios.filter(servicio => servicio.activo && servicio.visiblePublico));
        this.sucursales.set(respuesta.sucursales.filter(sucursal => sucursal.activa));
      },
      error: () => this.error.set('No fue posible cargar las promociones.')
    });
  }

  agregar(): void {
    this.promociones.update(promociones => [...promociones, {
      id: null, titulo: '', descripcion: null, imagenUrl: null, fechaInicio: null, fechaFin: null,
      tipoDescuento: 'PORCENTAJE', valorDescuento: 10, moneda: this.monedaPredeterminada(),
      textoBoton: 'Reservar promoción', activa: true, vigente: true, orden: promociones.length,
      servicioIds: [], sucursalIds: []
    }]);
    this.limpiarEstado();
  }

  quitar(indice: number): void {
    this.promociones.update(promociones => promociones.filter((_, i) => i !== indice).map((item, orden) => ({ ...item, orden })));
    this.limpiarEstado();
  }

  mover(indice: number, desplazamiento: -1 | 1): void {
    const promociones = [...this.promociones()];
    const destino = indice + desplazamiento;
    if (destino < 0 || destino >= promociones.length) return;
    [promociones[indice], promociones[destino]] = [promociones[destino], promociones[indice]];
    this.promociones.set(promociones.map((item, orden) => ({ ...item, orden })));
    this.limpiarEstado();
  }

  guardar(): void {
    if (this.guardando() || this.promociones().some(item => !item.titulo.trim() || item.valorDescuento <= 0)) return;
    this.guardando.set(true);
    this.limpiarEstado();
    this.facade.guardar({
      promociones: this.promociones().map((item, orden) => ({
        titulo: item.titulo.trim(), descripcion: item.descripcion?.trim() || null,
        imagenUrl: item.imagenUrl?.trim() || null, fechaInicio: item.fechaInicio || null,
        fechaFin: item.fechaFin || null, tipoDescuento: item.tipoDescuento,
        valorDescuento: item.valorDescuento, moneda: item.moneda.trim().toUpperCase(),
        textoBoton: item.textoBoton?.trim() || 'Reservar promoción', activa: item.activa,
        orden, servicioIds: item.servicioIds, sucursalIds: item.sucursalIds
      }))
    }).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: promociones => {
        this.promociones.set(promociones);
        this.mensaje.set('Las promociones quedaron actualizadas.');
      },
      error: error => this.error.set(error?.error?.message || 'No fue posible guardar las promociones.')
    });
  }

  estado(item: PromocionSitioAdmin): string {
    if (!item.activa) return 'Oculta';
    if (item.fechaInicio && item.fechaInicio > this.hoy()) return 'Programada';
    if (item.fechaFin && item.fechaFin < this.hoy()) return 'Finalizada';
    return 'Visible';
  }

  private hoy(): string { return new Date().toISOString().slice(0, 10); }
  private monedaPredeterminada(): string { return this.servicios().find(item => item.moneda)?.moneda || 'MXN'; }
  private limpiarEstado(): void { this.mensaje.set(null); this.error.set(null); }
}
