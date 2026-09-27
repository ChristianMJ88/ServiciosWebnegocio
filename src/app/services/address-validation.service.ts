import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AddressPlatformConfig {
  autocompleteHabilitado: boolean;
  validacionServidorHabilitada: boolean;
  validacionRequerida: boolean;
  browserApiKey: string | null;
  regionPredeterminada: string;
  regionesPermitidas: string[];
}

export interface AddressInput {
  calle: string;
  numeroExterior: string;
  numeroInterior: string | null;
  colonia: string;
  municipio: string;
  estado: string;
  codigoPostal: string;
  codigoPais: string;
}

export interface AddressValidationResult {
  valida: boolean;
  estadoValidacion: string;
  mensaje: string;
  direccionFormateada: string | null;
  direccion: AddressInput;
  placeId: string | null;
  latitud: number | null;
  longitud: number | null;
}

@Injectable({ providedIn: 'root' })
export class AddressValidationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/publico/direcciones`;

  configuration(): Observable<AddressPlatformConfig> {
    return this.http.get<AddressPlatformConfig>(`${this.baseUrl}/configuracion`);
  }

  validate(address: AddressInput): Observable<AddressValidationResult> {
    return this.http.post<AddressValidationResult>(`${this.baseUrl}/validar`, address);
  }
}
