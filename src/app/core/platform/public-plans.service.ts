import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
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
  private readonly http=inject(HttpClient);
  listar(): Observable<PlanPublicoFluora[]> {
    return this.http.get<PlanPublicoFluora[]>(`${environment.apiBaseUrl}/publico/plataforma/planes`);
  }
}
