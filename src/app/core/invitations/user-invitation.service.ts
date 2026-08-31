import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface UserInvitationDetails {
  empresa: string;
  correo: string;
  nombreCompleto: string;
  puesto: string | null;
  expiraEn: string;
}

@Injectable({ providedIn: 'root' })
export class UserInvitationService {
  private readonly http = inject(HttpClient);

  get(token: string) {
    return this.http.get<UserInvitationDetails>(`${environment.apiBaseUrl}/publico/invitaciones/usuario`, {
      params: { token }
    });
  }

  accept(token: string, contrasena: string, registroSocialToken?: string) {
    return this.http.post(`${environment.apiBaseUrl}/publico/invitaciones/usuario/aceptar`, {
      token,
      contrasena,
      registroSocialToken
    });
  }
}
