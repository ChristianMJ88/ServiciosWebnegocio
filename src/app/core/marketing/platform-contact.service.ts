import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, of, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';
export interface PublicSupport {
  correo: string; telefono: string; dias: number[]; horaInicio: string; horaFin: string; zonaHoraria: string;
}
export interface PlatformContact extends PublicSupport {
  correoLegal: string; version: number | null; visible: boolean; actualizadoEn?: string; actualizadoPor?: number | null;
}
@Injectable({providedIn: 'root'})
export class PlatformContactService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;
  // Shared within a page, requested only by eligible Fluora consumers. HTTP cache expires in 60s.
  private supportExpires = 0;
  private supportRequest?: ReturnType<PlatformContactService['requestSupport']>;
  private requestSupport() {
    return this.http.get<PublicSupport>(this.base + '/publico/plataforma/soporte')
      .pipe(catchError(() => of(null)), shareReplay({bufferSize: 1, refCount: true}));
  }
  publicSupport() {
    if(!this.supportRequest || Date.now() >= this.supportExpires) {
      this.supportRequest = this.requestSupport(); this.supportExpires = Date.now() + 60_000;
    }
    return this.supportRequest;
  }
  legalContact() { return this.http.get<{correo: string}>(this.base + '/publico/plataforma/contacto-legal'); }
  get() { return this.http.get<PlatformContact>(this.base + '/plataforma/marketing/soporte'); }
  save(value: PlatformContact) {
    const {version, correo, correoLegal, telefono, dias, horaInicio, horaFin, zonaHoraria, visible} = value;
    return this.http.put<PlatformContact>(this.base + '/plataforma/marketing/soporte',
      {version, correo, correoLegal, telefono, dias, horaInicio, horaFin, zonaHoraria, visible});
  }
}
