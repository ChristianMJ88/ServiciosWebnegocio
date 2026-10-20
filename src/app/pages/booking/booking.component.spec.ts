import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { BreakpointObserver } from '@angular/cdk/layout';
import { of, Subject } from 'rxjs';
import { BookingComponent } from './booking.component';
import { BookingDataService, ServicioCatalogo, SucursalCatalogo } from '../../services/booking-data.service';
import { AppointmentService, FranjaDisponible, CotizacionCitas, PrestadorPublico } from '../../services/appointment.service';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { ReservationCartService } from '../../services/reservation-cart.service';
import { PublicCheckoutService } from '../../services/public-checkout.service';
import { vi } from 'vitest';

const service = (id: number, branch = 10, moneda = 'MXN'): ServicioCatalogo => ({
  id, sucursalId: branch, nombre: `Servicio ${id}`, slug: `servicio-${id}`, descripcion: '', moneda,
  precio: 125.5, duracionMinutos: 30, bufferAntesMinutos: 0, bufferDespuesMinutos: 0,
  grupoId: null, grupoNombre: null, subgrupoId: null, subgrupoNombre: null, imagenUrl: null,
  ordenPublico: id, visiblePublico: true, requiereAnticipo: false, anticipoTipo: null, anticipoValor: null
});

