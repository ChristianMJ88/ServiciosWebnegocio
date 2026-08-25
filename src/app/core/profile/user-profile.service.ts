import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../auth/auth.service';
import { environment } from '../../../environments/environment';

export interface PerfilUsuarioLocal {
  nombre: string;
  puesto: string;
  fotoDataUrl: string | null;
}

export interface PerfilUsuarioBackend {
  usuarioId: number;
  correo: string;
  nombreCompleto: string | null;
  puesto: string | null;
  sucursalId: number | null;
}

const PERFIL_VACIO: PerfilUsuarioLocal = {
  nombre: '',
  puesto: '',
  fotoDataUrl: null
};

@Injectable({
  providedIn: 'root'
})
export class UserProfileService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly perfil = signal<PerfilUsuarioLocal>(PERFIL_VACIO);
  private readonly perfilBackend = signal<PerfilUsuarioBackend | null>(null);

  readonly perfilActual = computed(() => this.perfil());
  readonly perfilRegistrado = computed(() => this.perfilBackend());

  cargar() {
    this.perfil.set(this.leer());
    if (environment.apiBaseUrl && this.authService.autenticado()) {
      this.http.get<PerfilUsuarioBackend>(`${environment.apiBaseUrl}/auth/perfil`).subscribe({
        next: perfil => this.perfilBackend.set(perfil),
        error: () => this.perfilBackend.set(null)
      });
    }
  }

  guardar(perfil: PerfilUsuarioLocal) {
    const normalizado: PerfilUsuarioLocal = {
      nombre: perfil.nombre.trim(),
      puesto: perfil.puesto.trim(),
      fotoDataUrl: perfil.fotoDataUrl || null
    };
    localStorage.setItem(this.clave(), JSON.stringify(normalizado));
    this.perfil.set(normalizado);
  }

  private leer(): PerfilUsuarioLocal {
    try {
      const raw = localStorage.getItem(this.clave());
      if (!raw) {
        return PERFIL_VACIO;
      }
      const perfil = JSON.parse(raw) as Partial<PerfilUsuarioLocal>;
      return {
        nombre: perfil.nombre?.trim() ?? '',
        puesto: perfil.puesto?.trim() ?? '',
        fotoDataUrl: perfil.fotoDataUrl || null
      };
    } catch {
      return PERFIL_VACIO;
    }
  }

  private clave(): string {
    const sesion = this.authService.sesionActual();
    return `agenda_admin_profile_${sesion?.empresaId ?? 'empresa'}_${sesion?.usuarioId ?? 'usuario'}`;
  }
}
