import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { BreakpointObserver } from '@angular/cdk/layout';
import { of, Subject } from 'rxjs';
import { BookingComponent } from './booking.component';
import { BookingDataService, ServicioCatalogo } from '../../services/booking-data.service';
import { AppointmentService, FranjaDisponible } from '../../services/appointment.service';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { ReservationCartService } from '../../services/reservation-cart.service';

const service = (id: number, branch = 10, moneda = 'MXN'): ServicioCatalogo => ({
  id, sucursalId: branch, nombre: `Servicio ${id}`, slug: `servicio-${id}`, descripcion: '', moneda,
  precio: 125.5, duracionMinutos: 30, bufferAntesMinutos: 0, bufferDespuesMinutos: 0,
  grupoId: null, grupoNombre: null, subgrupoId: null, subgrupoNombre: null, imagenUrl: null,
  ordenPublico: id, visiblePublico: true, requiereAnticipo: false, anticipoTipo: null, anticipoValor: null
});

describe('Public booking selection', () => {
  beforeEach(() => new ReservationCartService().clear(1));
  function setup(params: Record<string, string> = {}) {
    const slots = new Subject<FranjaDisponible[]>();
    TestBed.configureTestingModule({ providers: [
      { provide: TenantContextService, useValue: { empresaId: () => 1 } },
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(params) } } },
      { provide: BreakpointObserver, useValue: { isMatched: () => true, observe: () => of({ matches: true }) } },
      { provide: BookingDataService, useValue: {
        getBranches: () => of([{ id: 10 }, { id: 20 }]),
        getServices: (branch: number) => of(branch === 10 ? [service(1), service(2), service(3, 10, 'USD')] : [service(4, 20)])
      } },
      { provide: AppointmentService, useValue: { getPublicStaff: () => of([]), getAvailableSlots: () => slots } }
    ] });
    const component = TestBed.runInInjectionContext(() => new BookingComponent());
    return { component, slots, cart: TestBed.inject(ReservationCartService) };
  }

  it('hydrates the requested branch in selection order, excluding unknown services and other currencies', () => {
    const { component } = setup({ sucursal: '10', servicios: '2,1,99,3' });
    component.ngOnInit();
    expect(component.bookingForm.value.branchId).toBe(10);
    expect(component.selectedServices().map(item => item.id)).toEqual([2, 1]);
    expect(component.selectionNotice).toContain('Revisa');
  });

  it('restores a shared draft and clears it when the branch changes without replaying URL services', () => {
    const { component, cart } = setup();
    cart.save(1, 10, [service(2)]);
    component.ngOnInit();
    expect(component.selectedServices().map(item => item.id)).toEqual([2]);
    component.onBranchChange('20');
    expect(component.selectedServices()).toEqual([]);
    expect(cart.read(1)).toEqual({ branchId: 20, serviceIds: [] });
    expect(component.selectionNotice).toContain('Cambiaste de sucursal');
  });

  it('invalidates availability and the required service field when the final item is removed', () => {
    const { component, slots, cart } = setup({ sucursal: '10', servicios: '1' });
    component.ngOnInit();
    component.onDateSelect('2026-09-17');
    component.removeSelectedService(1);
    slots.next([{ inicio: '2026-09-17T10:00:00-06:00', fin: '2026-09-17T10:30:00-06:00', hora: '10:00', prestadorId: 1 } as FranjaDisponible]);
    slots.complete();
    expect(component.selectedDate()).toBeNull();
    expect(component.availableItineraries()).toEqual([]);
    expect(component.bookingForm.controls['serviceId'].invalid).toBe(true);
    expect(cart.read(1)?.serviceIds).toEqual([]);
  });
});
