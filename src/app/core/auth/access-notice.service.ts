import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AccessNoticeService {
  readonly mensaje = signal<string | null>(null);
  mostrar(mensaje: string) { this.mensaje.set(mensaje); }
  limpiar() { this.mensaje.set(null); }
}
