import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
@Injectable({providedIn:'root'})
export class PasswordRecoveryService {
  private readonly http=inject(HttpClient);
  request(correo:string){return this.http.post<{mensaje:string}>(`${environment.apiBaseUrl}/publico/recuperacion-contrasena/solicitar`,{correo});}
  confirm(token:string,contrasena:string){return this.http.post<{mensaje:string}>(`${environment.apiBaseUrl}/publico/recuperacion-contrasena/confirmar`,{token,contrasena});}
}
