import { Injectable, NgZone, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, finalize, map, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PERMISOS } from './permissions';
import { TenantContextService } from '../tenant/tenant-context.service';

export interface SesionUsuario {
  tokenAcceso: string;
  usuarioId: number;
  empresaId: number;
  empresaSlug?: string;
  empresaNombre?: string;
  roles: string[];
  permisos: string[];
  sucursalesPermitidas: number[];
  correo: string;
  correoVerificado: boolean;
}

interface LoginRequest {
  empresaId: number;
  correo: string;
  contrasena: string;
}

interface RegistroRequest {
  empresaId: number;
  nombreCompleto: string;
  correo: string;
  telefono: string;
  contrasena: string;
}

export interface RespuestaTokenJwt {
  tokenAcceso: string;
  tipoToken: string;
  usuarioId: number;
  empresaId: number;
  empresaSlug?: string;
  empresaNombre?: string;
  correoVerificado: boolean;
  roles: string[];
  permisos: string[];
  sucursalesPermitidas: number[];
}

export interface EmpresaAccesoApp {
  empresaId: number;
  empresaSlug: string;
  empresaNombre: string;
  roles: string[];
  permisos: string[];
}

export interface RespuestaAccesoApp {
  estado: 'AUTENTICADO' | 'SELECCION_EMPRESA';
  mensaje: string;
  empresas: EmpresaAccesoApp[] | null;
  sesion: RespuestaTokenJwt | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly ngZone = inject(NgZone);
  private readonly tenantContext = inject(TenantContextService);
  private readonly storageKey = 'agenda_sesion';
  private readonly sesion = signal<SesionUsuario | null>(this.loadSession());
  private refreshEnCurso$: Observable<string> | null = null;

  readonly sesionActual = computed(() => this.sesion());
  readonly autenticado = computed(() => Boolean(this.sesion()));
  readonly esCliente = computed(() => this.tienePermiso(PERMISOS.clientePanel));
  readonly esStaff = computed(() => this.tienePermiso(PERMISOS.staffPanel));
  readonly esAdmin = computed(() => this.tienePermiso(PERMISOS.panelAdmin));
  readonly esRecepcionista = computed(() => this.tienePermiso(PERMISOS.recepcionAcceso));
  readonly esCajero = computed(() => this.tienePermiso(PERMISOS.cajaAcceso));
  readonly puedeVerAdmin = computed(() => this.tienePermiso(PERMISOS.panelAdmin));
  readonly puedeVerRecepcion = computed(() => this.tienePermiso(PERMISOS.recepcionAcceso));
  readonly puedeVerCaja = computed(() => this.tienePermiso(PERMISOS.cajaAcceso));
  readonly puedeVerStaff = computed(() => this.tienePermiso(PERMISOS.staffPanel));
  readonly puedeVerPanelCliente = computed(() => this.tienePermiso(PERMISOS.clientePanel));
  readonly puedeGestionarRecepcionCitas = computed(() => this.tienePermiso(PERMISOS.recepcionCitasGestionar));
  readonly puedeBuscarClientesRecepcion = computed(() => this.tienePermiso(PERMISOS.recepcionClientesVer));
  readonly puedeRegistrarCheckInRecepcion = computed(() => this.tienePermiso(PERMISOS.recepcionCheckin));
  readonly puedeCobrarCaja = computed(() => this.tienePermiso(PERMISOS.cajaCobrar));
  readonly puedeGestionarSesionCaja = computed(() => this.tienePermiso(PERMISOS.cajaSesionGestionar));
  readonly puedeGestionarMovimientosCaja = computed(() => this.tienePermiso(PERMISOS.cajaMovimientosGestionar));
  readonly puedeVerAgendaStaff = computed(() => this.tienePermiso(PERMISOS.staffAgendaVer));
  readonly puedeGestionarCitasStaff = computed(() => this.tienePermiso(PERMISOS.staffCitasGestionar));
  readonly puedeGestionarDisponibilidadStaff = computed(() => this.tienePermiso(PERMISOS.staffDisponibilidadGestionar));
  readonly puedeGestionarConfiguracionEmpresa = computed(() => this.tienePermiso(PERMISOS.configuracionEmpresaGestionar));
  readonly puedeGestionarWhatsapp = computed(() => this.tienePermiso(PERMISOS.whatsappConfigurar));
  readonly puedeVerContactosAdmin = computed(() => this.tienePermiso(PERMISOS.contactosAdminVer));
  readonly puedeVerReportesAdmin = computed(() => this.tienePermiso(PERMISOS.reportesAdminVer));
  readonly puedeGestionarCitasAdmin = computed(() => this.tienePermiso(PERMISOS.citasAdminGestionar));
  readonly puedeGestionarSucursales = computed(() => this.tienePermiso(PERMISOS.sucursalesGestionar));
  readonly puedeGestionarServicios = computed(() => this.tienePermiso(PERMISOS.serviciosGestionar));
  readonly puedeGestionarPrestadores = computed(() => this.tienePermiso(PERMISOS.prestadoresGestionar));
  readonly puedeGestionarUsuariosInternos = computed(() => this.tienePermiso(PERMISOS.usuariosInternosGestionar));
  readonly sucursalesPermitidas = computed(() =>
    (this.sesion()?.sucursalesPermitidas ?? [])
      .map(valor => Number(valor))
      .filter(valor => Number.isFinite(valor) && valor > 0)
  );
  readonly rutaPanel = computed(() => {
    if (this.puedeVerAdmin()) {
      return '/admin';
    }
    if (this.puedeVerRecepcion()) {
      return '/recepcion';
    }
    if (this.puedeVerCaja()) {
      return '/caja';
    }
    if (this.puedeVerStaff()) {
      return '/staff';
    }
    return '/mi-cuenta';
  });

