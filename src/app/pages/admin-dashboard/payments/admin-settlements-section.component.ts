import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import {
  ConciliacionPago,
  ConfiguracionReporteLiberacion,
  GuardarConfiguracionReporteLiberacion,
  PaymentsAdminService,
  ReporteLiberacion
} from './payments-admin.service';

@Component({
  selector: 'app-admin-settlements-section',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-settlements-section.component.html',
  styleUrl: './admin-settlements-section.component.css'
})
export class AdminSettlementsSectionComponent implements OnInit {
  private readonly servicio = inject(PaymentsAdminService);

  readonly cargando = signal(true);
  readonly procesando = signal(false);
  readonly error = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly conciliaciones = signal<ConciliacionPago[]>([]);
  readonly reportes = signal<ReporteLiberacion[]>([]);
  readonly configuracion = signal<ConfiguracionReporteLiberacion | null>(null);
  readonly configuracionDisponible = signal(true);

  readonly monedaResumen = computed(() => {
    const monedas = new Set(this.conciliaciones().map(item => item.moneda));
    return monedas.size === 1 ? [...monedas][0] : null;
  });
  readonly bruto = computed(() => this.monedaResumen() == null ? null
    : this.conciliaciones().reduce((total, item) => total + item.importeBruto, 0));
  readonly neto = computed(() => this.monedaResumen() == null ? null
    : this.conciliaciones().filter(item => item.liquidacionDefinitiva)
      .reduce((total, item) => total + (item.importeNeto ?? 0), 0));
  readonly pendientes = computed(() => this.conciliaciones()
    .filter(item => !item.liquidacionDefinitiva && item.estado !== 'REEMBOLSADA').length);

