import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SucursalCatalogo {
  id: number;
  empresaId: number;
  nombre: string;
  direccion: string;
  telefono: string;
  zonaHoraria: string;
}

export interface ServicioCatalogo {
  id: number;
  sucursalId: number;
  nombre: string;
  descripcion: string;
  duracionMinutos: number;
  bufferAntesMinutos: number;
  bufferDespuesMinutos: number;
  precio: number;
  moneda: string;
}

@Injectable({
  providedIn: 'root'
})
export class BookingDataService {
  private readonly LEGACY_BRANCHES: SucursalCatalogo[] = [
    {
      id: 1,
      empresaId: environment.empresaId,
      nombre: 'Sucursal Principal',
      direccion: 'Por definir',
      telefono: '5550000000',
      zonaHoraria: 'America/Mexico_City'
    }
  ];

  private readonly LEGACY_SERVICES: ServicioCatalogo[] = [
    {
      id: 1,
      sucursalId: 1,
      nombre: 'Manicura',
      descripcion: 'Servicio clasico',
      duracionMinutos: 60,
      bufferAntesMinutos: 0,
      bufferDespuesMinutos: 10,
      precio: 250,
      moneda: 'MXN'
    },
    {
      id: 2,
      sucursalId: 1,
      nombre: 'Pedicura',
      descripcion: 'Servicio clasico',
      duracionMinutos: 75,
      bufferAntesMinutos: 0,
      bufferDespuesMinutos: 15,
      precio: 320,
      moneda: 'MXN'
    },
    {
      id: 3,
      sucursalId: 1,
      nombre: 'Uñas Acrílicas',
      descripcion: 'Servicio especial',
      duracionMinutos: 90,
      bufferAntesMinutos: 0,
      bufferDespuesMinutos: 15,
      precio: 450,
      moneda: 'MXN'
    }
  ];

  constructor(private http: HttpClient) {}

  getBranches(): Observable<SucursalCatalogo[]> {
    if (environment.useBackendCatalog && environment.apiBaseUrl) {
      return this.http.get<SucursalCatalogo[]>(
        `${environment.apiBaseUrl}/publico/sucursales?empresaId=${environment.empresaId}`
      ).pipe(
        catchError(error => {
          if (environment.allowLegacyFallback) {
            console.warn('Fallo el catálogo backend, usando fallback legacy.', error);
            return of([...this.LEGACY_BRANCHES]);
          }
          throw error;
        })
      );
    }

    return of([...this.LEGACY_BRANCHES]);
  }

  getServices(branchId: number): Observable<ServicioCatalogo[]> {
    if (environment.useBackendCatalog && environment.apiBaseUrl) {
      return this.http.get<ServicioCatalogo[]>(
        `${environment.apiBaseUrl}/publico/servicios?sucursalId=${branchId}`
      ).pipe(
        catchError(error => {
          if (environment.allowLegacyFallback) {
            console.warn('Fallo el catálogo backend, usando servicios legacy.', error);
            return of(this.LEGACY_SERVICES.filter(service => service.sucursalId === branchId));
          }
          throw error;
        })
      );
    }

    return of(this.LEGACY_SERVICES.filter(service => service.sucursalId === branchId));
  }
}