describe('Public booking selection', () => {
  function setupPendingSelection() {
    const context = setup({sucursal:'10',servicios:'1'});
    const quote = new Subject<CotizacionCitas>(), staff = new Subject<PrestadorPublico[]>();
    context.cotizar.mockReturnValue(quote);
    const getStaff = vi.spyOn(TestBed.inject(AppointmentService), 'getPublicStaff').mockReturnValue(staff);
    context.component.ngOnInit();
    return {...context,quote,staff,getStaff};
  }
  it.each(['quote', 'staff'])('waits for both initial responses even when %s arrives first', first => {
    const {component,quote,staff,serverQuote,getStaff,dias} = setupPendingSelection();
    expect(component.catalogoInicialListo()).toBe(true);
    expect(component.presentacionInicialLista()).toBe(false); // Antes del effect de cotización.
    TestBed.tick();
    // El fixture no tiene días: el flujo existente consulta también el mes siguiente.
    expect(dias).toHaveBeenCalledTimes(2);
    expect(getStaff).toHaveBeenCalledExactlyOnceWith(1,10,1);
    if (first === 'quote') { quote.next(serverQuote); quote.complete(); }
    else { staff.next([]); staff.complete(); }
    expect(component.presentacionInicialLista()).toBe(false);
    if (first === 'quote') { staff.next([]); staff.complete(); }
    else { quote.next(serverQuote); quote.complete(); }
    expect(component.presentacionInicialLista()).toBe(true);
  });
  it('reveals errors without retaining an initial quote or staff wait', () => {
    const {component,quote,staff} = setupPendingSelection(); TestBed.tick();
    staff.error(new Error('Staff unavailable'));
    quote.error({error:{mensaje:'Cotización no disponible'}});
    expect(component.presentacionInicialLista()).toBe(true);
    expect(component.errorStaff()).not.toBe('');
    expect(component.errorCotizacion()).toBe('Cotización no disponible');
  });
  it('does not release the selection barrier for an obsolete quote', () => {
    const {component,quote,staff,cotizar,serverQuote} = setupPendingSelection(); TestBed.tick();
    staff.next([]); staff.complete();
    const currentQuote = new Subject<CotizacionCitas>(); cotizar.mockReturnValue(currentQuote);
    component.selectedDate.set('2026-10-18'); TestBed.tick();
    quote.next(serverQuote); quote.complete();
    expect(component.presentacionInicialLista()).toBe(false);
    currentQuote.next(serverQuote); currentQuote.complete();
    expect(component.presentacionInicialLista()).toBe(true);
  });
  it('releases a pending selection when a branch change removes its initial services', () => {
    const {component,staff} = setupPendingSelection(); TestBed.tick();
    staff.next([]); staff.complete();
    component.onBranchChange('20'); TestBed.tick();
    expect(component.presentacionInicialLista()).toBe(true);
  });
  it('ignores obsolete staff after changing branch and releases its initial wait on completion', () => {
    const {component,staff,quote,serverQuote} = setupPendingSelection(); TestBed.tick();
    component.onBranchChange('20'); TestBed.tick();
    expect(component.presentacionInicialLista()).toBe(false);
    quote.next(serverQuote); quote.complete();
    expect(component.cotizacion()).toBeNull();
    staff.next([{usuarioId:99,sucursalId:10,nombreMostrar:'Profesional anterior',biografia:null,fotoUrl:null,colorAgenda:null}]);
    expect(component.staffByService()).toEqual({});
    expect(component.presentacionInicialLista()).toBe(false);
    staff.complete();
    expect(component.presentacionInicialLista()).toBe(true);
    expect(component.staffByService()).toEqual({});
    expect(component.selectedServices()).toEqual([]);
  });
  it('does not hide again for later staff and price requests', () => {
    const {component,quote,staff,serverQuote,cotizar,getStaff} = setupPendingSelection(); TestBed.tick();
    staff.next([]); staff.complete(); quote.next(serverQuote); quote.complete();
    expect(component.presentacionInicialLista()).toBe(true);
    getStaff.mockReturnValue(new Subject<PrestadorPublico[]>());
    cotizar.mockReturnValue(new Subject<CotizacionCitas>());
    component.toggleService(service(2)); TestBed.tick();
    expect(component.cotizando()).toBe(true);
    expect(component.presentacionInicialLista()).toBe(true);
  });
  const branch = (id: number): SucursalCatalogo => ({ id, empresaId: 1, nombre: `Sucursal ${id}`, direccion: '', telefono: '', zonaHoraria: 'America/Mexico_City' });
  function setupPendingCatalog(params: Record<string, string> = {}) {
    const context = setup(params), branches = new Subject<SucursalCatalogo[]>(), services = new Subject<ServicioCatalogo[]>();
    const data = TestBed.inject(BookingDataService);
    vi.spyOn(data, 'getBranches').mockReturnValue(branches);
    const getServices = vi.spyOn(data, 'getServices').mockReturnValue(services);
    context.component.ngOnInit();
    return { ...context, branches, services, getServices };
  }
  it('keeps the initial shell pending through branches and services, then reveals hydrated selection', () => {
    const {component, branches, services} = setupPendingCatalog({sucursal:'10',servicios:'1'});
    expect(component.catalogoInicialListo()).toBe(false);
    branches.next([branch(10)]);
    expect(component.catalogoInicialListo()).toBe(false);
    services.next([service(1), service(2)]); services.complete();
    expect(component.catalogoInicialListo()).toBe(true);
    expect(component.selectedServices().map(item => item.id)).toEqual([1]);
  });
  it.each([{values: [] as SucursalCatalogo[]}, {values: [branch(10), branch(20)]}])('reveals an initial catalog without an automatically selected branch (%j)', ({values}) => {
    const {component, branches, getServices} = setupPendingCatalog();
    branches.next(values);
    expect(component.catalogoInicialListo()).toBe(true);
    expect(getServices).not.toHaveBeenCalled();
  });
  it('reveals branch errors instead of leaving the initial form hidden', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    const {component, branches} = setupPendingCatalog();
    branches.error(new Error('Network failure'));
    expect(component.catalogoInicialListo()).toBe(true);
    expect(component.errorMessage).toContain('sucursales');
  });
  it('reveals service errors and keeps the existing error message', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    const {component, branches, services} = setupPendingCatalog();
    branches.next([branch(10)]); services.error(new Error('Network failure'));
    expect(component.catalogoInicialListo()).toBe(true);
    expect(component.errorMessage).toContain('servicios');
  });
  it('does not reveal an obsolete service response before the current branch arrives', () => {
    const {component, branches, services, getServices} = setupPendingCatalog({sucursal:'10'});
    branches.next([branch(10), branch(20)]);
    const currentServices = new Subject<ServicioCatalogo[]>();
    getServices.mockReturnValue(currentServices); component.onBranchChange('20');
    services.next([service(1)]); services.complete();
    expect(component.catalogoInicialListo()).toBe(false);
    currentServices.next([service(4,20)]); currentServices.complete();
    expect(component.catalogoInicialListo()).toBe(true);
    expect(component.services().map(item => item.id)).toEqual([4]);
  });
  it('keeps the form revealed when changing branch after the first load', () => {
    const {component, branches} = setupPendingCatalog();
    branches.next([branch(10), branch(20)]);
    component.onBranchChange('20');
    expect(component.loadingCatalog()).toBe(true);
    expect(component.catalogoInicialListo()).toBe(true);
  });
  it('keeps the form revealed while reloading its sole branch after success', () => {
    vi.useFakeTimers();
    try {
      const {component, branches, services, getServices} = setupPendingCatalog();
      branches.next([branch(10)]); services.next([service(1)]); services.complete();
      getServices.mockReturnValue(new Subject<ServicioCatalogo[]>());
      (component as any).handleSuccess();
      expect(component.loadingCatalog()).toBe(true);
      expect(component.catalogoInicialListo()).toBe(true);
    } finally { vi.clearAllTimers(); vi.useRealTimers(); }
  });
  it('keeps later starting hours and distinct provider identities beyond eighteen combinations', () => {
    const { component } = setup();
    const slots: FranjaDisponible[] = Array.from({ length: 24 }, (_, index) => ({
      inicio: `2026-10-13T${String(9 + Math.floor(index / 3)).padStart(2, '0')}:00:00-06:00`,
      fin: `2026-10-13T${String(9 + Math.floor(index / 3)).padStart(2, '0')}:30:00-06:00`,
      hora: `${9 + Math.floor(index / 3)}:00`, prestadorId: index % 3 + 1
    } as FranjaDisponible));
    const itineraries = (component as any).buildItineraries([service(1)], [slots], 15);
    expect(itineraries.length).toBe(24);
    expect(new Set(itineraries.map((item: any) => item.id)).size).toBe(24);
    expect(itineraries.some((item: any) => item.inicio.includes('T16:00'))).toBe(true);
  });
  it('offers later complete routes for three services without overlapping their slots', () => {
    const { component } = setup();
    const slots: FranjaDisponible[] = Array.from({ length: 27 }, (_, index) => ({
      inicio: `2026-10-13T${String(9 + Math.floor(index / 3)).padStart(2, '0')}:00:00-06:00`,
      fin: `2026-10-13T${String(9 + Math.floor(index / 3)).padStart(2, '0')}:30:00-06:00`,
      hora: `${9 + Math.floor(index / 3)}:00`, prestadorId: index % 3 + 1
    } as FranjaDisponible));
    const itineraries = (component as any).buildItineraries(
      [service(1), service(2), service(3)], [slots, slots, slots], 60);
    expect(itineraries.some((item: any) => item.inicio.includes('T15:00'))).toBe(true);
    for (const itinerary of itineraries) {
      expect(itinerary.items.length).toBe(3);
      expect(new Date(itinerary.items[1].slot.inicio).getTime())
        .toBeGreaterThanOrEqual(new Date(itinerary.items[0].slot.fin).getTime());
      expect(new Date(itinerary.items[2].slot.inicio).getTime())
        .toBeGreaterThanOrEqual(new Date(itinerary.items[1].slot.fin).getTime());
    }
  });
  beforeEach(() => new ReservationCartService().clear(1));
  function setup(params: Record<string, string> = {}) {
    const slots = new Subject<FranjaDisponible[]>();
    // The catalog is deliberately stale; only the current backend quote defines money.
    const serverQuote: CotizacionCitas = {moneda:'MXN',subtotalLista:250,descuentoTotal:50,total:200,anticipoTotal:100,
      promocion:{id:88,titulo:'Oferta backend',tipoDescuento:'PORCENTAJE',valorDescuento:20,moneda:'MXN'},motivoNoAplicada:null,
      lineas:[{servicioId:1,nombre:'Servicio 1',precioLista:250,descuento:50,precioNeto:200,anticipo:100,promocionAplicada:true}]};
    const cotizar = vi.fn(() => of(serverQuote));
    const reservar = vi.fn((_payload: any) => of({id:90,mensaje:'Registrada',precio:200}));
    const checkoutConfig=vi.fn(() => of({habilitado:false}));
    const dias=vi.fn(() => of({hoyLocal:'2026-10-12',reservaHasta:'2026-11-12',dias:[] as string[],configuracion:{}}));
    const consultarFranjas=vi.fn((_request: any) => slots);
    TestBed.configureTestingModule({ providers: [
      { provide: TenantContextService, useValue: { empresaId: () => 1, slug: () => 'pruebas' } },
      { provide: PublicCheckoutService, useValue: { config: checkoutConfig } },
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(params) } } },
      { provide: BreakpointObserver, useValue: { isMatched: () => true, observe: () => of({ matches: true }) } },
      { provide: BookingDataService, useValue: {
        getBranches: () => of([{ id: 10 }, { id: 20 }]),
        getServices: (branch: number) => of(branch === 10 ? [service(1), service(2), service(3, 10, 'USD')] : [service(4, 20)])
      } },
      { provide: AppointmentService, useValue: { getPublicStaff: () => of([]), getAvailableSlots: consultarFranjas,
        getAvailableDays: dias, cotizarCitas: cotizar, bookAppointment: reservar } }
    ] });
    const component = TestBed.runInInjectionContext(() => new BookingComponent());
    return { component, slots, cart: TestBed.inject(ReservationCartService),cotizar,reservar,serverQuote,checkoutConfig,dias,consultarFranjas };
  }

  it('uses backend days including Sunday and keeps the selected professional in days, slots and draft', () => {
    const {component,dias,consultarFranjas,cart}=setup({sucursal:'10',servicios:'1'});
    dias.mockImplementation(() => of({hoyLocal:'2026-10-12',reservaHasta:'2026-11-12',dias:['2026-10-18'],configuracion:{}}));
    component.ngOnInit();TestBed.tick();
    expect(component.selectedDate()).toBe('2026-10-18');
    component.elegirPrestador(1,42);TestBed.tick();
    expect(dias).toHaveBeenLastCalledWith(10,[1],undefined,undefined,[42]);
    expect(consultarFranjas).toHaveBeenLastCalledWith(expect.objectContaining({prestadorId:42,fecha:'2026-10-18'}));
    expect(cart.read(1)?.prestadores).toEqual({1:42});
    component.elegirPrestador(1,null);TestBed.tick();
    expect(consultarFranjas).toHaveBeenLastCalledWith(expect.objectContaining({prestadorId:null}));
  });

