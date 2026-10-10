import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
export interface DirectoryBusiness { empresaId:number; slug:string; nombre:string; descripcion:string|null; imagenUrl:string|null; direccion:string|null; telefono:string|null; }
export interface DirectoryPage { negocios:DirectoryBusiness[]; siguienteCursor:number|null; }
export interface DirectoryParticipation { participante:boolean; disponible:boolean; }
@Injectable({providedIn:'root'})
export class MarketplaceService {
 private readonly http=inject(HttpClient);
 buscar(q:string,cursor=0){return this.http.get<DirectoryPage>(environment.apiBaseUrl+'/publico/marketplace',{params:new HttpParams().set('q',q).set('cursor',cursor)});}
 participacion(){return this.http.get<DirectoryParticipation>(environment.apiBaseUrl+'/admin/configuracion-sitio/marketplace');}
 guardar(participante:boolean){return this.http.put<DirectoryParticipation>(environment.apiBaseUrl+'/admin/configuracion-sitio/marketplace',{participante});}
}
