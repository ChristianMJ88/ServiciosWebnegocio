import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
export interface DirectoryService {id:number;nombre:string;categoria:string|null;precio:number;moneda:string;duracionMinutos:number;}
export interface DirectoryFilters {ubicacion:string;categoria:string;}
export interface DirectoryBranch {id:number;nombre:string;direccion:string|null;municipio:string|null;estadoRegion:string|null;paisCodigo:string|null;codigoPostal:string|null;zonaHoraria:string;horariosComerciales:{dia:number;apertura:string;cierre:string}[];}
export interface DirectoryBusiness { empresaId:number; slug:string; nombre:string; descripcion:string|null; imagenUrl:string|null; direccion:string|null; telefono:string|null; portadaUrl?:string|null;servicios?:DirectoryService[];sucursales?:DirectoryBranch[]; }
export interface DirectoryPage { negocios:DirectoryBusiness[]; siguienteCursor:number|null; }
export interface DirectoryParticipation { participante:boolean; disponible:boolean; }
@Injectable({providedIn:'root'})
export class MarketplaceService {
 private readonly http=inject(HttpClient);
 buscar(q:string,cursor=0,filters:DirectoryFilters={ubicacion:'',categoria:''}){return this.http.get<DirectoryPage>(environment.apiBaseUrl+'/publico/marketplace',{params:new HttpParams().set('q',q).set('cursor',cursor).set('ubicacion',filters.ubicacion).set('categoria',filters.categoria)});}
 participacion(){return this.http.get<DirectoryParticipation>(environment.apiBaseUrl+'/admin/configuracion-sitio/marketplace');}
 guardar(participante:boolean){return this.http.put<DirectoryParticipation>(environment.apiBaseUrl+'/admin/configuracion-sitio/marketplace',{participante});}
}
