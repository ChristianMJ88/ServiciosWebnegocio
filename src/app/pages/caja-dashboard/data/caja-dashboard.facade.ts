import { Injectable, inject } from '@angular/core';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { CajaService, CatalogoCaja } from '../../../core/caja/caja.service';

const catalogoVacio = (sucursalActivaId?: number | null): CatalogoCaja => ({
  sucursalActivaId: sucursalActivaId ?? null,
  sucursales: [],
  metodosPago: [],
  tiposMovimiento: [],
  estadosSesion: [],
  estadoSesionAbierta: '',
  estadoSesionCerrada: '',
  metodoPagoEfectivo: ''
});

@Injectable({ providedIn: 'root' })
export class CajaDashboardFacade {
  private readonly cajaService = inject(CajaService);

  cargarConCatalogo(sucursalId?: number | null) {
    return this.cajaService.getCatalogo(sucursalId).pipe(
      catchError(() => of(catalogoVacio(sucursalId))),
      switchMap(catalogo => this.cargarTablero(catalogo.sucursalActivaId ?? sucursalId).pipe(
        map(tablero => ({ catalogo, tablero }))
      ))
    );
  }

  cargarTablero(sucursalId?: number | null) {
    return forkJoin({
      sesion: this.cajaService.getSesionActual(sucursalId),
      citas: this.cajaService.getCitasPorCobrar(sucursalId),
      resumen: this.cajaService.getResumen(sucursalId),
      movimientos: this.cajaService.listarMovimientos(sucursalId)
    });
  }

}
