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
  ultimaVisita?: string | null;
  citasAgendadas?: number;
}
export interface FiltroClientesAdmin { nombre?: string; telefono?: string; visitaDesde?: string; visitaHasta?: string; citas?: string; orden?: string; }
export interface MetadatosClientesAdmin { ordenes: {valor:string;etiqueta:string}[]; citas: {valor:string;etiqueta:string}[]; ordenPredeterminado:string; citasPredeterminado:string; }
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
  listar(busqueda = '', pagina = 0, tamano?: number, filtros: FiltroClientesAdmin = {}): Observable<PaginaClientesAdmin> {
    let params = new HttpParams().set('pagina', pagina);
    if (tamano !== undefined) params = params.set('tamano', tamano);
    if (busqueda.trim()) params = params.set('busqueda', busqueda.trim());
    for (const [clave, valor] of Object.entries(filtros)) { if (valor?.trim()) params = params.set(clave, valor.trim()); }
    return this.http.get<PaginaClientesAdmin>(`${environment.apiBaseUrl}/admin/clientes`, { params });
  }
  metadatos(): Observable<MetadatosClientesAdmin> {
    return this.http.get<MetadatosClientesAdmin>(`${environment.apiBaseUrl}/admin/clientes/metadatos`);
  }
  crear(payload: GuardarCliente): Observable<ClienteAdmin> {
    return this.http.post<ClienteAdmin>(`${environment.apiBaseUrl}/admin/clientes`, payload);
  }
  actualizar(id: number, payload: GuardarCliente): Observable<ClienteAdmin> {
    return this.http.patch<ClienteAdmin>(`${environment.apiBaseUrl}/admin/clientes/${id}`, payload);
  }
}
