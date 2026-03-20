import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import {
  AdminService,
  ExcepcionDisponibilidadAdmin,
  GuardarExcepcionDisponibilidadPayload,
  GuardarPrestadorPayload,
  GuardarReglaDisponibilidadPayload,
  GuardarServicioPayload,
  GuardarSucursalPayload,
  PrestadorAdmin,
  ReporteServicioAdmin,
  ReglaDisponibilidadAdmin,
  ResumenAdmin,
  ServicioAdmin,
  SucursalAdmin
} from '../../core/admin/admin.service';
import { CitaCliente } from '../../core/auth/client-appointments.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, DatePipe, CurrencyPipe, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  readonly loading = signal(false);
  readonly resumen = signal<ResumenAdmin | null>(null);
  readonly citas = signal<CitaCliente[]>([]);
  readonly sucursales = signal<SucursalAdmin[]>([]);
  readonly servicios = signal<ServicioAdmin[]>([]);
  readonly prestadores = signal<PrestadorAdmin[]>([]);
  readonly reglasDisponibilidad = signal<ReglaDisponibilidadAdmin[]>([]);
  readonly excepcionesDisponibilidad = signal<ExcepcionDisponibilidadAdmin[]>([]);
  readonly reporteServicios = signal<ReporteServicioAdmin[]>([]);
  readonly diasSemana = [
    { value: 1, label: 'Lunes' },
    { value: 2, label: 'Martes' },
    { value: 3, label: 'Miércoles' },
    { value: 4, label: 'Jueves' },
    { value: 5, label: 'Viernes' },
    { value: 6, label: 'Sábado' },
    { value: 7, label: 'Domingo' }
  ];
  readonly tiposBloqueo = ['BLOQUEO', 'DESCANSO', 'VACACIONES', 'HORARIO_ESPECIAL'];
  error = '';
  guardandoSucursal = false;
  guardandoServicio = false;
  guardandoPrestador = false;
  guardandoRegla = false;
  guardandoExcepcion = false;
  sucursalEditandoId: number | null = null;
  servicioEditandoId: number | null = null;
  prestadorEditandoId: number | null = null;
  reglaEditandoId: number | null = null;
  excepcionEditandoId: number | null = null;

  formularioSucursal: GuardarSucursalPayload = {
    nombre: '',
    direccion: '',
    telefono: '',
    zonaHoraria: 'America/Mexico_City',
    activa: true
  };

  formularioServicio: GuardarServicioPayload = {
    sucursalId: 0,
    nombre: '',
    descripcion: '',
    duracionMinutos: 60,
    bufferAntesMinutos: 0,
    bufferDespuesMinutos: 0,
    precio: 0,
    moneda: 'MXN',
    activo: true
  };

  formularioPrestador: GuardarPrestadorPayload = {
    sucursalId: 0,
    correo: '',
    contrasenaTemporal: '',
    nombreMostrar: '',
    biografia: '',
    colorAgenda: '#2563eb',
    activo: true,
    servicioIds: []
  };

  formularioRegla: GuardarReglaDisponibilidadPayload = {
    tipoSujeto: 'SUCURSAL',
    sujetoId: 0,
    diaSemana: 1,
    horaInicio: '09:00',
    horaFin: '18:00',
    intervaloMinutos: 15,
    vigenteDesde: null,
    vigenteHasta: null
  };

  formularioExcepcion: GuardarExcepcionDisponibilidadPayload = {
    tipoSujeto: 'SUCURSAL',
    sujetoId: 0,
    fechaExcepcion: '',
    horaInicio: null,
    horaFin: null,
    tipoBloqueo: 'BLOQUEO',
    motivo: null
  };

  ngOnInit(): void {
    this.recargar();
  }

  recargar() {
    this.loading.set(true);
    this.error = '';
    forkJoin({
      resumen: this.adminService.getResumen(),
      citas: this.adminService.getCitas(),
      sucursales: this.adminService.getSucursales(),
      servicios: this.adminService.getServicios(),
      prestadores: this.adminService.getPrestadores(),
      reglas: this.adminService.getReglasDisponibilidad(),
      excepciones: this.adminService.getExcepcionesDisponibilidad(),
      reporteServicios: this.adminService.getReporteServicios()
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ resumen, citas, sucursales, servicios, prestadores, reglas, excepciones, reporteServicios }) => {
          this.resumen.set(resumen);
          this.citas.set(citas);
          this.sucursales.set(sucursales);
          this.servicios.set(servicios);
          this.prestadores.set(prestadores);
          this.reglasDisponibilidad.set(reglas);
          this.excepcionesDisponibilidad.set(excepciones);
          this.reporteServicios.set(reporteServicios);
          if (!this.formularioServicio.sucursalId && sucursales.length > 0) {
            this.formularioServicio.sucursalId = sucursales[0].id;
          }
          if (!this.formularioPrestador.sucursalId && sucursales.length > 0) {
            this.formularioPrestador.sucursalId = sucursales[0].id;
          }
          if (!this.formularioRegla.sujetoId) {
            this.formularioRegla.sujetoId = this.opcionesSujetosDisponibilidad('SUCURSAL')[0]?.id ?? 0;
          }
          if (!this.formularioExcepcion.sujetoId) {
            this.formularioExcepcion.sujetoId = this.opcionesSujetosDisponibilidad('SUCURSAL')[0]?.id ?? 0;
          }
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo cargar el panel administrativo.';
        }
      });
  }

  editarSucursal(sucursal: SucursalAdmin) {
    this.sucursalEditandoId = sucursal.id;
    this.formularioSucursal = {
      nombre: sucursal.nombre,
      direccion: sucursal.direccion ?? '',
      telefono: sucursal.telefono ?? '',
      zonaHoraria: sucursal.zonaHoraria,
      activa: sucursal.activa
    };
  }

  cancelarEdicionSucursal() {
    this.sucursalEditandoId = null;
    this.formularioSucursal = {
      nombre: '',
      direccion: '',
      telefono: '',
      zonaHoraria: 'America/Mexico_City',
      activa: true
    };
  }

  guardarSucursal() {
    this.guardandoSucursal = true;
    this.error = '';
    const payload: GuardarSucursalPayload = {
      ...this.formularioSucursal,
      direccion: this.normalizarTexto(this.formularioSucursal.direccion),
      telefono: this.normalizarTexto(this.formularioSucursal.telefono)
    };

    const operacion = this.sucursalEditandoId
      ? this.adminService.actualizarSucursal(this.sucursalEditandoId, payload)
      : this.adminService.crearSucursal(payload);

    operacion
      .pipe(finalize(() => this.guardandoSucursal = false))
      .subscribe({
        next: () => {
          this.cancelarEdicionSucursal();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar la sucursal.';
        }
      });
  }

  editarServicio(servicio: ServicioAdmin) {
    this.servicioEditandoId = servicio.id;
    this.formularioServicio = {
      sucursalId: servicio.sucursalId,
      nombre: servicio.nombre,
      descripcion: servicio.descripcion ?? '',
      duracionMinutos: servicio.duracionMinutos,
      bufferAntesMinutos: servicio.bufferAntesMinutos,
      bufferDespuesMinutos: servicio.bufferDespuesMinutos,
      precio: servicio.precio,
      moneda: servicio.moneda,
      activo: servicio.activo
    };
  }

  cancelarEdicionServicio() {
    this.servicioEditandoId = null;
    this.formularioServicio = {
      sucursalId: this.sucursales()[0]?.id ?? 0,
      nombre: '',
      descripcion: '',
      duracionMinutos: 60,
      bufferAntesMinutos: 0,
      bufferDespuesMinutos: 0,
      precio: 0,
      moneda: 'MXN',
      activo: true
    };
  }

  guardarServicio() {
    this.guardandoServicio = true;
    this.error = '';
    const payload: GuardarServicioPayload = {
      ...this.formularioServicio,
      descripcion: this.normalizarTexto(this.formularioServicio.descripcion),
      moneda: (this.formularioServicio.moneda || 'MXN').toUpperCase()
    };

    const operacion = this.servicioEditandoId
      ? this.adminService.actualizarServicio(this.servicioEditandoId, payload)
      : this.adminService.crearServicio(payload);

    operacion
      .pipe(finalize(() => this.guardandoServicio = false))
      .subscribe({
        next: () => {
          this.cancelarEdicionServicio();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar el servicio.';
        }
      });
  }

  editarPrestador(prestador: PrestadorAdmin) {
    this.prestadorEditandoId = prestador.usuarioId;
    this.formularioPrestador = {
      sucursalId: prestador.sucursalId,
      correo: prestador.correo,
      contrasenaTemporal: '',
      nombreMostrar: prestador.nombreMostrar,
      biografia: prestador.biografia ?? '',
      colorAgenda: prestador.colorAgenda ?? '#2563eb',
      activo: prestador.activo,
      servicioIds: [...prestador.servicioIds]
    };
  }

  cancelarEdicionPrestador() {
    this.prestadorEditandoId = null;
    this.formularioPrestador = {
      sucursalId: this.sucursales()[0]?.id ?? 0,
      correo: '',
      contrasenaTemporal: '',
      nombreMostrar: '',
      biografia: '',
      colorAgenda: '#2563eb',
      activo: true,
      servicioIds: []
    };
  }

  guardarPrestador() {
    this.guardandoPrestador = true;
    this.error = '';
    const payload: GuardarPrestadorPayload = {
      ...this.formularioPrestador,
      correo: this.formularioPrestador.correo.trim().toLowerCase(),
      contrasenaTemporal: this.normalizarTexto(this.formularioPrestador.contrasenaTemporal),
      biografia: this.normalizarTexto(this.formularioPrestador.biografia),
      colorAgenda: this.normalizarTexto(this.formularioPrestador.colorAgenda)
    };

    const operacion = this.prestadorEditandoId
      ? this.adminService.actualizarPrestador(this.prestadorEditandoId, payload)
      : this.adminService.crearPrestador(payload);

    operacion
      .pipe(finalize(() => this.guardandoPrestador = false))
      .subscribe({
        next: () => {
          this.cancelarEdicionPrestador();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar el prestador.';
        }
      });
  }

  cambiarSucursalPrestador(sucursalId: number) {
    this.formularioPrestador.sucursalId = sucursalId;
    const serviciosValidos = new Set(this.serviciosDisponiblesPrestador().map(servicio => servicio.id));
    this.formularioPrestador.servicioIds = this.formularioPrestador.servicioIds.filter(id => serviciosValidos.has(id));
  }

  alternarServicioPrestador(servicioId: number, marcado: boolean) {
    const seleccionados = new Set(this.formularioPrestador.servicioIds);
    if (marcado) {
      seleccionados.add(servicioId);
    } else {
      seleccionados.delete(servicioId);
    }
    this.formularioPrestador.servicioIds = Array.from(seleccionados);
  }

  servicioPrestadorSeleccionado(servicioId: number): boolean {
    return this.formularioPrestador.servicioIds.includes(servicioId);
  }

  serviciosDisponiblesPrestador(): ServicioAdmin[] {
    return this.servicios().filter(servicio => servicio.sucursalId === this.formularioPrestador.sucursalId);
  }

  opcionesSujetosDisponibilidad(tipoSujeto: string): Array<{ id: number; nombre: string }> {
    if (tipoSujeto === 'PRESTADOR') {
      return this.prestadores().map(prestador => ({
        id: prestador.usuarioId,
        nombre: `${prestador.nombreMostrar} · ${prestador.sucursalNombre}`
      }));
    }
    return this.sucursales().map(sucursal => ({
      id: sucursal.id,
      nombre: sucursal.nombre
    }));
  }

  cambiarTipoSujetoRegla(tipoSujeto: string) {
    this.formularioRegla.tipoSujeto = tipoSujeto;
    this.formularioRegla.sujetoId = this.opcionesSujetosDisponibilidad(tipoSujeto)[0]?.id ?? 0;
  }

  cambiarTipoSujetoExcepcion(tipoSujeto: string) {
    this.formularioExcepcion.tipoSujeto = tipoSujeto;
    this.formularioExcepcion.sujetoId = this.opcionesSujetosDisponibilidad(tipoSujeto)[0]?.id ?? 0;
  }

  editarRegla(regla: ReglaDisponibilidadAdmin) {
    this.reglaEditandoId = regla.id;
    this.formularioRegla = {
      tipoSujeto: regla.tipoSujeto,
      sujetoId: regla.sujetoId,
      diaSemana: regla.diaSemana,
      horaInicio: regla.horaInicio,
      horaFin: regla.horaFin,
      intervaloMinutos: regla.intervaloMinutos,
      vigenteDesde: regla.vigenteDesde,
      vigenteHasta: regla.vigenteHasta
    };
  }

  cancelarEdicionRegla() {
    this.reglaEditandoId = null;
    this.formularioRegla = {
      tipoSujeto: 'SUCURSAL',
      sujetoId: this.opcionesSujetosDisponibilidad('SUCURSAL')[0]?.id ?? 0,
      diaSemana: 1,
      horaInicio: '09:00',
      horaFin: '18:00',
      intervaloMinutos: 15,
      vigenteDesde: null,
      vigenteHasta: null
    };
  }

  guardarRegla() {
    this.guardandoRegla = true;
    this.error = '';
    const payload: GuardarReglaDisponibilidadPayload = {
      ...this.formularioRegla,
      vigenteDesde: this.normalizarTexto(this.formularioRegla.vigenteDesde),
      vigenteHasta: this.normalizarTexto(this.formularioRegla.vigenteHasta)
    };
    const operacion = this.reglaEditandoId
      ? this.adminService.actualizarReglaDisponibilidad(this.reglaEditandoId, payload)
      : this.adminService.crearReglaDisponibilidad(payload);

    operacion
      .pipe(finalize(() => this.guardandoRegla = false))
      .subscribe({
        next: () => {
          this.cancelarEdicionRegla();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar la regla de disponibilidad.';
        }
      });
  }

  editarExcepcion(excepcion: ExcepcionDisponibilidadAdmin) {
    this.excepcionEditandoId = excepcion.id;
    this.formularioExcepcion = {
      tipoSujeto: excepcion.tipoSujeto,
      sujetoId: excepcion.sujetoId,
      fechaExcepcion: excepcion.fechaExcepcion,
      horaInicio: excepcion.horaInicio,
      horaFin: excepcion.horaFin,
      tipoBloqueo: excepcion.tipoBloqueo,
      motivo: excepcion.motivo
    };
  }

  cancelarEdicionExcepcion() {
    this.excepcionEditandoId = null;
    this.formularioExcepcion = {
      tipoSujeto: 'SUCURSAL',
      sujetoId: this.opcionesSujetosDisponibilidad('SUCURSAL')[0]?.id ?? 0,
      fechaExcepcion: '',
      horaInicio: null,
      horaFin: null,
      tipoBloqueo: 'BLOQUEO',
      motivo: null
    };
  }

  guardarExcepcion() {
    this.guardandoExcepcion = true;
    this.error = '';
    const payload: GuardarExcepcionDisponibilidadPayload = {
      ...this.formularioExcepcion,
      horaInicio: this.normalizarTexto(this.formularioExcepcion.horaInicio),
      horaFin: this.normalizarTexto(this.formularioExcepcion.horaFin),
      motivo: this.normalizarTexto(this.formularioExcepcion.motivo)
    };
    const operacion = this.excepcionEditandoId
      ? this.adminService.actualizarExcepcionDisponibilidad(this.excepcionEditandoId, payload)
      : this.adminService.crearExcepcionDisponibilidad(payload);

    operacion
      .pipe(finalize(() => this.guardandoExcepcion = false))
      .subscribe({
        next: () => {
          this.cancelarEdicionExcepcion();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar la excepción de disponibilidad.';
        }
      });
  }

  etiquetaDiaSemana(dia: number): string {
    return this.diasSemana.find(item => item.value === dia)?.label ?? `Día ${dia}`;
  }

  private normalizarTexto(valor: string | null): string | null {
    const limpio = valor?.trim();
    return limpio ? limpio : null;
  }
}
