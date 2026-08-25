import { Injectable, inject } from '@angular/core';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { CajaService, CatalogoCaja } from '../../../core/caja/caja.service';

const catalogoVacio = (sucursalActivaId?: number | null): CatalogoCaja => ({
  sucursalActivaId: sucursalActivaId ?? null,
  sucursales: [],
  metodosPago: [],
  tiposMovimiento: [],
  estadoSesionAbierta: '',
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

  abrirCaja(payload: Parameters<CajaService['abrirCaja']>[0]) {
    return this.cajaService.abrirCaja(payload);
  }

  cerrarCaja(id: number, payload: Parameters<CajaService['cerrarCaja']>[1]) {
    return this.cajaService.cerrarCaja(id, payload);
  }

  listarPagos(citaId: number) {
    return this.cajaService.listarPagosCita(citaId);
  }

  registrarPago(citaId: number, payload: Parameters<CajaService['registrarPago']>[1]) {
    return this.cajaService.registrarPago(citaId, payload);
  }

  registrarMovimiento(payload: Parameters<CajaService['registrarMovimiento']>[0]) {
    return this.cajaService.registrarMovimiento(payload);
  }
}