  constructor() {
    this.sincronizarSesionPersistida();
  }

  asegurarSesion(): boolean {
    return this.autenticado() || this.sincronizarSesionPersistida();
  }

  haySesionPersistida(): boolean {
    return Boolean(this.loadSession());
  }

  sincronizarSesionPersistida(): boolean {
    if (this.sesion()) {
      return true;
    }

    const sesionPersistida = this.loadSession();
    if (!sesionPersistida) {
      return false;
    }

    this.ngZone.run(() => {
      this.sesion.set(sesionPersistida);
    });
    return true;
  }

  rutaPanelPersistida(): string {
    const sesion = this.sesion() ?? this.loadSession();
    const permisos = sesion?.permisos ?? [];
    if (permisos.includes(PERMISOS.panelAdmin)) {
      return '/admin';
    }
    if (permisos.includes(PERMISOS.recepcionAcceso)) {
      return '/recepcion';
    }
    if (permisos.includes(PERMISOS.cajaAcceso)) {
      return '/caja';
    }
    if (permisos.includes(PERMISOS.staffPanel)) {
      return '/staff';
    }
    if (permisos.includes(PERMISOS.clientePanel)) {
      return '/mi-cuenta';
    }
    return '/mi-cuenta';
  }

  tienePermiso(permiso: string): boolean {
    return this.sesion()?.permisos.includes(permiso) ?? false;
  }

  nombreUsuarioVisible(): string {
    const correo = this.sesion()?.correo ?? this.loadSession()?.correo ?? '';
    return this.formatearNombreDesdeCorreo(correo);
  }

  inicialesUsuarioVisible(): string {
    const nombre = this.nombreUsuarioVisible();
    if (!nombre || nombre === 'Usuario') {
      return 'US';
    }

    const partes = nombre.split(' ').filter(Boolean);
    return partes
      .slice(0, 2)
      .map(parte => parte[0]?.toUpperCase() ?? '')
      .join('') || 'US';
  }

  login(credentials: Omit<LoginRequest, 'empresaId'>): Observable<RespuestaTokenJwt> {
    if (!environment.apiBaseUrl) {
      return throwError(() => new Error('La autenticación todavía no está disponible en este entorno.'));
    }

    const empresaId = this.obtenerEmpresaIdActual();
    return this.http
      .post<RespuestaTokenJwt>(`${environment.apiBaseUrl}/auth/iniciar-sesion`, {
        empresaId,
        correo: credentials.correo.trim().toLowerCase(),
        contrasena: credentials.contrasena
      })
      .pipe(
        tap(response => {
          this.persistirSesionDesdeRespuesta(response, credentials.correo.trim().toLowerCase());
        })
      );
  }

  appLogin(payload: { correo: string; contrasena: string; empresaId?: number | null }): Observable<RespuestaAccesoApp> {
    if (!environment.apiBaseUrl) {
      return throwError(() => new Error('La autenticación todavía no está disponible en este entorno.'));
    }

    return this.http
      .post<RespuestaAccesoApp>(`${environment.apiBaseUrl}/auth/app-login`, {
        correo: payload.correo.trim().toLowerCase(),
        contrasena: payload.contrasena,
        empresaId: payload.empresaId ?? null
      })
      .pipe(
        tap(response => {
          if (response.estado === 'AUTENTICADO' && response.sesion) {
            this.persistirSesionDesdeRespuesta(response.sesion, payload.correo.trim().toLowerCase());
          }
        })
      );
  }

