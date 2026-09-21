import { CommonModule } from '@angular/common';
import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';
import { PreguntaFrecuenteSitioAdmin } from '../../../core/admin/admin.service';
import { SiteFaqFacade } from './site-faq.facade';

@Component({
  selector: 'app-site-faq', standalone: true,
  imports: [CommonModule, FormsModule, CdkDropList, CdkDrag, CdkDragHandle, MatButtonModule, MatFormFieldModule, MatInputModule, MatSlideToggleModule],
  providers: [SiteFaqFacade], templateUrl: './site-faq.component.html', styleUrl: './site-faq.component.css'
})
export class SiteFaqComponent implements OnInit {
  private readonly facade = inject(SiteFaqFacade);
  readonly limite = 20;
  readonly preguntas = signal<PreguntaFrecuenteSitioAdmin[]>([]);
  readonly cargando = signal(true); readonly guardando = signal(false);
  readonly mensaje = signal<string | null>(null); readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.facade.cargar().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: items => this.preguntas.set(items),
      error: () => this.error.set('No fue posible cargar las preguntas frecuentes.')
    });
  }
  agregar(): void {
    if (this.preguntas().length >= this.limite) return;
    this.preguntas.update(items => [...items, { id: null, pregunta: '', respuesta: '', activa: true, orden: items.length }]);
    this.limpiar();
  }
  quitar(indice: number): void { this.preguntas.update(items => this.reindexar(items.filter((_, i) => i !== indice))); this.limpiar(); }
  mover(indice: number, desplazamiento: -1 | 1): void {
    const destino = indice + desplazamiento;
    if (destino < 0 || destino >= this.preguntas().length) return;
    const copia = [...this.preguntas()]; moveItemInArray(copia, indice, destino); this.preguntas.set(this.reindexar(copia)); this.limpiar();
  }
  soltar(evento: CdkDragDrop<PreguntaFrecuenteSitioAdmin[]>): void {
    const copia = [...this.preguntas()]; moveItemInArray(copia, evento.previousIndex, evento.currentIndex); this.preguntas.set(this.reindexar(copia)); this.limpiar();
  }
  guardar(): void {
    if (this.preguntas().some(item => !item.pregunta.trim() || !item.respuesta.trim())) { this.error.set('Cada elemento necesita una pregunta y una respuesta.'); return; }
    this.guardando.set(true); this.limpiar();
    this.facade.guardar({ preguntas: this.preguntas().map((item, orden) => ({ pregunta: item.pregunta.trim(), respuesta: item.respuesta.trim(), activa: item.activa, orden })) })
      .pipe(finalize(() => this.guardando.set(false))).subscribe({
        next: items => { this.preguntas.set(items); this.mensaje.set('Las preguntas frecuentes quedaron actualizadas.'); },
        error: err => this.error.set(err?.error?.message || err?.error?.detail || 'No fue posible guardar las preguntas frecuentes.')
      });
  }
  private reindexar(items: PreguntaFrecuenteSitioAdmin[]) { return items.map((item, orden) => ({ ...item, orden })); }
  private limpiar() { this.mensaje.set(null); this.error.set(null); }
}
