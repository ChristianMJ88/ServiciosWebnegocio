import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { ConciliacionPago, ConfiguracionCheckout, ConfiguracionReporteLiberacion, ConexionProveedorPago, MetodoPagoCheckout, PaymentsAdminService, TerminalPoint, WebhookMercadoPago } from './payments-admin.service';
import { AdminService, SucursalAdmin } from '../../../core/admin/admin.service';

@Component({
  selector: 'app-admin-payments-section',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-payments-section.component.html',
  styleUrl: './admin-payments-section.component.css'
})
export class AdminPaymentsSectionComponent implements OnInit {
  private readonly servicio = inject(PaymentsAdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly admin = inject(AdminService);

  readonly conexion = signal<ConexionProveedorPago | null>(null);
  readonly configuracion = signal<ConfiguracionCheckout | null>(null);
  readonly cargando = signal(true);
  readonly procesando = signal(false);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly terminales = signal<TerminalPoint[]>([]);
  readonly sucursales = signal<SucursalAdmin[]>([]);
  readonly cargandoTerminales = signal(false);
  readonly metodosPago = signal<MetodoPagoCheckout[]>([]);
  readonly cargandoMetodos = signal(false);
  readonly webhook = signal<WebhookMercadoPago | null>(null);
  readonly conciliaciones = signal<ConciliacionPago[]>([]);
  readonly configuracionReporte = signal<ConfiguracionReporteLiberacion | null>(null);
  readonly monedaResumen = computed(() => {
    const monedas = new Set(this.conciliaciones().map(item => item.moneda));
    if (monedas.size === 0) return this.configuracion()?.moneda ?? null;
    return monedas.size === 1 ? [...monedas][0] : null;
  });
  readonly brutoConciliado = computed(() => this.monedaResumen() == null ? null
    : this.conciliaciones().reduce((total, item) => total + item.importeBruto, 0));
  readonly netoInformado = computed(() => this.monedaResumen() == null ? null
    : this.conciliaciones().filter(item => item.liquidacionDefinitiva)
      .reduce((total, item) => total + (item.importeNeto ?? 0), 0));
  readonly pendientesConciliacion = computed(() => this.conciliaciones().filter(item => !item.liquidacionDefinitiva && item.estado !== 'REEMBOLSADA').length);
  terminalSucursal: Record<string, number | null> = {};
  terminalNombre: Record<string, string> = {};

  ngOnInit(): void {
    const resultado = this.route.snapshot.queryParamMap.get('mercadoPagoOAuth');
    if (resultado === 'ok') this.mensaje.set('La cuenta de Mercado Pago quedó conectada correctamente.');
    if (resultado === 'error') {
      this.error.set(this.route.snapshot.queryParamMap.get('mensaje') || 'No fue posible conectar Mercado Pago.');
    }
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    forkJoin({
      conexion: this.servicio.estado(),
      configuracion: this.servicio.obtenerConfiguracion(),
      webhook: this.servicio.webhook(),
      conciliaciones: this.servicio.conciliaciones(),
      configuracionReporte: this.servicio.configuracionReporteLiberacion().pipe(catchError(() => of(null)))
    })
      .pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: respuesta => {
        this.conexion.set(respuesta.conexion);
        this.configuracion.set(respuesta.configuracion);
        this.webhook.set(respuesta.webhook);
        this.conciliaciones.set(respuesta.conciliaciones);
        this.configuracionReporte.set(respuesta.configuracionReporte);
        if (respuesta.conexion.estado === 'CONECTADO') { this.cargarTerminales(); this.cargarMetodos(); }
      },
      error: err => this.error.set(this.detalleError(err, 'No fue posible consultar el estado de pagos.'))
    });
  }

  async copiarWebhook(): Promise<void> {
    const url = this.webhook()?.url;
    if (!url) return;
    await navigator.clipboard.writeText(url);
    this.mensaje.set('URL del webhook copiada.');
  }

  cargarMetodos(): void {
    this.cargandoMetodos.set(true);
    this.servicio.metodosPago().pipe(finalize(() => this.cargandoMetodos.set(false))).subscribe({
      next: metodos => this.metodosPago.set(metodos),
      error: err => this.error.set(this.detalleError(err, 'No fue posible consultar los métodos disponibles.'))
    });
  }

  guardarMetodo(metodo: MetodoPagoCheckout, activo: boolean, vigenciaDias: number): void {
    if (this.procesando()) return;
    this.procesando.set(true); this.error.set(null); this.mensaje.set(null);
    this.servicio.guardarMetodoPago({ metodoId: metodo.id, activo, soloProductos: true, vigenciaDias })
      .pipe(finalize(() => this.procesando.set(false))).subscribe({
        next: guardado => {
          this.metodosPago.update(items => items.map(item => item.id === guardado.id ? guardado : item));
          this.mensaje.set(`${guardado.nombre} quedó ${guardado.activo ? 'habilitado' : 'deshabilitado'} para productos.`);
        },
        error: err => this.error.set(this.detalleError(err, 'No fue posible guardar el método de pago.'))
      });
  }

  cargarTerminales(): void {
    this.cargandoTerminales.set(true);
    forkJoin({ terminales: this.servicio.terminalesPoint(), sucursales: this.admin.getSucursales() })
      .pipe(finalize(() => this.cargandoTerminales.set(false))).subscribe({
        next: ({ terminales, sucursales }) => {
          this.terminales.set(terminales); this.sucursales.set(sucursales.filter(item => item.activa));
          terminales.forEach(item => {
            this.terminalSucursal[item.terminalId] = item.sucursalId;
            this.terminalNombre[item.terminalId] = item.nombre;
          });
        },
        error: err => this.error.set(this.detalleError(err, 'No fue posible consultar las terminales Point.'))
      });
  }

  guardarTerminal(terminal: TerminalPoint): void {
    const sucursalId = this.terminalSucursal[terminal.terminalId];
    const nombre = this.terminalNombre[terminal.terminalId]?.trim();
    if (!sucursalId || !nombre || this.procesando()) return;
    this.procesando.set(true); this.error.set(null); this.mensaje.set(null);
    this.servicio.guardarTerminalPoint({ terminalId: terminal.terminalId, sucursalId, nombre, activa: true })
      .pipe(finalize(() => this.procesando.set(false))).subscribe({
        next: guardada => {
          this.terminales.update(items => items.map(item => item.terminalId === guardada.terminalId ? guardada : item));
          this.mensaje.set(`La terminal ${guardada.nombre} quedó vinculada con ${guardada.sucursalNombre}.`);
        },
        error: err => this.error.set(this.detalleError(err, 'No fue posible guardar la terminal Point.'))
      });
  }

  conectar(): void {
    if (this.procesando()) return;
    this.procesando.set(true);
    this.error.set(null);
    this.servicio.iniciar().pipe(finalize(() => this.procesando.set(false))).subscribe({
      next: respuesta => window.location.assign(respuesta.urlAutorizacion),
      error: err => this.error.set(this.detalleError(err, 'Mercado Pago todavía no está configurado en Fluora.'))
    });
  }

  conectarPruebas(): void {
    if (this.procesando()) return;
    this.procesando.set(true);
    this.error.set(null);
    this.servicio.conectarPruebas().pipe(finalize(() => this.procesando.set(false))).subscribe({
      next: conexion => {
        this.conexion.set(conexion);
        this.cargarConfiguracion();
        this.mensaje.set('Las credenciales de prueba quedaron activadas para este negocio.');
      },
      error: err => this.error.set(this.detalleError(err, 'Las credenciales de prueba todavía no están configuradas.'))
    });
  }

  desconectar(): void {
    if (this.procesando() || !window.confirm('¿Desconectar Mercado Pago de este negocio? Los cobros en línea dejarán de estar disponibles.')) return;
    this.procesando.set(true);
    this.error.set(null);
    this.servicio.desconectar().pipe(finalize(() => this.procesando.set(false))).subscribe({
      next: conexion => {
        this.conexion.set(conexion);
        this.configuracion.update(actual => actual ? { ...actual, habilitado: false, conectado: false } : actual);
        this.mensaje.set('Mercado Pago se desconectó de este negocio.');
      },
      error: err => this.error.set(this.detalleError(err, 'No fue posible desconectar Mercado Pago.'))
    });
  }

  actualizar<K extends keyof ConfiguracionCheckout>(campo: K, valor: ConfiguracionCheckout[K]): void {
    this.configuracion.update(actual => actual ? { ...actual, [campo]: valor } : actual);
  }

  guardarConfiguracion(): void {
    const configuracion = this.configuracion();
    if (!configuracion || this.procesando()) return;
    this.procesando.set(true);
    this.error.set(null);
    this.mensaje.set(null);
    this.servicio.guardarConfiguracion(configuracion).pipe(finalize(() => this.procesando.set(false))).subscribe({
      next: guardada => {
        this.configuracion.set(guardada);
        this.mensaje.set(guardada.habilitado
          ? 'El checkout quedó habilitado para el sitio público.'
          : 'La configuración del checkout quedó guardada.');
      },
      error: err => this.error.set(this.detalleError(err, 'No fue posible guardar la configuración del checkout.'))
    });
  }

  private cargarConfiguracion(): void {
    this.servicio.obtenerConfiguracion().subscribe({
      next: configuracion => this.configuracion.set(configuracion),
      error: err => this.error.set(this.detalleError(err, 'No fue posible consultar la configuración del checkout.'))
    });
  }

  private detalleError(error: any, predeterminado: string): string {
    return error?.error?.message || error?.error?.detail || predeterminado;
  }
}
