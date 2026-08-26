import { Injectable, inject } from '@angular/core';
import { EMPTY, catchError, finalize, tap } from 'rxjs';
import { RecepcionDashboardStore } from '../state/recepcion-dashboard.store';
import { RecepcionDashboardFacade } from './recepcion-dashboard.facade';

@Injectable({ providedIn: 'root' })
export class RecepcionDashboardCoordinator {
  private readonly facade = inject(RecepcionDashboardFacade);
  private readonly store = inject(RecepcionDashboardStore);

  load(fecha: string, sucursalId?: number | null) {
    this.prepareLoad();
    return this.facade.cargarConCatalogo(fecha, sucursalId).pipe(
      tap(({ catalogo, agenda, espera }) => {
        this.store.applyCatalog(catalogo);
        this.store.applySnapshot({ agenda, espera });
      }),
      catchError(error => this.fail(error, 'No pude cargar la agenda de recepción.')),
      finalize(() => this.store.setLoading(false))
    );
  }

  refresh(fecha: string, sucursalId?: number | null) {
    this.prepareLoad();
    return this.facade.cargarSnapshot(fecha, sucursalId).pipe(
      tap(snapshot => this.store.applySnapshot(snapshot)),
      catchError(error => this.fail(error, 'No pude actualizar la agenda.')),
      finalize(() => this.store.setLoading(false))
    );
  }

  private prepareLoad(): void {
    this.store.setLoading(true);
    this.store.setError('');
  }

  private fail(error: unknown, fallback: string) {
    this.store.setError(extractRecepcionHttpMessage(error, fallback));
    return EMPTY;
  }
}

export function extractRecepcionHttpMessage(error: unknown, fallback: string): string {
  const httpError = error as { error?: { message?: string }; message?: string };
  return httpError?.error?.message || httpError?.message || fallback;
}
