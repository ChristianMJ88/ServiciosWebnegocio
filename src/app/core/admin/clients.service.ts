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
export interface PaginaClientesAdmin {
  contenido: ClienteAdmin[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

@Injectable({ providedIn: 'root' })
export class ClientsService {
  private readonly http = inject(HttpClient);
  listar(busqueda = '', pagina = 0, tamano = 25): Observable<PaginaClientesAdmin> {
    let params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    if (busqueda.trim()) params = params.set('busqueda', busqueda.trim());
    return this.http.get<PaginaClientesAdmin>(`${environment.apiBaseUrl}/admin/clientes`, { params });
  }
  crear(payload: GuardarCliente): Observable<ClienteAdmin> {
    return this.http.post<ClienteAdmin>(`${environment.apiBaseUrl}/admin/clientes`, payload);
  }
  actualizar(id: number, payload: GuardarCliente): Observable<ClienteAdmin> {
    return this.http.patch<ClienteAdmin>(`${environment.apiBaseUrl}/admin/clientes/${id}`, payload);
  }
}
