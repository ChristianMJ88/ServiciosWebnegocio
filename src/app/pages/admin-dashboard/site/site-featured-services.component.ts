import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';
import {
  GrupoServicioAdmin,
  ServicioAdmin,
  ServicioDestacadoSeleccionAdmin,
  ServiciosDestacadosAdmin,
  SubgrupoServicioAdmin
} from '../../../core/admin/admin.service';
import { SiteFeaturedServicesFacade } from './site-featured-services.facade';

interface CatalogoDestacadoOpcion {
  tipo: 'GRUPO' | 'SUBGRUPO' | 'SERVICIO';
  referenciaId: number;
  nombre: string;
  ruta: string;
  disponible: boolean;
}

@Component({
  selector: 'app-site-featured-services',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule],
  providers: [SiteFeaturedServicesFacade],
  templateUrl: './site-featured-services.component.html',
  styleUrl: './site-featured-services.component.css'
})
export class SiteFeaturedServicesComponent implements OnInit {
  private readonly facade = inject(SiteFeaturedServicesFacade);

  readonly configuracion = signal<ServiciosDestacadosAdmin | null>(null);
  readonly grupos = signal<GrupoServicioAdmin[]>([]);
  readonly subgrupos = signal<SubgrupoServicioAdmin[]>([]);
  readonly servicios = signal<ServicioAdmin[]>([]);
  readonly busqueda = signal('');
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  readonly opciones = computed<CatalogoDestacadoOpcion[]>(() => {
    const grupoPorId = new Map(this.grupos().map(grupo => [grupo.id, grupo]));
    const subgrupoPorId = new Map(this.subgrupos().map(subgrupo => [subgrupo.id, subgrupo]));
    return [
      ...this.grupos().map(grupo => ({
        tipo: 'GRUPO' as const, referenciaId: grupo.id, nombre: grupo.nombre,
        ruta: grupo.nombre, disponible: grupo.activo
      })),
      ...this.subgrupos().map(subgrupo => ({
        tipo: 'SUBGRUPO' as const, referenciaId: subgrupo.id, nombre: subgrupo.nombre,
        ruta: `${subgrupo.grupoNombre} / ${subgrupo.nombre}`,
        disponible: subgrupo.activo && (grupoPorId.get(subgrupo.grupoId)?.activo ?? false)
      })),
      ...this.servicios().map(servicio => {
        const grupo = servicio.grupoId ? grupoPorId.get(servicio.grupoId) : null;
        const subgrupo = servicio.subgrupoId ? subgrupoPorId.get(servicio.subgrupoId) : null;
        return {
          tipo: 'SERVICIO' as const, referenciaId: servicio.id, nombre: servicio.nombre,
          ruta: [grupo?.nombre, subgrupo?.nombre, servicio.nombre].filter(Boolean).join(' / '),
          disponible: servicio.activo && servicio.visiblePublico
        };
      })
    ].filter(opcion => opcion.disponible);
  });

  readonly opcionesFiltradas = computed(() => {
    const termino = this.normalizar(this.busqueda());
    const seleccionados = new Set((this.configuracion()?.elementos ?? []).map(item => `${item.tipo}:${item.referenciaId}`));
    return this.opciones()
      .filter(opcion => !seleccionados.has(`${opcion.tipo}:${opcion.referenciaId}`))
      .filter(opcion => !termino || this.normalizar(`${opcion.nombre} ${opcion.ruta} ${this.etiquetaTipo(opcion.tipo)}`).includes(termino))
      .slice(0, 40);
  });

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.facade.cargar().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: respuesta => {
        this.configuracion.set({ ...respuesta.configuracion, elementos: [...respuesta.configuracion.elementos].sort((a, b) => a.orden - b.orden) });
        this.grupos.set(respuesta.grupos);
        this.subgrupos.set(respuesta.subgrupos);
        this.servicios.set(respuesta.servicios);
      },
      error: () => this.error.set('No fue posible cargar los servicios destacados.')
    });
  }

  agregar(opcion: CatalogoDestacadoOpcion): void {
    this.configuracion.update(config => config ? ({ ...config, elementos: [...config.elementos, {
      tipo: opcion.tipo, referenciaId: opcion.referenciaId, nombre: opcion.nombre,
      rutaCatalogo: opcion.ruta, etiqueta: null, orden: config.elementos.length, disponible: true
    }], seleccionAutomatica: false }) : config);
    this.limpiarEstado();
  }

  quitar(indice: number): void {
    this.actualizarElementos(elementos => elementos.filter((_, posicion) => posicion !== indice));
  }

  mover(indice: number, desplazamiento: -1 | 1): void {
    const destino = indice + desplazamiento;
    const elementos = [...(this.configuracion()?.elementos ?? [])];
    if (destino < 0 || destino >= elementos.length) return;
    [elementos[indice], elementos[destino]] = [elementos[destino], elementos[indice]];
    this.establecerElementos(elementos);
  }

  cambiarEtiqueta(indice: number, etiqueta: string): void {
    this.actualizarElementos(elementos => elementos.map((item, posicion) => posicion === indice ? { ...item, etiqueta } : item));
  }

  actualizar<K extends keyof ServiciosDestacadosAdmin>(campo: K, valor: ServiciosDestacadosAdmin[K]): void {
    this.configuracion.update(config => config ? { ...config, [campo]: valor } : config);
    this.limpiarEstado();
  }

  guardar(): void {
    const config = this.configuracion();
    if (!config || this.guardando()) return;
    this.guardando.set(true);
    this.limpiarEstado();
    this.facade.guardar({
      vista: config.vista,
      mostrarPrecio: config.mostrarPrecio,
      mostrarDuracion: config.mostrarDuracion,
      mostrarDescripcion: config.mostrarDescripcion,
      limite: config.limite,
      elementos: config.elementos.map((item, orden) => ({
        tipo: item.tipo,
        referenciaId: item.referenciaId,
        etiqueta: item.etiqueta?.trim() || null,
        orden
      }))
    }).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: guardada => {
        this.configuracion.set(guardada);
        this.mensaje.set(guardada.seleccionAutomatica
          ? 'Se activó la selección automática con los primeros grupos disponibles.'
          : 'Los servicios destacados quedaron actualizados.');
      },
      error: () => this.error.set('No fue posible guardar la selección. Revisa los datos e inténtalo nuevamente.')
    });
  }

  etiquetaTipo(tipo: string): string {
    return ({ GRUPO: 'Grupo', SUBGRUPO: 'Subgrupo', SERVICIO: 'Servicio' } as Record<string, string>)[tipo] ?? tipo;
  }

  private actualizarElementos(transformar: (items: ServicioDestacadoSeleccionAdmin[]) => ServicioDestacadoSeleccionAdmin[]): void {
    this.establecerElementos(transformar([...(this.configuracion()?.elementos ?? [])]));
  }

  private establecerElementos(elementos: ServicioDestacadoSeleccionAdmin[]): void {
    this.configuracion.update(config => config ? ({
      ...config,
      elementos: elementos.map((item, orden) => ({ ...item, orden })),
      seleccionAutomatica: elementos.length === 0
    }) : config);
    this.limpiarEstado();
  }

  private limpiarEstado(): void { this.mensaje.set(null); this.error.set(null); }

  private normalizar(valor: string): string {
    return valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
  }
}
