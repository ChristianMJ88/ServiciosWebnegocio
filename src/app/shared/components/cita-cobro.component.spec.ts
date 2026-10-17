import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, it, expect } from 'vitest';
import { CitaCobroComponent } from './cita-cobro.component';
import { DatosCobroCita } from '../../core/caja/cobro-cita.models';

describe('Datos y autorización de cobro del backend', () => {
  function render(datos: DatosCobroCita) {
    TestBed.configureTestingModule({imports:[CitaCobroComponent],providers:[provideRouter([])]});
    const fixture=TestBed.createComponent(CitaCobroComponent);
    fixture.componentRef.setInput('datos',datos);fixture.componentRef.setInput('citaId',27);fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }
  it('enlaza la cita y sucursal autorizadas con el saldo exacto recibido', () => {
    const vista=render({estadoCobro:'PARCIAL',pagado:25,pendiente:75,moneda:'MXN',sucursalId:12,puedeCobrar:true});
    expect(vista.textContent).toContain('Pago parcial');
    const enlace=vista.querySelector('a')!;
    expect(enlace.textContent).toContain('$75.00');
    expect(enlace.getAttribute('href')).toBe('/caja?cita=27&sucursal=12');
    expect(vista.textContent).toContain('$25.00');
  });
  it('informa el saldo sin ofrecer cobrar cuando el backend no lo autoriza', () => {
    const vista=render({estadoCobro:'PENDIENTE',pendiente:75,moneda:'MXN',puedeCobrar:false});
    expect(vista.querySelector('a')).toBeNull();expect(vista.textContent).toContain('Queda saldo pendiente en Caja');
  });
  it('respeta el estado pagada o no cobrable y no deduce deuda de otros campos', () => {
    const vista=render({estadoCobro:'PAGADA',pagado:100,pendiente:999,moneda:'MXN',puedeCobrar:false});
    expect(vista.textContent).toContain('Pagada');expect(vista.textContent).not.toContain('999');expect(vista.querySelector('a')).toBeNull();
  });
});