it('does not quote again when only unselected available slots change', () => {
  const { component, cotizar } = setup({ sucursal: '10', servicios: '1', promocion: '88' });
  component.ngOnInit(); TestBed.tick();
  const before = cotizar.mock.calls.length;
  component.slotsByService.set({ 1: [] }); TestBed.tick();
  expect(cotizar.mock.calls.length).toBe(before);
});

  it('retains the promotion ID and uses backend totals without recalculating its percentage', () => {
    const {component,cart,cotizar} = setup({sucursal:'10',servicios:'1',promocion:'88'});
    component.ngOnInit(); TestBed.tick();
    expect(cart.read(1)?.promocionId).toBe(88);
    expect(cotizar).toHaveBeenCalledWith(expect.objectContaining({empresaId:1,sucursalId:10,promocionId:88}));
    expect(component.cotizacion()?.total).toBe(200);
    expect(component.cotizacion()?.anticipoTotal).toBe(100);
  });

  it('sends only the promotion ID with the reservation and keeps the chosen start for server validation', () => {
    const {component,cotizar,reservar} = setup({sucursal:'10',servicios:'1',promocion:'88'});
    component.ngOnInit();
    const slot = {inicio:'2026-12-01T10:00:00-06:00',fin:'2026-12-01T10:30:00-06:00',hora:'10:00',prestadorId:1};
    component.selectedDate.set('2026-12-01');
    component.availableItineraries.set([{id:'ruta',items:[{servicio:service(1),slot,staff:null}],inicio:slot.inicio,fin:slot.fin,totalPrecio:125.5,totalMinutos:30,usaMultiplesStaff:false}]);
    component.selectedItineraryId.set('ruta');
    component.bookingForm.patchValue({name:'Cliente prueba',phone:'2220000000',email:'cliente@example.com'});
    TestBed.tick(); component.onSubmit();
    expect(cotizar).toHaveBeenLastCalledWith(expect.objectContaining({items:[{servicioId:1,inicio:slot.inicio,fecha:'2026-12-01'}]}));
    expect(reservar).toHaveBeenCalledWith(expect.objectContaining({promocionId:88,inicio:slot.inicio}));
    const payload=reservar.mock.calls[0][0] as any;
    expect(payload.precio).toBeUndefined();expect(payload.descuento).toBeUndefined();
  });

  it('respeta el rechazo de reserva del backend sin crear cliente o cita',()=>{
    const {component,serverQuote,reservar}=setup();component.cotizacion.set({...serverQuote,reservableEnLinea:false,motivoNoReservable:'Anticipo no disponible del negocio'});
    component.onSubmit();expect(reservar).not.toHaveBeenCalled();
  });

  it('allows normal prices when the server reports an unavailable promotion', () => {
    const {component,cotizar,serverQuote}=setup({sucursal:'10',servicios:'1',promocion:'88'});
    cotizar.mockReturnValue(of({...serverQuote,promocion:null,motivoNoAplicada:'La promoción venció',total:250,descuentoTotal:0,anticipoTotal:250,
      lineas:serverQuote.lineas.map(linea=>({...linea,precioNeto:250,descuento:0,anticipo:250,promocionAplicada:false}))}));
    component.ngOnInit();TestBed.tick();
    expect(component.cotizacion()?.promocion).toBeNull();expect(component.cotizacion()?.motivoNoAplicada).toContain('venció');
    expect(component.selectedServicesPriceTotal()).toBe(250);
  });

  it('ignores a stale price response after the service is removed', () => {
    const {component,cotizar,serverQuote}=setup({sucursal:'10',servicios:'1',promocion:'88'});
    const pendiente=new Subject<CotizacionCitas>();cotizar.mockReturnValue(pendiente);
    component.ngOnInit();TestBed.tick();component.removeSelectedService(1);TestBed.tick();
    pendiente.next(serverQuote);
    expect(component.cotizacion()).toBeNull();expect(component.cotizando()).toBe(false);
  });

  it('does not send a zero-price reservation to checkout', () => {
    const {component,reservar,checkoutConfig}=setup({sucursal:'10',servicios:'1',promocion:'88'});
    reservar.mockReturnValue(of({id:90,mensaje:'Registrada',precio:0}));
    component.ngOnInit();
    const slot={inicio:'2026-12-01T10:00:00-06:00',fin:'2026-12-01T10:30:00-06:00',hora:'10:00',prestadorId:1};
    component.selectedDate.set('2026-12-01');component.availableItineraries.set([{id:'ruta',items:[{servicio:service(1),slot,staff:null}],inicio:slot.inicio,fin:slot.fin,totalPrecio:125.5,totalMinutos:30,usaMultiplesStaff:false}]);component.selectedItineraryId.set('ruta');
    component.bookingForm.patchValue({name:'Cliente prueba',phone:'2220000000',email:'cliente@example.com'});
    TestBed.tick();component.onSubmit();expect(reservar).toHaveBeenCalled();expect(checkoutConfig).not.toHaveBeenCalled();
  });

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
    expect(cart.read(1)).toEqual({ branchId: 20, serviceIds: [], prestadores: {} });
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
