import { Injectable, inject } from '@angular/core';
import { EMPTY, catchError, finalize, map, tap } from 'rxjs';
import { CatalogoCaja } from '../../../core/caja/caja.service';
import { CajaDashboardFacade } from './caja-dashboard.facade';
import { CajaDashboardSnapshot, CajaDashboardStore } from '../state/caja-dashboard.store';

export interface CajaDashboardLoadResult {
  catalogo: CatalogoCaja | null;
  snapshot: CajaDashboardSnapshot;
}

@Injectable({ providedIn: 'root' })
export class CajaDashboardCoordinator {
  private readonly facade = inject(CajaDashboardFacade);
  private readonly store = inject(CajaDashboardStore);

  load(branchId?: number | null) {
    this.prepareLoad();
    return this.facade.cargarConCatalogo(branchId).pipe(
      map(({ catalogo, tablero }) => ({ catalogo, snapshot: tablero })),
      tap(({ catalogo, snapshot }) => {
        this.store.applyCatalog(catalogo);
        this.store.applyDashboard(snapshot);
      }),
      catchError(error => this.fail(error, 'No pude cargar las sucursales para Caja.')),
      finalize(() => this.store.setLoading(false))
    );
  }

  refresh(branchId?: number | null) {
    this.prepareLoad();
    return this.facade.cargarTablero(branchId).pipe(
      map(snapshot => ({ catalogo: null, snapshot } satisfies CajaDashboardLoadResult)),
      tap(({ snapshot }) => this.store.applyDashboard(snapshot)),
      catchError(error => this.fail(error, 'No pude actualizar el tablero de Caja.')),
      finalize(() => this.store.setLoading(false))
    );
  }

  private prepareLoad(): void {
    this.store.setLoading(true);
    this.store.setError('');
  }

  private fail(error: unknown, fallback: string) {
    this.store.setError(extractHttpMessage(error, fallback));
    return EMPTY;
  }
}

export function extractHttpMessage(error: unknown, fallback: string): string {
  const httpError = error as { error?: { message?: string }; message?: string };
  return httpError?.error?.message || httpError?.message || fallback;
}
