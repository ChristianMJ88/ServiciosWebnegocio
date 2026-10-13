import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DATE_LOCALE, MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';
import { ConfiguracionMediaAdmin, PromocionSitioAdmin, ServicioAdmin, SucursalAdmin } from '../../../core/admin/admin.service';
import { SitePromotionsFacade } from './site-promotions.facade';

@Component({
  selector: 'app-site-promotions',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatDatepickerModule, MatNativeDateModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule],
  providers: [SitePromotionsFacade, { provide: MAT_DATE_LOCALE, useValue: 'es-MX' }],
  templateUrl: './site-promotions.component.html',
  styleUrl: './site-promotions.component.css'
})
export class SitePromotionsComponent implements OnInit {
  private readonly facade = inject(SitePromotionsFacade);
  private readonly fechas = new Map<string, Date>();
  readonly abiertas = signal<ReadonlySet<PromocionSitioAdmin>>(new Set());
  readonly promociones = signal<PromocionSitioAdmin[]>([]);
  readonly servicios = signal<ServicioAdmin[]>([]);
  readonly sucursales = signal<SucursalAdmin[]>([]);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly subiendoImagen = signal<PromocionSitioAdmin | null>(null);
  readonly configuracionMedia = signal<ConfiguracionMediaAdmin | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.facade.cargar().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: respuesta => {
        this.promociones.set(respuesta.promociones);
        this.servicios.set(respuesta.servicios.filter(servicio => servicio.activo && servicio.visiblePublico));
        this.sucursales.set(respuesta.sucursales.filter(sucursal => sucursal.activa));
        this.configuracionMedia.set(respuesta.media);
      },
      error: () => this.error.set('No fue posible cargar las promociones.')
    });
  }

  agregar(): void {
    const nueva: PromocionSitioAdmin = {
      id: null, titulo: '', descripcion: null, mediaId: null, imagenUrl: null, fechaInicio: null, fechaFin: null,
      tipoDescuento: 'PORCENTAJE', valorDescuento: 10, moneda: this.monedaPredeterminada(),
      textoBoton: 'Reservar promoción', activa: true, vigente: true, orden: this.promociones().length,
      servicioIds: [], sucursalIds: []
    };
    this.promociones.update(promociones => [...promociones, nueva]);
    this.alternar(nueva, true);
    this.limpiarEstado();
  }

  quitar(indice: number): void {
    const item = this.promociones()[indice];
    if (item) this.alternar(item, false);
    this.promociones.update(promociones => {
      const restantes = promociones.filter((_, i) => i !== indice);
      restantes.forEach((item, orden) => item.orden = orden);
      return restantes;
    });
    this.limpiarEstado();
  }

  mover(indice: number, desplazamiento: -1 | 1): void {
    const promociones = [...this.promociones()];
    const destino = indice + desplazamiento;
    if (destino < 0 || destino >= promociones.length) return;
    [promociones[indice], promociones[destino]] = [promociones[destino], promociones[indice]];
    promociones.forEach((item, orden) => item.orden = orden);
    this.promociones.set(promociones);
    this.limpiarEstado();
  }

  guardar(): void {
    if (this.guardando() || this.promociones().some(item => !item.titulo.trim() || item.valorDescuento <= 0)) return;
    if (this.promociones().some(item => item.fechaInicio && item.fechaFin && item.fechaFin < item.fechaInicio)) {
      this.error.set('La fecha final no puede ser anterior a la fecha de inicio.');
      return;
    }
    this.guardando.set(true);
    this.limpiarEstado();
    this.facade.guardar({
      promociones: this.promociones().map((item, orden) => ({
        id: item.id,
        titulo: item.titulo.trim(), descripcion: item.descripcion?.trim() || null,
        mediaId: item.mediaId, imagenUrl: item.imagenUrl?.trim() || null, fechaInicio: item.fechaInicio || null,
        fechaFin: item.fechaFin || null, tipoDescuento: item.tipoDescuento,
        valorDescuento: item.valorDescuento, moneda: item.moneda.trim().toUpperCase(),
        textoBoton: item.textoBoton?.trim() || 'Reservar promoción', activa: item.activa,
        orden, servicioIds: item.servicioIds, sucursalIds: item.sucursalIds
      }))
    }).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: promociones => {
        this.abiertas.set(new Set());
        this.fechas.clear();
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

  seleccionarImagen(evento: Event, item: PromocionSitioAdmin): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo || this.subiendoImagen() !== null) return;
    const configuracion = this.configuracionMedia();
    if (!configuracion?.cargaHabilitada) {
      this.error.set('La carga de imágenes todavía no está habilitada en la plataforma.');
      return;
    }
    if (archivo.size > configuracion.maxBytesImagen) {
      this.error.set(`La imagen supera el máximo de ${this.megabytes(configuracion.maxBytesImagen)} MB.`);
      return;
    }
    this.subiendoImagen.set(item);
    this.limpiarEstado();
    this.facade.subirImagen(archivo).pipe(finalize(() => this.subiendoImagen.set(null))).subscribe({
      next: media => {
        item.mediaId = media.id;
        item.imagenUrl = media.secureUrl;
        this.mensaje.set('Imagen cargada. Guarda las promociones para publicarla.');
      },
      error: err => this.error.set(err?.error?.message || err?.error?.detail || 'No fue posible cargar la imagen.')
    });
  }

  quitarImagen(item: PromocionSitioAdmin): void {
    item.mediaId = null;
    item.imagenUrl = null;
    this.limpiarEstado();
  }

  actualizarUrlManual(item: PromocionSitioAdmin, url: string): void {
    item.mediaId = null;
    item.imagenUrl = url;
    this.limpiarEstado();
  }

  fechaComoDate(valor: string | null): Date | null {
    if (!valor) return null;
    const existente = this.fechas.get(valor);
    if (existente) return existente;
    const [anio, mes, dia] = valor.split('-').map(Number);
    if (!anio || !mes || !dia) return null;
    const fecha = new Date(anio, mes - 1, dia);
    this.fechas.set(valor, fecha);
    return fecha;
  }

  alternar(item: PromocionSitioAdmin, abierta: boolean): void {
    if (this.abiertas().has(item) === abierta) return;
    this.abiertas.update(actuales => {
      const siguientes = new Set(actuales);
      if (abierta) siguientes.add(item);
      else siguientes.delete(item);
      return siguientes;
    });
  }

  actualizarFecha(item: PromocionSitioAdmin, campo: 'fechaInicio' | 'fechaFin', valor: Date | null): void {
    item[campo] = valor ? this.formatearFecha(valor) : null;
    this.limpiarEstado();
  }

  megabytes(bytes: number): string {
    return (bytes / 1024 / 1024).toFixed(bytes % (1024 * 1024) === 0 ? 0 : 1);
  }

  private hoy(): string { return new Date().toISOString().slice(0, 10); }
  private formatearFecha(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
  }
  private monedaPredeterminada(): string { return this.servicios().find(item => item.moneda)?.moneda || 'MXN'; }
  private limpiarEstado(): void { this.mensaje.set(null); this.error.set(null); }
}
