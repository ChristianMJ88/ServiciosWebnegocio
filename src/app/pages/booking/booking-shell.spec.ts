import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { DeferBlockBehavior, TestBed } from '@angular/core/testing';
import { BreakpointObserver } from '@angular/cdk/layout';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { BookingComponent } from './booking.component';
import { BookingDataService, ServicioCatalogo, SucursalCatalogo } from '../../services/booking-data.service';
import { PublicCheckoutService } from '../../services/public-checkout.service';
import { AppointmentService, CotizacionCitas, PrestadorPublico } from '../../services/appointment.service';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { vi } from 'vitest';

registerLocaleData(localeEs);

describe('Booking: presentación del catálogo inicial', () => {
  it.each([false, true])('mantiene la precarga inert y conserva el formulario hasta resolver la selección (error: %s)', async failure => {
    const branches = new Subject<SucursalCatalogo[]>(), quote = new Subject<CotizacionCitas>(), staff = new Subject<PrestadorPublico[]>();
    const cotizar = vi.fn(() => quote.asObservable());
    const service: ServicioCatalogo = {
      id:1,sucursalId:10,nombre:'Servicio de prueba',slug:'servicio',descripcion:'',moneda:'MXN',precio:100,duracionMinutos:30,
      bufferAntesMinutos:0,bufferDespuesMinutos:0,grupoId:null,grupoNombre:null,subgrupoId:null,subgrupoNombre:null,imagenUrl:null,
      ordenPublico:0,visiblePublico:true,requiereAnticipo:false,anticipoTipo:null,anticipoValor:null
    };
    const result: CotizacionCitas = {moneda:'MXN',subtotalLista:100,descuentoTotal:0,total:100,anticipoTotal:0,promocion:null,motivoNoAplicada:null,
      lineas:[{servicioId:1,nombre:'Servicio de prueba',precioLista:100,descuento:0,precioNeto:100,anticipo:0,promocionAplicada:false}]};
    await TestBed.configureTestingModule({
      imports: [BookingComponent], deferBlockBehavior: DeferBlockBehavior.Manual,
      providers: [provideHttpClient(),provideHttpClientTesting(),provideRouter([]),
        {provide:ActivatedRoute,useValue:{snapshot:{queryParamMap:convertToParamMap({sucursal:'10',servicios:'1'})}}},
        {provide:BreakpointObserver,useValue:{isMatched:()=>true,observe:()=>of({matches:true})}},
        {provide:BookingDataService,useValue:{getBranches:()=>branches,getServices:()=>of([service])}},
        {provide:PublicCheckoutService,useValue:{config:()=>of({habilitado:false})}},
        {provide:AppointmentService,useValue:{cotizarCitas:cotizar,getPublicStaff:()=>staff,
          getAvailableDays:()=>of({hoyLocal:'2027-02-01',reservaHasta:'2027-02-02',dias:[],configuracion:{}})}}]
    }).compileComponents();
    TestBed.inject(TenantContextService).setTenant({
      empresaId:42,slug:'prueba',nombreComercial:'Negocio de prueba',publicado:true,logoUrl:null,descripcionCorta:null,
      colorPrimario:null,colorSecundario:null,fuenteTitulos:null,fuenteCuerpo:null,heroTitulo:null,heroSubtitulo:null,heroImagenUrl:null,
      whatsapp:null,telefono:null,correo:null,direccion:null,instagramUrl:null,facebookUrl:null,tema:null
    });
    const fixture = TestBed.createComponent(BookingComponent); fixture.detectChanges();
    const shell = fixture.nativeElement.querySelector('.booking-shell') as HTMLElement;
    const flow = shell.querySelector('.booking-flow');
    branches.next([{id:10,empresaId:42,nombre:'Sucursal',direccion:'',telefono:'',zonaHoraria:'America/Mexico_City'}]);
    fixture.detectChanges();
    const status = fixture.nativeElement.querySelector('p[role="status"]') as HTMLElement;
    expect(shell.hasAttribute('inert')).toBe(true);
    expect(shell.getAttribute('aria-busy')).toBe('true');
    expect(status.textContent).toContain('Preparando tu selección');
    expect(shell.contains(status)).toBe(false);
    expect(shell.querySelector('.booking-flow')).toBe(flow);
    staff.next([]); staff.complete(); fixture.detectChanges();
    expect(shell.hasAttribute('inert')).toBe(true);
    if (failure) quote.error({error:{mensaje:'Precio temporalmente no disponible'}});
    else { quote.next(result); quote.complete(); }
    fixture.detectChanges();
    expect(shell.hasAttribute('inert')).toBe(false);
    expect(status.textContent?.trim()).toBe('');
    expect(shell.querySelector('.booking-flow')).toBe(flow);
    if (failure) {
      const retry = Array.from(shell.querySelectorAll('button')).find(button=>button.textContent?.trim()==='Reintentar cotización')!;
      expect(retry).toBeTruthy();
      cotizar.mockReturnValue(of(result)); retry.click(); fixture.detectChanges();
      expect(cotizar).toHaveBeenCalledTimes(2);
      expect(fixture.componentInstance.cotizacion()).toEqual(result);
      expect(shell.hasAttribute('inert')).toBe(false);
    }
    const nextQuote = new Subject<CotizacionCitas>(); cotizar.mockReturnValue(nextQuote.asObservable());
    fixture.componentInstance.reintentarCotizacion(); fixture.detectChanges();
    const quoteStatus = shell.querySelector('.booking-quote-status') as HTMLElement;
    expect(quoteStatus.textContent?.trim()).toBe('Confirmando condiciones de tu reserva…');
    expect(quoteStatus.textContent).not.toContain('$');
    expect(fixture.componentInstance.cotizacion()).toBeNull();
    expect(shell.hasAttribute('inert')).toBe(false);
    nextQuote.next(result); nextQuote.complete(); fixture.detectChanges();
    expect(fixture.componentInstance.cotizacion()).toEqual(result);
    expect(shell.querySelector('.booking-quote-status')?.textContent).toContain('pendiente de confirmación');
  });
  it('conserva el formulario mientras espera y anuncia la carga fuera del contenedor oculto', async () => {
    const branches = new Subject<SucursalCatalogo[]>();
    await TestBed.configureTestingModule({
      imports: [BookingComponent],
      deferBlockBehavior: DeferBlockBehavior.Manual,
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } },
        { provide: BreakpointObserver, useValue: { isMatched: () => true, observe: () => of({ matches: true }) } },
        { provide: BookingDataService, useValue: { getBranches: () => branches } },
        { provide: PublicCheckoutService, useValue: { config: () => of({ habilitado: false }) } }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(BookingComponent); fixture.detectChanges();
    const shell = fixture.nativeElement.querySelector('.booking-shell') as HTMLElement;
    const form = shell.querySelector('.booking-flow');
    const status = fixture.nativeElement.querySelector('p[role="status"]') as HTMLElement;
    expect(shell.classList.contains('booking-shell--pending')).toBe(true);
    expect(shell.hasAttribute('inert')).toBe(true);
    expect(shell.contains(status)).toBe(false);
    expect(status.textContent).toContain('Cargando servicios');
    branches.next([]); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.booking-flow')).toBe(form);
    expect(shell.classList.contains('booking-shell--pending')).toBe(false);
    expect(shell.hasAttribute('inert')).toBe(false);
    expect(status.textContent?.trim()).toBe('');
  });
});
