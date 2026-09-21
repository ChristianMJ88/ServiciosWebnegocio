import { CommonModule } from '@angular/common';
import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDragPlaceholder, CdkDragPreview, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';
import { ConfiguracionMediaAdmin, GaleriaImagenSitioAdmin, GaleriaSitioAdmin, ServicioAdmin, SucursalAdmin } from '../../../core/admin/admin.service';
import { SiteGalleryFacade } from './site-gallery.facade';

@Component({
  selector: 'app-site-gallery',
  standalone: true,
  imports: [CommonModule, FormsModule, CdkDropList, CdkDrag, CdkDragHandle, CdkDragPreview, CdkDragPlaceholder, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule],
  providers: [SiteGalleryFacade],
  templateUrl: './site-gallery.component.html',
  styleUrl: './site-gallery.component.css'
})
export class SiteGalleryComponent implements OnInit {
  readonly limiteImagenes = 60;
  private readonly facade = inject(SiteGalleryFacade);
  readonly galeria = signal<GaleriaSitioAdmin>({
    titulo: 'Nuestro trabajo', descripcion: null, vista: 'CUADRICULA', ampliacionHabilitada: true, imagenes: []
  });
  readonly servicios = signal<ServicioAdmin[]>([]);
  readonly sucursales = signal<SucursalAdmin[]>([]);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly subiendoIndice = signal<number | null>(null);
  readonly configuracionMedia = signal<ConfiguracionMediaAdmin | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.facade.cargar().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: respuesta => {
        this.galeria.set(respuesta.galeria);
        this.servicios.set(respuesta.servicios.filter(item => item.activo && item.visiblePublico));
        this.sucursales.set(respuesta.sucursales.filter(item => item.activa));
        this.configuracionMedia.set(respuesta.media);
      },
      error: () => this.error.set('No fue posible cargar la galería.')
    });
  }

  agregar(): void {
    if (this.galeria().imagenes.length >= this.limiteImagenes) {
      this.error.set(`La galería admite hasta ${this.limiteImagenes} imágenes.`);
      return;
    }
    const imagen: GaleriaImagenSitioAdmin = {
      id: null, mediaId: null, imagenUrl: '', textoAlternativo: '', titulo: null, descripcion: null,
      servicioId: null, servicioNombre: null, sucursalId: null, sucursalNombre: null,
      activa: true, orden: this.galeria().imagenes.length
    };
    this.galeria.update(galeria => ({ ...galeria, imagenes: [...galeria.imagenes, imagen] }));
    this.limpiarEstado();
  }

  seleccionarArchivo(evento: Event, indice: number): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo || this.subiendoIndice() !== null) return;
    const configuracion = this.configuracionMedia();
    if (!configuracion?.cargaHabilitada) {
      this.error.set('La carga de imágenes todavía no está habilitada en la plataforma.');
      return;
    }
    if (archivo.size > configuracion.maxBytesImagen) {
      this.error.set(`La imagen supera el máximo de ${this.megabytes(configuracion.maxBytesImagen)} MB.`);
      return;
    }
    this.subiendoIndice.set(indice);
    this.limpiarEstado();
    this.facade.subirImagen(archivo).pipe(finalize(() => this.subiendoIndice.set(null))).subscribe({
      next: media => {
        this.galeria.update(galeria => ({
          ...galeria,
          imagenes: galeria.imagenes.map((item, i) => i === indice ? {
            ...item,
            mediaId: media.id,
            imagenUrl: media.secureUrl,
            textoAlternativo: item.textoAlternativo || this.nombreSinExtension(archivo.name)
          } : item)
        }));
        this.mensaje.set('Imagen cargada. Guarda la galería para publicarla.');
      },
      error: err => this.error.set(err?.error?.message || err?.error?.detail || 'No fue posible cargar la imagen.')
    });
  }

  actualizarUrlManual(item: GaleriaImagenSitioAdmin, valor: string): void {
    item.imagenUrl = valor;
    item.mediaId = null;
    this.limpiarEstado();
  }

  megabytes(bytes: number): string {
    return (bytes / 1024 / 1024).toFixed(bytes % (1024 * 1024) === 0 ? 0 : 1);
  }

  private nombreSinExtension(nombre: string): string {
    return nombre.replace(/\.[^.]+$/, '').replace(/[-_]+/g, ' ').trim();
  }

  quitar(indice: number): void {
    this.galeria.update(galeria => ({
      ...galeria,
      imagenes: galeria.imagenes.filter((_, i) => i !== indice).map((item, orden) => ({ ...item, orden }))
    }));
    this.limpiarEstado();
  }

  mover(indice: number, desplazamiento: -1 | 1): void {
    const imagenes = [...this.galeria().imagenes];
    const destino = indice + desplazamiento;
    if (destino < 0 || destino >= imagenes.length) return;
    [imagenes[indice], imagenes[destino]] = [imagenes[destino], imagenes[indice]];
    this.galeria.update(galeria => ({ ...galeria, imagenes: imagenes.map((item, orden) => ({ ...item, orden })) }));
    this.limpiarEstado();
  }

  soltar(evento: CdkDragDrop<GaleriaImagenSitioAdmin[]>): void {
    if (evento.previousIndex === evento.currentIndex) return;
    const imagenes = [...this.galeria().imagenes];
    moveItemInArray(imagenes, evento.previousIndex, evento.currentIndex);
    this.galeria.update(galeria => ({
      ...galeria,
      imagenes: imagenes.map((item, orden) => ({ ...item, orden }))
    }));
    this.limpiarEstado();
  }

  guardar(): void {
    const galeria = this.galeria();
    if (this.guardando()) return;
    if (!galeria.titulo.trim()) {
      this.error.set('Escribe un título para la galería.');
      return;
    }
    if (galeria.imagenes.some(item => !item.imagenUrl.trim() || !item.textoAlternativo.trim())) {
      this.error.set('Cada imagen necesita un archivo o URL y un texto alternativo.');
      return;
    }
    this.guardando.set(true);
    this.limpiarEstado();
    this.facade.guardar({
      titulo: galeria.titulo.trim(), descripcion: galeria.descripcion?.trim() || null,
      vista: galeria.vista, ampliacionHabilitada: galeria.ampliacionHabilitada,
      imagenes: galeria.imagenes.map((item, orden) => ({
        mediaId: item.mediaId, imagenUrl: item.imagenUrl.trim(), textoAlternativo: item.textoAlternativo.trim(),
        titulo: item.titulo?.trim() || null, descripcion: item.descripcion?.trim() || null,
        servicioId: item.servicioId || null, sucursalId: item.sucursalId || null, activa: item.activa, orden
      }))
    }).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: respuesta => {
        this.galeria.set(respuesta);
        this.mensaje.set('La galería quedó actualizada.');
      },
      error: err => this.error.set(err?.error?.message || err?.error?.detail || 'No fue posible guardar la galería.')
    });
  }

  private limpiarEstado(): void {
    this.mensaje.set(null);
    this.error.set(null);
  }
}
