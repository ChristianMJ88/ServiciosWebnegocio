import { HttpClient } from '@angular/common/http';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { EMPTY, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PlanPublicoFluora {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  moneda: string;
  precioMensual: number;
  precioAnual: number | null;
  destacado: boolean;
  diasPrueba: number;
  diasGracia: number;
  vigenteDesde: string | null;
  vigenteHasta: string | null;
  modulos: { moduloId: number; codigo: string; nombre: string; limiteUso: number | null }[];
  limites: { limiteId: number; codigo: string; nombre: string; unidad: string; valor: number }[];
}

@Injectable({providedIn:'root'})
export class PublicPlansService {
  private readonly browser=isPlatformBrowser(inject(PLATFORM_ID));
  private readonly http=inject(HttpClient);
  listar(): Observable<PlanPublicoFluora[]> {
    // No dynamic backend data in the server build; hydration policy belongs to R3.
    if(!this.browser)return EMPTY;
    return this.http.get<PlanPublicoFluora[]>(`${environment.apiBaseUrl}/publico/plataforma/planes`);
  }
}
