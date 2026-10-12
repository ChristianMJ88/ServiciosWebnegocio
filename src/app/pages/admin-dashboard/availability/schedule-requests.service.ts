import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
export interface ScheduleRequest {
 id:number; prestadorNombre:string; estado:string;
 propuesta:{reemplazar:boolean; diasSemana:number[]; horario:{horaInicio:string;horaFin:string;vigenteDesde:string|null;vigenteHasta:string|null;intervaloMinutos:number}};
}
@Injectable({providedIn:'root'})
export class ScheduleRequestsService {
 private http=inject(HttpClient);
 private base=environment.apiBaseUrl+'/admin/disponibilidad';
 metadata(){return this.http.get<{diasSemana:{valor:number;etiqueta:string}[]}>(this.base+'/metadatos');}
 pending(){return this.http.get<ScheduleRequest[]>(this.base+'/solicitudes-horario');}
 resolve(id:number,aprobar:boolean,contrasena:string){return this.http.post<void>(this.base+'/solicitudes-horario/'+id+'/resolver',{aprobar,contrasena});}
}
