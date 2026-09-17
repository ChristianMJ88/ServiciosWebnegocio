import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { ServicesComponent } from './services.component';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { BookingDataService, ServicioCatalogo } from '../../services/booking-data.service';
import { AppointmentService } from '../../services/appointment.service';
import { ReservationCartService } from '../../services/reservation-cart.service';

const service = (id: number, branch: number, moneda = 'MXN') => ({ id, sucursalId: branch, moneda, precio: 125.5, nombre: `Servicio ${id}` }) as ServicioCatalogo;

describe('Services cart', () => {
  it('requires the chosen branch and rejects mixed currencies without changing the selection', () => {
    TestBed.configureTestingModule({ providers: [
      { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
      { provide: BookingDataService, useValue: {} },
      { provide: AppointmentService, useValue: { getPublicStaff: () => of([]) } },
      { provide: TenantContextService, useValue: { empresaId: () => 123, routeFor: () => '/e/test/agendar' } }
    ] });
    const page = TestBed.runInInjectionContext(() => new ServicesComponent());
    const cart = TestBed.inject(ReservationCartService);
    cart.clear(123);
    page.agregarServicio(service(1, 10));
    expect(page.serviciosSeleccionados()).toEqual([]);
    page.branchId.set(10);
    page.agregarServicio(service(1, 10));
    page.agregarServicio(service(2, 20));
    page.agregarServicio(service(3, 10, 'USD'));
    expect(page.serviciosSeleccionados().map(item => item.id)).toEqual([1]);
    expect(page.notice()).toContain('misma moneda');
    page.agregarServicio(service(4, 10));
    expect(page.bookingQueryParams()).toEqual({ sucursal: '10', servicios: '1,4' });
    expect(cart.read(123)?.serviceIds).toEqual([1, 4]);
    page.quitarServicio(1);
    expect(cart.read(123)?.serviceIds).toEqual([4]);
    cart.clear(123);
  });
});
