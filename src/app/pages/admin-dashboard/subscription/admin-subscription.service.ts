import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface EstadoSuscripcionEmpresa {
  tieneSuscripcion: boolean;
  suscripcionId: number | null;
  planId: number | null;
  planNombre: string | null;
  estado: string | null;
  ciclo: string | null;
  precioAcordado: number | null;
  moneda: string | null;
  iniciaEn: string | null;
  pruebaHasta: string | null;
  periodoActualHasta: string | null;
  cancelarAlFinalPeriodo: boolean;
  administrableStripe: boolean;
}

@Injectable({providedIn:'root'})
export class AdminSubscriptionService {
  private readonly http=inject(HttpClient);
  private readonly baseUrl=`${environment.apiBaseUrl}/admin/suscripcion`;
  estado(): Observable<EstadoSuscripcionEmpresa> { return this.http.get<EstadoSuscripcionEmpresa>(this.baseUrl); }
  checkout(planId:number,ciclo:'MENSUAL'|'ANUAL'): Observable<{url:string}> { return this.http.post<{url:string}>(`${this.baseUrl}/checkout`,{planId,ciclo}); }
  portal(): Observable<{url:string}> { return this.http.post<{url:string}>(`${this.baseUrl}/portal`,{}); }
  correo(): Observable<{correo:string|null}> { return this.http.get<{correo:string|null}>(`${this.baseUrl}/correo`); }
  guardarCorreo(correo:string): Observable<void> { return this.http.put<void>(`${this.baseUrl}/correo`,{correo}); }
}
