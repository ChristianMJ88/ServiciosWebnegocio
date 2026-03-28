import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, finalize, map, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SesionUsuario {
  tokenAcceso: string;
  tokenActualizacion: string;
  usuarioId: number;
  empresaId: number;
  roles: string[];
  correo: string;
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

interface RespuestaTokenJwt {
  tokenAcceso: string;
  tokenActualizacion: string;
  tipoToken: string;
  usuarioId: number;
  empresaId: number;
  roles: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storageKey = 'agenda_sesion';
  private readonly sesion = signal<SesionUsuario | null>(this.loadSession());
  private refreshEnCurso$: Observable<string> | null = null;

  readonly sesionActual = computed(() => this.sesion());
  readonly autenticado = computed(() => Boolean(this.sesion()));
  readonly esCliente = computed(() => this.sesion()?.roles.includes('CLIENTE') ?? false);
  readonly esStaff = computed(() => this.sesion()?.roles.includes('STAFF') ?? false);
  readonly esAdmin = computed(() => this.sesion()?.roles.includes('ADMIN') ?? false);
  readonly esRecepcionista = computed(() => this.sesion()?.roles.includes('RECEPCIONISTA') ?? false);
  readonly esCajero = computed(() => this.sesion()?.roles.includes('CAJERO') ?? false);
  readonly puedeVerAdmin = computed(() => this.esAdmin());
  readonly puedeVerRecepcion = computed(() => this.esAdmin() || this.esRecepcionista());
  readonly puedeVerCaja = computed(() => this.esAdmin() || this.esRecepcionista() || this.esCajero());
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
    if (this.esStaff()) {
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

    this.sesion.set(sesionPersistida);
    return true;
  }

  rutaPanelPersistida(): string {
    const roles = this.sesion()?.roles ?? this.loadSession()?.roles ?? [];
    if (roles.includes('ADMIN')) {
      return '/admin';
    }
    if (roles.includes('RECEPCIONISTA')) {
      return '/recepcion';
    }
    if (roles.includes('CAJERO')) {
      return '/caja';
    }
    if (roles.includes('STAFF')) {
      return '/staff';
    }
    return '/mi-cuenta';
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

    return this.http
      .post<RespuestaTokenJwt>(`${environment.apiBaseUrl}/auth/iniciar-sesion`, {
        empresaId: environment.empresaId,
        correo: credentials.correo.trim().toLowerCase(),
        contrasena: credentials.contrasena
      })
      .pipe(
        tap(response => {
          this.persistirSesionDesdeRespuesta(response, credentials.correo.trim().toLowerCase());
        })
      );
  }

  register(data: Omit<RegistroRequest, 'empresaId'>): Observable<void> {
    if (!environment.apiBaseUrl) {
      return throwError(() => new Error('El registro todavía no está disponible en este entorno.'));
    }

    return this.http.post<void>(`${environment.apiBaseUrl}/auth/registrar-cliente`, {
      empresaId: environment.empresaId,
      ...data,
      correo: data.correo.trim().toLowerCase()
    });
  }

  logout() {
    const tokenActualizacion = this.sesion()?.tokenActualizacion;
    if (!tokenActualizacion || !environment.apiBaseUrl) {
      this.limpiarSesion();
      return;
    }

    this.http.post<void>(
      `${environment.apiBaseUrl}/auth/cerrar-sesion`,
      { tokenActualizacion },
      { headers: { 'X-Omitir-Refresh': 'true' } }
    ).subscribe({
      next: () => this.limpiarSesion(),
      error: () => this.limpiarSesion()
    });
  }

  getTokenAcceso(): string | null {
    return this.sesion()?.tokenAcceso ?? null;
  }

  getTokenActualizacion(): string | null {
    return this.sesion()?.tokenActualizacion ?? null;
  }

  refrescarToken(): Observable<string> {
    if (this.refreshEnCurso$) {
      return this.refreshEnCurso$;
    }

    const tokenActualizacion = this.getTokenActualizacion();
    if (!tokenActualizacion || !environment.apiBaseUrl) {
      this.limpiarSesion();
      return throwError(() => new Error('No hay token de actualización disponible.'));
    }

    this.refreshEnCurso$ = this.http
      .post<RespuestaTokenJwt>(
        `${environment.apiBaseUrl}/auth/refrescar-token`,
        { tokenActualizacion },
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
    localStorage.removeItem(this.storageKey);
    this.sesion.set(null);
  }

  private saveSession(sesion: SesionUsuario) {
    localStorage.setItem(this.storageKey, JSON.stringify(sesion));
    this.sesion.set(sesion);
  }

  private persistirSesionDesdeRespuesta(response: RespuestaTokenJwt, correoFallback?: string) {
    const payload = this.decodeJwtPayload(response.tokenAcceso);
    const sesion: SesionUsuario = {
      tokenAcceso: response.tokenAcceso,
      tokenActualizacion: response.tokenActualizacion,
      usuarioId: response.usuarioId,
      empresaId: response.empresaId,
      roles: response.roles,
      correo: payload?.sub || correoFallback || this.sesion()?.correo || ''
    };
    this.saveSession(sesion);
  }

  private loadSession(): SesionUsuario | null {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as SesionUsuario;
    } catch {
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
}
