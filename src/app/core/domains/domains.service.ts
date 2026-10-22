import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
export interface DnsRecord { tipo: string; nombre: string; valor: string; }
export interface DomainView { id: number; hostname: string; estado: string; version: number; raiz: boolean; registros: DnsRecord[]; aviso: string; ultimoError: string | null; verificadoEn: string | null; activadoEn: string | null; ultimoChequeoEn: string | null; acciones: string[]; }
export interface DomainPanel { incluido: boolean; puedeAgregar: boolean; limite: number | null; uso: number; dominios: DomainView[]; }
export interface PlatformDomain { empresaId: number; dominio: DomainView; excedeLimite: boolean; pendienteRetiroHosting: boolean; }
export interface DomainPage { content: PlatformDomain[]; page: { size: number; number: number; totalElements: number; totalPages: number }; }
@Injectable({ providedIn: 'root' })
export class DomainsService {
 private readonly http=inject(HttpClient);
 private readonly base=environment.apiBaseUrl;
 panel(){return this.http.get<DomainPanel>(`${this.base}/admin/dominios`);}
 request(hostname: string){return this.http.post<DomainView>(`${this.base}/admin/dominios`,{hostname});}
 verify(d: DomainView,platform=false){return this.http.post<DomainView>(`${this.base}/${platform?'plataforma':'admin'}/dominios/${d.id}/verificar`,{version:d.version});}
 retire(d: DomainView,platform=false){return this.http.post<DomainView>(`${this.base}/${platform?'plataforma':'admin'}/dominios/${d.id}/retirar`,{version:d.version});}
 queue(estado: string,pagina: number){return this.http.get<DomainPage>(`${this.base}/plataforma/dominios`,{params:{pagina,tamano:20,...(estado?{estado}:{})}});}
 connect(d: DomainView,registros: DnsRecord[]){return this.http.put<DomainView>(`${this.base}/plataforma/dominios/${d.id}/conexion`,{version:d.version,registros});}
}
export function domainStateLabel(state: string): string { return ({PENDIENTE_VERIFICACION:'Verifica tu dominio',VERIFICADO:'En preparación por Fluora',CONECTANDO:'Configura la conexión',ACTIVO:'Activo',ERROR:'Revisar conexión',RETIRADO:'Retirado'} as Record<string,string>)[state]??state; }

export function domainErrorLabel(code: string): string { return ({TXT_NO_ENCONTRADO:'Aún no vemos el registro TXT; la propagación puede tardar hasta 48 h.', HOST_EN_USO:'Este dominio ya está vinculado. Contacta a Fluora para revisarlo.', COMPROBACION_NO_DISPONIBLE:'No fue posible comprobarlo ahora. Inténtalo más tarde.', HOSTING_NO_DISPONIBLE:'La conexión aún no está lista. Revisa los registros y vuelve a intentar.', LIBERADO_POR_OTRA_CUENTA:'Otra cuenta comprobó la propiedad de este dominio.', DOMINIO_LEGADO_INVALIDO:'El dominio anterior no tenía un formato válido. Crea una nueva solicitud.'} as Record<string,string>)[code]??'Revisa el estado de la solicitud e inténtalo de nuevo.'; }
