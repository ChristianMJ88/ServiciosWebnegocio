import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ParametroSistemaAdmin {
  clave: string; nombre: string; descripcion: string | null; tipo: string; valor: string;
  valorPredeterminado: string; categoria: string; opciones: string[];
  personalizado: boolean; editable: boolean; actualizadoEn: string | null;
}
@Injectable({ providedIn: 'root' })
export class SystemParametersService {
  private readonly http = inject(HttpClient);
  listar(): Observable<ParametroSistemaAdmin[]> { return this.http.get<ParametroSistemaAdmin[]>(`${environment.apiBaseUrl}/admin/parametros-sistema`); }
  guardar(clave: string, valor: string): Observable<ParametroSistemaAdmin> { return this.http.put<ParametroSistemaAdmin>(`${environment.apiBaseUrl}/admin/parametros-sistema/${encodeURIComponent(clave)}`, { valor }); }
  restablecer(clave: string): Observable<ParametroSistemaAdmin> { return this.http.delete<ParametroSistemaAdmin>(`${environment.apiBaseUrl}/admin/parametros-sistema/${encodeURIComponent(clave)}`); }
}