  register(data: Omit<RegistroRequest, 'empresaId'>): Observable<void> {
    if (!environment.apiBaseUrl) {
      return throwError(() => new Error('El registro todavía no está disponible en este entorno.'));
    }

    const empresaId = this.obtenerEmpresaIdActual();
    return this.http.post<void>(`${environment.apiBaseUrl}/auth/registrar-cliente`, {
      empresaId,
      ...data,
      correo: data.correo.trim().toLowerCase()
    });
  }

  logout() {
    if (!environment.apiBaseUrl) {
      this.limpiarSesion();
      return;
    }

    this.http.post<void>(
      `${environment.apiBaseUrl}/auth/cerrar-sesion`,
      {},
      { headers: { 'X-Omitir-Refresh': 'true' } }
    ).subscribe({
      next: () => this.limpiarSesion(),
      error: () => this.limpiarSesion()
    });
  }

  getTokenAcceso(): string | null {
    this.sincronizarSesionPersistida();
    return this.sesion()?.tokenAcceso ?? this.loadSession()?.tokenAcceso ?? null;
  }

  refrescarToken(): Observable<string> {
    if (this.refreshEnCurso$) {
      return this.refreshEnCurso$;
    }

    if (!environment.apiBaseUrl) {
      this.limpiarSesion();
      return throwError(() => new Error('La renovación de sesión no está disponible.'));
    }

    this.refreshEnCurso$ = this.http
      .post<RespuestaTokenJwt>(
        `${environment.apiBaseUrl}/auth/refrescar-token`,
        {},
        { headers: { 'X-Omitir-Refresh': 'true' } }
      )
      .pipe(
        tap(response => this.persistirSesionDesdeRespuesta(response)),
        map(response => response.tokenAcceso),
        catchError(error => {
          this.limpiarSesion();
          return throwError(() => error);
        }),
        finalize(() => this.refreshEnCurso$ = null),
        shareReplay(1)
      );

    return this.refreshEnCurso$;
  }

  limpiarSesion() {
    this.ngZone.run(() => {
      sessionStorage.removeItem(this.storageKey);
      localStorage.removeItem(this.storageKey);
      this.sesion.set(null);
    });
  }

  adoptarSesion(response: RespuestaTokenJwt, correo: string): void {
    this.persistirSesionDesdeRespuesta(response, correo.trim().toLowerCase());
  }

  private saveSession(sesion: SesionUsuario) {
    this.ngZone.run(() => {
      sessionStorage.setItem(this.storageKey, JSON.stringify(sesion));
      localStorage.removeItem(this.storageKey);
      this.sesion.set(sesion);
    });
  }

  private persistirSesionDesdeRespuesta(response: RespuestaTokenJwt, correoFallback?: string) {
    const payload = this.decodeJwtPayload(response.tokenAcceso);
    const sesion: SesionUsuario = {
      tokenAcceso: response.tokenAcceso,
      usuarioId: response.usuarioId,
      empresaId: response.empresaId,
      empresaSlug: response.empresaSlug,
      empresaNombre: response.empresaNombre,
      roles: response.roles,
      permisos: response.permisos ?? [],
      sucursalesPermitidas: response.sucursalesPermitidas ?? [],
      correo: payload?.sub || correoFallback || this.sesion()?.correo || '',
      correoVerificado: response.correoVerificado ?? false
    };
    this.saveSession(sesion);
  }

  private loadSession(): SesionUsuario | null {
    const raw = sessionStorage.getItem(this.storageKey) ?? localStorage.getItem(this.storageKey);
    if (!raw) {
      return null;
    }

    try {
      const sesion = JSON.parse(raw) as SesionUsuario;
      if (!sessionStorage.getItem(this.storageKey)) {
        sessionStorage.setItem(this.storageKey, raw);
        localStorage.removeItem(this.storageKey);
      }
      return sesion;
    } catch {
      sessionStorage.removeItem(this.storageKey);
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }

  private decodeJwtPayload(token: string): { sub?: string } | null {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch {
      return null;
    }
  }

  private formatearNombreDesdeCorreo(correo: string): string {
    const local = correo.split('@')[0]?.trim();
    if (!local) {
      return 'Usuario';
    }

    return local
      .split(/[._-]+/)
      .filter(Boolean)
      .map(fragmento => fragmento.charAt(0).toUpperCase() + fragmento.slice(1))
      .join(' ');
  }

  private obtenerEmpresaIdActual(): number {
    const empresaId = this.tenantContext.empresaId();
    if (!empresaId) {
      throw new Error('No hay un tenant activo para esta operación');
    }
    return empresaId;
  }
}
