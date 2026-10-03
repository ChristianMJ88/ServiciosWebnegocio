import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SesionPlataforma {
  tokenAcceso: string;
  tipoToken: string;
  usuarioId: number;
  nombre: string;
  correo: string;
  roles: string[];
  permisos: string[];
}

@Injectable({ providedIn: 'root' })
export class PlatformAuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly storageKey = 'fluora_plataforma_sesion';
  private readonly sesion = signal<SesionPlataforma | null>(this.cargar());

  readonly sesionActual = computed(() => this.sesion());
  readonly autenticado = computed(() => Boolean(this.sesion()));

  iniciar(correo: string, contrasena: string): Observable<SesionPlataforma> {
    return this.http.post<SesionPlataforma>(`${environment.apiBaseUrl}/plataforma/auth/iniciar-sesion`, {
      correo: correo.trim().toLowerCase(), contrasena
    }, { headers: { 'X-Omitir-Refresh': 'true' } }).pipe(tap(sesion => this.guardar(sesion)));
  }

  token(): string | null { return this.sesion()?.tokenAcceso ?? null; }
  tienePermiso(codigo: string): boolean { return this.sesion()?.permisos.includes(codigo) ?? false; }

  cerrar(): void {
    localStorage.removeItem(this.storageKey);
    this.sesion.set(null);
    void this.router.navigate(['/plataforma/acceso']);
  }

  private guardar(sesion: SesionPlataforma): void {
    localStorage.setItem(this.storageKey, JSON.stringify(sesion));
    this.sesion.set(sesion);
  }

  private cargar(): SesionPlataforma | null {
    try {
      const valor = localStorage.getItem(this.storageKey);
      return valor ? JSON.parse(valor) as SesionPlataforma : null;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }
}
