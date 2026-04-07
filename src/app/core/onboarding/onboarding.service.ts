import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RegistrarEmpresaPayload {
  nombreEmpresa: string;
  slug?: string;
  giro: string;
  nombreAdministrador: string;
  correoAdministrador: string;
  telefonoAdministrador: string;
  contrasena: string;
  zonaHoraria?: string;
}

export interface RegistrarEmpresaResponse {
  empresaId: number;
  slug: string;
  nombreEmpresa: string;
  correoAdministrador: string;
  rutaSitioPublico: string;
  rutaAcceso: string;
}

@Injectable({
  providedIn: 'root'
})
export class OnboardingService {
  private readonly http = inject(HttpClient);

  registrarEmpresa(payload: RegistrarEmpresaPayload): Observable<RegistrarEmpresaResponse> {
    if (!environment.apiBaseUrl) {
      return throwError(() => new Error('El onboarding todavía no está disponible en este entorno.'));
    }

    return this.http.post<RegistrarEmpresaResponse>(`${environment.apiBaseUrl}/onboarding/empresas`, payload);
  }
}
