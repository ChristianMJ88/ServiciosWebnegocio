import { CommonModule } from '@angular/common';
import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';
import {
  IconoIndicadorConfianza,
  IndicadorConfianzaSitioAdmin,
  ResenaSitioAdmin,
  ResenasSitioAdmin
} from '../../../core/admin/admin.service';
import { SiteReviewsFacade } from './site-reviews.facade';

@Component({
  selector: 'app-site-reviews',
  standalone: true,
  imports: [CommonModule, FormsModule, CdkDropList, CdkDrag, CdkDragHandle, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule],
  providers: [SiteReviewsFacade],
  templateUrl: './site-reviews.component.html',
  styleUrl: './site-reviews.component.css'
})
export class SiteReviewsComponent implements OnInit {
  private readonly facade = inject(SiteReviewsFacade);
  readonly limiteResenas = 20;
  readonly limiteIndicadores = 4;
  readonly configuracion = signal<ResenasSitioAdmin>({ googleMapsUrl: null, resenas: [], indicadores: [] });
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly subiendoFoto = signal<number | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly iconos: Array<{ valor: IconoIndicadorConfianza; etiqueta: string; clase: string }> = [
    { valor: 'ESTRELLA', etiqueta: 'Calificación', clase: 'bi bi-star' },
    { valor: 'PERSONAS', etiqueta: 'Clientes', clase: 'bi bi-people' },
    { valor: 'EXPERIENCIA', etiqueta: 'Experiencia', clase: 'bi bi-calendar2-check' },
    { valor: 'PREMIO', etiqueta: 'Distintivo', clase: 'bi bi-award' },
    { valor: 'CORAZON', etiqueta: 'Preferencia', clase: 'bi bi-heart' },
    { valor: 'ESCUDO', etiqueta: 'Confianza', clase: 'bi bi-shield-check' }
  ];

  ngOnInit(): void {
    this.facade.cargar().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: configuracion => this.configuracion.set(configuracion),
      error: () => this.error.set('No fue posible cargar las reseñas.')
    });
  }

  agregarResena(): void {
    if (this.configuracion().resenas.length >= this.limiteResenas) return;
    this.configuracion.update(config => ({ ...config, resenas: [...config.resenas, {
      id: null, nombre: '', calificacion: 5, comentario: '', mediaId: null, fotoUrl: null, destacada: false, activa: true, orden: config.resenas.length
    }] }));
    this.limpiarEstado();
  }

  agregarIndicador(): void {
    if (this.configuracion().indicadores.length >= this.limiteIndicadores) return;
    this.configuracion.update(config => ({ ...config, indicadores: [...config.indicadores, {
      id: null, valor: '', etiqueta: '', icono: 'ESTRELLA', activo: true, orden: config.indicadores.length
    }] }));
    this.limpiarEstado();
  }

  quitarResena(indice: number): void {
    this.configuracion.update(config => ({ ...config, resenas: this.reindexar(config.resenas.filter((_, i) => i !== indice)) }));
    this.limpiarEstado();
  }

  quitarIndicador(indice: number): void {
    this.configuracion.update(config => ({ ...config, indicadores: this.reindexar(config.indicadores.filter((_, i) => i !== indice)) }));
    this.limpiarEstado();
  }

  moverResena(indice: number, desplazamiento: -1 | 1): void {
    this.configuracion.update(config => ({ ...config, resenas: this.mover(config.resenas, indice, desplazamiento) }));
    this.limpiarEstado();
  }

  moverIndicador(indice: number, desplazamiento: -1 | 1): void {
    this.configuracion.update(config => ({ ...config, indicadores: this.mover(config.indicadores, indice, desplazamiento) }));
    this.limpiarEstado();
  }

  soltarResena(evento: CdkDragDrop<ResenaSitioAdmin[]>): void {
    if (evento.previousIndex === evento.currentIndex) return;
    this.configuracion.update(config => ({ ...config, resenas: this.soltar(config.resenas, evento) }));
    this.limpiarEstado();
  }

  soltarIndicador(evento: CdkDragDrop<IndicadorConfianzaSitioAdmin[]>): void {
    if (evento.previousIndex === evento.currentIndex) return;
    this.configuracion.update(config => ({ ...config, indicadores: this.soltar(config.indicadores, evento) }));
    this.limpiarEstado();
  }

  calificar(item: ResenaSitioAdmin, calificacion: number): void {
    item.calificacion = calificacion;
    this.limpiarEstado();
  }

  seleccionarFoto(evento: Event, item: ResenaSitioAdmin, indice: number): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo || this.subiendoFoto() !== null) return;
    this.subiendoFoto.set(indice);
    this.limpiarEstado();
    this.facade.subirFoto(archivo).pipe(finalize(() => this.subiendoFoto.set(null))).subscribe({
      next: media => {
        item.mediaId = media.id;
        item.fotoUrl = media.secureUrl;
        this.mensaje.set('Foto cargada. Guarda las reseñas para aplicar el cambio.');
      },
      error: err => this.error.set(err?.error?.message || err?.error?.detail || 'No fue posible cargar la foto.')
    });
  }

  quitarFoto(item: ResenaSitioAdmin): void {
    item.mediaId = null;
    item.fotoUrl = null;
    this.limpiarEstado();
  }

  iconoClase(icono: IconoIndicadorConfianza): string {
    return this.iconos.find(item => item.valor === icono)?.clase ?? 'bi bi-star';
  }

  guardar(): void {
    if (this.guardando()) return;
    const config = this.configuracion();
    if (config.resenas.some(item => !item.nombre.trim() || !item.comentario.trim())) {
      this.error.set('Cada reseña necesita el nombre del cliente y su comentario.');
      return;
    }
    if (config.indicadores.some(item => !item.valor.trim() || !item.etiqueta.trim())) {
      this.error.set('Cada indicador necesita un valor y una etiqueta.');
      return;
    }
    this.guardando.set(true);
    this.limpiarEstado();
    this.facade.guardar({
      googleMapsUrl: config.googleMapsUrl?.trim() || null,
      resenas: config.resenas.map((item, orden) => ({
        nombre: item.nombre.trim(), calificacion: item.calificacion, comentario: item.comentario.trim(),
        mediaId: item.mediaId, fotoUrl: item.fotoUrl,
        destacada: item.destacada, activa: item.activa, orden
      })),
      indicadores: config.indicadores.map((item, orden) => ({
        valor: item.valor.trim(), etiqueta: item.etiqueta.trim(), icono: item.icono, activo: item.activo, orden
      }))
    }).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: configuracion => {
        this.configuracion.set(configuracion);
        this.mensaje.set('Las reseñas y señales de confianza quedaron actualizadas.');
      },
      error: err => this.error.set(err?.error?.message || err?.error?.detail || 'No fue posible guardar las reseñas.')
    });
  }

  private mover<T extends { orden: number }>(items: T[], indice: number, desplazamiento: -1 | 1): T[] {
    const destino = indice + desplazamiento;
    if (destino < 0 || destino >= items.length) return items;
    const copia = [...items];
    moveItemInArray(copia, indice, destino);
    return this.reindexar(copia);
  }

  private soltar<T extends { orden: number }>(items: T[], evento: CdkDragDrop<T[]>): T[] {
    const copia = [...items];
    moveItemInArray(copia, evento.previousIndex, evento.currentIndex);
    return this.reindexar(copia);
  }

  private reindexar<T extends { orden: number }>(items: T[]): T[] {
    return items.map((item, orden) => ({ ...item, orden }));
  }

  private limpiarEstado(): void {
    this.mensaje.set(null);
    this.error.set(null);
  }
}
