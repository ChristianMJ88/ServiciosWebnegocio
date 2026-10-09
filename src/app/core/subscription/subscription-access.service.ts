import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { finalize, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SubscriptionAccessNotice {
  bloqueado: boolean;
  advertencia: boolean;
  titulo: string | null;
  mensaje: string | null;
  graciaHasta: string | null;
  puedeRegularizar: boolean;
}

@Injectable({ providedIn: 'root' })
export class SubscriptionAccessService {
  private readonly http = inject(HttpClient);
  private loading = false;
  readonly notice = signal<SubscriptionAccessNotice | null>(null);
  refresh(): void {
    if (this.loading) return;
    this.loading = true;
    this.http.get<SubscriptionAccessNotice>(`${environment.apiBaseUrl}/admin/suscripcion/acceso`)
      .pipe(tap(value => this.notice.set(value)), finalize(() => this.loading = false))
      .subscribe({ error: () => {} });
  }
  clear(): void { this.notice.set(null); }
}
