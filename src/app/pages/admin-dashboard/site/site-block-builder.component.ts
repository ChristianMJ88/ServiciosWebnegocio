import { CommonModule } from '@angular/common';
import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { BloqueSitioAdmin } from '../../../core/admin/admin.service';
import { SiteBlockBuilderFacade } from './site-block-builder.facade';

@Component({
  selector: 'app-site-block-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, CdkDropList, CdkDrag, CdkDragHandle, MatButtonModule, MatSlideToggleModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  providers: [SiteBlockBuilderFacade],
  templateUrl: './site-block-builder.component.html',
  styleUrl: './site-block-builder.component.css'
})
export class SiteBlockBuilderComponent implements OnInit {
  private readonly facade = inject(SiteBlockBuilderFacade);

  readonly bloques = signal<BloqueSitioAdmin[]>([]);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly personalizando = signal<string | null>(null);
  readonly totalConfigurables = computed(() => this.bloques().filter(bloque => bloque.disponible).length);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.facade.cargar().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: bloques => this.bloques.set(this.normalizarOrden(bloques)),
      error: () => this.error.set('No fue posible cargar la estructura del sitio.')
    });
  }

  soltar(evento: CdkDragDrop<BloqueSitioAdmin[]>): void {
    if (evento.previousIndex === evento.currentIndex) return;
    const bloques = [...this.bloques()];
    moveItemInArray(bloques, evento.previousIndex, evento.currentIndex);
    this.bloques.set(this.reindexar(bloques));
    this.marcarCambios();
  }

  mover(indice: number, desplazamiento: -1 | 1): void {
    if (!this.bloques()[indice]?.disponible) return;
    const destino = indice + desplazamiento;
    if (destino < 0 || destino >= this.totalConfigurables()) return;
    const bloques = [...this.bloques()];
    moveItemInArray(bloques, indice, destino);
    this.bloques.set(this.reindexar(bloques));
    this.marcarCambios();
  }

  cambiarVisibilidad(tipo: string, habilitado: boolean): void {
    this.bloques.update(bloques => bloques.map(bloque =>
      bloque.tipo === tipo && bloque.disponible ? { ...bloque, habilitado } : bloque
    ));
    this.marcarCambios();
  }

  alternarPersonalizacion(tipo: string): void {
    this.personalizando.update(actual => actual === tipo ? null : tipo);
  }

  actualizarCampo<K extends keyof BloqueSitioAdmin>(tipo: string, campo: K, valor: BloqueSitioAdmin[K]): void {
    this.bloques.update(bloques => bloques.map(bloque =>
      bloque.tipo === tipo ? { ...bloque, [campo]: valor } : bloque
    ));
    this.marcarCambios();
  }

  guardar(): void {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.error.set(null);
    this.mensaje.set(null);
    this.facade.guardar(this.bloques()).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: bloques => {
        this.bloques.set(this.normalizarOrden(bloques));
        this.mensaje.set('La estructura del sitio quedó actualizada.');
      },
      error: () => this.error.set('No fue posible guardar la estructura. Inténtalo nuevamente.')
    });
  }

  private marcarCambios(): void {
    this.mensaje.set(null);
    this.error.set(null);
  }

  private normalizarOrden(bloques: BloqueSitioAdmin[]): BloqueSitioAdmin[] {
    const ordenados = [...bloques].sort((a, b) => a.orden - b.orden);
    return [
      ...ordenados.filter(bloque => bloque.disponible),
      ...ordenados.filter(bloque => !bloque.disponible)
    ].map((bloque, orden) => ({ ...bloque, orden }));
  }

  private reindexar(bloques: BloqueSitioAdmin[]): BloqueSitioAdmin[] {
    return bloques.map((bloque, orden) => ({ ...bloque, orden }));
  }
}
