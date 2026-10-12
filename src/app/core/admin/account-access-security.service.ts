import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface BloqueoAccesoResponse {
  usuarioId: number;
  bloqueado: boolean;
  habilitado: boolean;
}

@Injectable({ providedIn: 'root' })
export class AccountAccessSecurityService {
  private readonly http = inject(HttpClient);

  cambiar(usuarioId: number, prestador: boolean, bloqueado: boolean, motivo: string) {
    return this.http.patch<BloqueoAccesoResponse>(
      `${environment.apiBaseUrl}/admin/${prestador ? 'prestadores' : 'usuarios-internos'}/${usuarioId}/bloqueo`,
      { bloqueado, motivo });
  }
}