  fechaDesde = this.fechaLocal(-7);
  fechaHasta = this.fechaLocal(0);
  formulario: GuardarConfiguracionReporteLiberacion = {
    prefijo: '', zonaHoraria: this.zonaHorariaGmt(), frecuenciaTipo: 'daily', frecuenciaHora: 2,
    frecuenciaValor: '0', incluirRetirosAlFinal: true, ejecutarDespuesDeRetiro: false
  };

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    forkJoin({
      conciliaciones: this.servicio.conciliaciones(),
      reportes: this.servicio.reportesLiberacion(),
      configuracion: this.servicio.configuracionReporteLiberacion().pipe(catchError(() => {
        this.configuracionDisponible.set(false);
        return of(null);
      }))
    }).pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: ({ conciliaciones, reportes, configuracion }) => {
        this.conciliaciones.set(conciliaciones);
        this.reportes.set(reportes);
        this.configuracion.set(configuracion);
        if (configuracion) {
          this.configuracionDisponible.set(true);
          this.formulario = {
            prefijo: configuracion.prefijo ?? '',
            zonaHoraria: configuracion.zonaHoraria ?? this.zonaHorariaGmt(),
            frecuenciaTipo: this.tipoFrecuencia(configuracion.frecuenciaTipo),
            frecuenciaHora: configuracion.frecuenciaHora ?? 2,
            frecuenciaValor: configuracion.frecuenciaValor ?? '0',
            incluirRetirosAlFinal: configuracion.incluirRetirosAlFinal,
            ejecutarDespuesDeRetiro: configuracion.ejecutarDespuesDeRetiro
          };
        }
      },
      error: err => this.error.set(this.detalleError(err, 'No fue posible cargar las liquidaciones.'))
    });
  }

  alCambiarFrecuencia(): void {
    if (this.formulario.frecuenciaTipo === 'daily') this.formulario.frecuenciaValor = '0';
    if (this.formulario.frecuenciaTipo === 'weekly' && !this.diasSemana.includes(this.formulario.frecuenciaValor)) {
      this.formulario.frecuenciaValor = 'monday';
    }
    if (this.formulario.frecuenciaTipo === 'monthly' && !/^([1-9]|[12]\d|3[01])$/.test(this.formulario.frecuenciaValor)) {
      this.formulario.frecuenciaValor = '1';
    }
  }

  guardarConfiguracion(): void {
    if (!this.formulario.prefijo.trim() || !this.formulario.zonaHoraria.trim()) {
      this.error.set('Completa el prefijo y la zona horaria del reporte.');
      return;
    }
    this.ejecutar(
      this.servicio.guardarConfiguracionReporte(this.formulario, !this.configuracionDisponible()),
      configuracion => {
        this.configuracion.set(configuracion);
        this.configuracionDisponible.set(true);
        this.mensaje.set('La configuración de liquidaciones quedó guardada.');
      },
      'No fue posible guardar la configuración del reporte.'
    );
  }

  cambiarProgramacion(): void {
    const activar = !this.configuracion()?.programado;
    this.ejecutar(this.servicio.cambiarProgramacionReporte(activar), configuracion => {
      this.configuracion.set(configuracion);
      this.mensaje.set(activar ? 'La generación automática quedó activa.' : 'La generación automática quedó desactivada.');
    }, 'No fue posible cambiar la programación.');
  }

  generarReporte(): void {
    if (!this.fechaDesde || !this.fechaHasta || this.fechaDesde > this.fechaHasta) {
      this.error.set('Selecciona un rango de fechas válido.');
      return;
    }
    this.ejecutar(this.servicio.solicitarReporteLiberacion(
      this.inicioDia(this.fechaDesde), this.inicioDiaSiguiente(this.fechaHasta)
    ), reporte => {
      this.reportes.update(items => [reporte, ...items]);
      this.mensaje.set('Mercado Pago comenzó a generar el reporte.');
    }, 'No fue posible solicitar el reporte.');
  }

  sincronizar(reporte: ReporteLiberacion): void {
    this.ejecutar(this.servicio.sincronizarReporteLiberacion(reporte.id), actualizado => {
      this.reportes.update(items => items.map(item => item.id === actualizado.id ? actualizado : item));
      this.servicio.conciliaciones().subscribe(items => this.conciliaciones.set(items));
      this.mensaje.set(actualizado.estado === 'PROCESADO'
        ? `Reporte conciliado: ${actualizado.filasProcesadas} movimientos actualizados.`
        : 'El reporte todavía se está generando.');
    }, 'No fue posible sincronizar el reporte.');
  }

  estadoLegible(estado: string): string {
    return ({ SOLICITADO: 'Solicitado', GENERANDO: 'Generando', LISTO: 'Listo', PROCESADO: 'Procesado', ERROR: 'Con error' } as Record<string, string>)[estado] ?? estado;
  }

  readonly diasSemana = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];
  readonly etiquetasDia: Record<string, string> = {
    monday: 'Lunes', tuesday: 'Martes', wednesday: 'Miércoles', thursday: 'Jueves',
    friday: 'Viernes', saturday: 'Sábado', sunday: 'Domingo'
  };

  private ejecutar<T>(peticion: import('rxjs').Observable<T>, exito: (valor: T) => void, mensajeError: string): void {
    if (this.procesando()) return;
    this.procesando.set(true); this.error.set(null); this.mensaje.set(null);
    peticion.pipe(finalize(() => this.procesando.set(false))).subscribe({
      next: exito,
      error: err => this.error.set(this.detalleError(err, mensajeError))
    });
  }

  private tipoFrecuencia(valor: string | null): 'daily' | 'weekly' | 'monthly' {
    return valor === 'weekly' || valor === 'monthly' ? valor : 'daily';
  }
  private fechaLocal(desplazamiento: number): string {
    const fecha = new Date(); fecha.setDate(fecha.getDate() + desplazamiento);
    return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
  }
  private inicioDia(valor: string): string { return new Date(`${valor}T00:00:00`).toISOString(); }
  private inicioDiaSiguiente(valor: string): string {
    const fecha = new Date(`${valor}T00:00:00`); fecha.setDate(fecha.getDate() + 1); return fecha.toISOString();
  }
  private zonaHorariaGmt(): string {
    const minutos = -new Date().getTimezoneOffset();
    const signo = minutos >= 0 ? '+' : '-';
    return `GMT${signo}${String(Math.floor(Math.abs(minutos) / 60)).padStart(2, '0')}`;
  }
  private detalleError(error: any, respaldo: string): string {
    return error?.error?.message || error?.error?.detail || error?.message || respaldo;
  }
}
