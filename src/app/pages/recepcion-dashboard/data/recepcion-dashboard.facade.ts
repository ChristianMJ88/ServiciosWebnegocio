import { Injectable, inject } from '@angular/core';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import {
  CatalogoRecepcion,
  CitaRecepcion,
  RecepcionService,
  SolicitudEsperaRecepcion
} from '../../../core/recepcion/recepcion.service';

export interface RecepcionDashboardSnapshot {
  agenda: CitaRecepcion[];
  espera: SolicitudEsperaRecepcion[];
}

export interface RecepcionDashboardInitialData extends RecepcionDashboardSnapshot {
  catalogo: CatalogoRecepcion;
}

@Injectable({ providedIn: 'root' })
export class RecepcionDashboardFacade {
  private readonly recepcionService = inject(RecepcionService);

  cargarConCatalogo(fecha: string, sucursalId?: number | null) {
    return this.recepcionService.getCatalogo(sucursalId).pipe(
      switchMap(catalogo => {
        const sucursalOperativaId = catalogo.sucursalActivaId ?? sucursalId;
        return this.cargarSnapshot(fecha, sucursalOperativaId).pipe(
          map(snapshot => ({ catalogo, ...snapshot }))
        );
      })
    );
  }

  cargarSnapshot(fecha: string, sucursalId?: number | null) {
    return forkJoin({
      agenda: this.recepcionService.getAgenda(fecha, sucursalId),
      espera: this.recepcionService.getSolicitudesEspera(fecha, sucursalId)
        .pipe(catchError(() => of([] as SolicitudEsperaRecepcion[])))
    });
  }
}
