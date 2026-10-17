import { DatePipe, formatDate, registerLocaleData } from '@angular/common';
import localeEsMx from '@angular/common/locales/es-MX';
import { TestBed } from '@angular/core/testing';
import { Type } from '@angular/core';
import { describe, beforeEach, expect, it } from 'vitest';
import { of } from 'rxjs';
import { CajaService, CitaPorCobrar, PagoCita } from '../../core/caja/caja.service';
import { CajaSessionSectionComponent } from './session/caja-session-section.component';
import { CajaMovementsSectionComponent } from './movements/caja-movements-section.component';
import { CajaPaymentsSectionComponent } from './payments/caja-payments-section.component';
import { CajaReceiptComponent } from './receipt/caja-receipt.component';
import { construirComprobanteCaja } from './receipt/caja-receipt.builder';
import { crearFormularioMovimiento, crearFormularioPago } from './forms/caja.forms';
import { RecepcionAgendaSectionComponent } from '../recepcion-dashboard/agenda/recepcion-agenda-section.component';
import { RecepcionWaitlistComponent } from '../recepcion-dashboard/waitlist/recepcion-waitlist.component';

describe('Momentos UTC en Caja y Recepción', () => {
  const momento = '2026-10-15T14:00:00Z';
  const cita = { citaId: 1, clienteNombre: 'Cliente', servicioNombre: 'Servicio', sucursalNombre: 'Sucursal',
    inicio: '2026-10-15T10:00:00', moneda: 'MXN', total: 100, pagado: 100, pendiente: 0 } as CitaPorCobrar;
  const pago = { id: 1, registradoEn: momento, monto: 100, metodoPago: 'TARJETA' } as PagoCita;
  const local = (formato = 'dd/MM HH:mm') => formatDate(new Date(momento), formato, 'es-MX');

  beforeEach(() => registerLocaleData(localeEsMx));

  function render<T>(tipo: Type<T>, entradas: object): HTMLElement {
    TestBed.configureTestingModule({ imports: [tipo], providers: [
      { provide: CajaService, useValue: { listarTerminalesPoint: () => of([]) } }
    ] });
    const fixture = TestBed.createComponent(tipo);
    Object.assign(fixture.componentInstance as object, entradas);
    fixture.detectChanges();
    return fixture.nativeElement;
  }

  it('interpreta el offset sin sumar ni restar manualmente horas', () => {
    const pipe = new DatePipe('es-MX');
    expect(pipe.transform(momento, 'dd/MM HH:mm', '-0600')).toBe('15/10 08:00');
    expect(pipe.transform(momento, 'dd/MM HH:mm', '+0200')).toBe('15/10 16:00');
    expect(pipe.transform('2026-10-15T01:00:00Z', 'dd/MM HH:mm', '-0600')).toBe('14/10 19:00');
  });

  it('muestra la apertura en la hora del navegador', () => {
    const vista = render(CajaSessionSectionComponent, { cajaAbierta: true, puedeGestionar: false,
      sesion: { abiertaEn: momento, sucursalNombre: 'Sucursal', montoInicial: 0 }, resumen: null });
    expect(vista.querySelector('.caja-session-summary')?.textContent).toContain(local('dd/MM/yyyy HH:mm'));
  });

  it('muestra la fecha del movimiento sin perder el offset', () => {
    const vista = render(CajaMovementsSectionComponent, { puedeGestionar: true, cajaAbierta: true, guardando: false,
      formulario: crearFormularioMovimiento(), tiposMovimiento: [], metodosPago: [], estadosSesion: [], estadoSesion: '',
      movimientos: [{ id: 1, concepto: 'Ingreso', monto: 10, registradoEn: momento }], ultimoMovimiento: null, resumen: null });
    expect(vista.querySelector('.caja-history-amount small')?.textContent).toContain(local());
  });

  it('muestra el pago recibido del backend en hora local', () => {
    const vista = render(CajaPaymentsSectionComponent, { sucursalId: null, citas: [cita], citaSeleccionada: cita,
      citaSeleccionadaId: 1, formularioPago: crearFormularioPago(), metodosPago: [], metodoPagoEfectivo: '',
      pagos: [pago], guardando: false, cajaAbierta: true, mensajeBloqueo: '' });
    expect(vista.querySelector('.caja-history-item small')?.textContent).toContain(local());
  });

  it('conserva la hora de agenda y convierte únicamente el momento del pago en el comprobante', () => {
    const recibo = construirComprobanteCaja('Negocio', cita, [pago], []);
    const vista = render(CajaReceiptComponent, { receipt: recibo });
    expect(recibo.pagos[0].fecha).toBe(momento);
    expect(vista.querySelector('tbody td')?.textContent).toContain(local('dd/MM/yyyy HH:mm'));
    expect(vista.querySelector('.caja-receipt-grid')?.textContent).toContain('15/10/2026 10:00');
  });

  it('muestra cuándo se registró la llegada sin modificar la cita', () => {
    const vista = render(RecepcionAgendaSectionComponent, { citas: [{ ...cita, id: 1, estado: 'CONFIRMADA',
      seccionAgenda: 'ACTIVA', checkInEn: momento }], guardando: false, puedeGestionarCitas: false, puedeRegistrarCheckIn: false,
      estadosCita: [], estadoPendiente: '', estadosFinalizables: [], estadosCancelables: [] });
    expect(vista.querySelector('.recepcion-checkin-time')?.textContent).toContain(local());
    expect(vista.querySelector('.recepcion-appointment-meta')?.textContent).toContain('15/10/2026 10:00');
  });

  it('convierte registro y notificación de espera, conservando la fecha deseada', () => {
    const vista = render(RecepcionWaitlistComponent, { solicitudes: [{ id: 1, nombreCliente: 'Cliente',
      fechaDeseada: '2026-10-15', creadaEn: momento, notificadaEn: momento, estado: 'AVISADO', aceptaWhatsapp: true }],
      estados: [], estadoPendiente: 'PENDIENTE', estadoNotificada: 'AVISADO', guardando: false });
    expect(vista.querySelector('.recepcion-waitlist-created')?.textContent).toContain(local());
    expect(vista.querySelector('.recepcion-waitlist-hint')?.textContent).toContain(local());
    expect(vista.querySelector('.recepcion-appointment-meta')?.textContent).toContain('15/10/2026');
  });
});
