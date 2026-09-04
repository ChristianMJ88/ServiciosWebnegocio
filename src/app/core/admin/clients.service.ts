import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ClienteAdmin {
  id: number;
  nombreCompleto: string;
  correo: string;
  telefono: string;
  aceptaWhatsapp: boolean;
  notas: string | null;
  totalCitas: number;
  ultimaCita: string | null;
}
export type GuardarCliente = Pick<ClienteAdmin, 'nombreCompleto' | 'correo' | 'telefono' | 'aceptaWhatsapp' | 'notas'>;

@Injectable({ providedIn: 'root' })
export class ClientsService {
  private readonly http = inject(HttpClient);
  listar(busqueda = ''): Observable<ClienteAdmin[]> {
    const params = busqueda.trim() ? new HttpParams().set('busqueda', busqueda.trim()) : undefined;
    return this.http.get<ClienteAdmin[]>(`${environment.apiBaseUrl}/admin/clientes`, { params });
  }
  crear(payload: GuardarCliente): Observable<ClienteAdmin> {
    return this.http.post<ClienteAdmin>(`${environment.apiBaseUrl}/admin/clientes`, payload);
  }
  actualizar(id: number, payload: GuardarCliente): Observable<ClienteAdmin> {
    return this.http.patch<ClienteAdmin>(`${environment.apiBaseUrl}/admin/clientes/${id}`, payload);
  }
}
