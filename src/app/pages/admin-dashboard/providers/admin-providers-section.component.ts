
import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ConfiguracionMediaAdmin, EncuadreFotoColaborador, GuardarPrestadorPayload, PrestadorAdmin, ServicioAdmin, SucursalAdmin } from '../../../core/admin/admin.service';
import { AdminProvidersFacade } from './admin-providers.facade';
import { construirUrlRetrato } from './admin-providers.forms';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-admin-providers-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule
],
  templateUrl: './admin-providers-section.component.html',
  styleUrls: ['./admin-providers-section.component.css']
})
export class AdminProvidersSectionComponent implements OnInit {
  private readonly facade = inject(AdminProvidersFacade);
  @Input({ required: true }) prestadorEditandoId!: number | null;
  @Input({ required: true }) formularioPrestador!: GuardarPrestadorPayload;
  @Input({ required: true }) sucursales!: SucursalAdmin[];
  @Input({ required: true }) guardandoPrestador!: boolean;
  @Input({ required: true }) serviciosPrestadorDisponibles!: ServicioAdmin[];
  @Input({ required: true }) prestadores!: PrestadorAdmin[];

  @Output() clearEdit = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();
  @Output() branchChange = new EventEmitter<number>();
  @Output() serviceToggle = new EventEmitter<{ servicioId: number; checked: boolean }>();
  @Output() editProvider = new EventEmitter<PrestadorAdmin>();

  readonly configuracionMedia = signal<ConfiguracionMediaAdmin | null>(null);
  readonly subiendoFoto = signal(false);
  readonly mensajeFoto = signal<string | null>(null);
  readonly errorFoto = signal<string | null>(null);
  readonly encuadres: ReadonlyArray<{ valor: EncuadreFotoColaborador; etiqueta: string; icono: string }> = [
    { valor: 'ROSTRO', etiqueta: 'Rostro', icono: 'bi-person-bounding-box' },
    { valor: 'AUTOMATICO', etiqueta: 'Inteligente', icono: 'bi-stars' },
    { valor: 'CENTRO', etiqueta: 'Centro', icono: 'bi-bullseye' },
    { valor: 'SUPERIOR', etiqueta: 'Superior', icono: 'bi-arrow-up' }
  ];

  ngOnInit(): void {
    this.facade.cargarConfiguracionMedia().subscribe({
      next: configuracion => this.configuracionMedia.set(configuracion),
      error: () => this.configuracionMedia.set(null)
    });
  }

  seleccionarFoto(evento: Event): void {
    const inputArchivo = evento.target as HTMLInputElement;
    const archivo = inputArchivo.files?.[0];
    inputArchivo.value = '';
    if (!archivo || this.subiendoFoto()) return;

    const configuracion = this.configuracionMedia();
    this.mensajeFoto.set(null);
    this.errorFoto.set(null);
    if (!configuracion?.cargaHabilitada) {
      this.errorFoto.set('Cloudinary todavía no está configurado. Puedes continuar usando una URL pública.');
      return;
    }
    if (!configuracion.tiposImagenPermitidos.includes(archivo.type)) {
      this.errorFoto.set('Usa una imagen JPG, PNG, WebP o AVIF.');
      return;
    }
    if (archivo.size > configuracion.maxBytesImagen) {
      this.errorFoto.set(`La imagen supera el máximo de ${this.megabytes(configuracion.maxBytesImagen)} MB.`);
      return;
    }

    this.subiendoFoto.set(true);
    this.facade.subirFoto(archivo).pipe(finalize(() => this.subiendoFoto.set(false))).subscribe({
      next: media => {
        this.formularioPrestador.fotoUrl = media.secureUrl;
        this.mensajeFoto.set('Foto cargada. Guarda el perfil para publicarla.');
      },
      error: err => this.errorFoto.set(err?.error?.mensaje || err?.error?.detail || 'No fue posible cargar la foto.')
    });
  }

  megabytes(bytes: number): string {
    return (bytes / 1024 / 1024).toFixed(bytes % (1024 * 1024) === 0 ? 0 : 1);
  }

  vistaPreviaFoto(): string | null {
    return construirUrlRetrato(
      this.formularioPrestador.fotoUrl,
      this.formularioPrestador.fotoEncuadre,
      this.formularioPrestador.fotoZoom
    );
  }

  vistaPreviaPrestador(prestador: PrestadorAdmin): string | null {
    return construirUrlRetrato(prestador.fotoUrl, prestador.fotoEncuadre, prestador.fotoZoom, 160);
  }
}
