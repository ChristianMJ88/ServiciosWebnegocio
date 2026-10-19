import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { MarketingOfferService } from './marketing-offer.service';
import { environment } from '../../../environments/environment';
describe('oferta única de marketing',()=>{
 it('consulta una vez y presenta los datos recibidos sin precios ni plazos propios',()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});const service=TestBed.inject(MarketingOfferService);service.load();service.load();const http=TestBed.inject(HttpTestingController);http.expectOne(environment.apiBaseUrl+'/publico/plataforma/planes').flush([{id:84,nombre:'Flexible',diasPrueba:0,destacado:true}]);expect(service.label()).toBe('Comenzar con Flexible');expect(service.shortLabel()).toBe('Empezar');expect(service.href()).toContain('plan=84');http.verify();});
 it('permite reintentar una consulta fallida',()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});const s=TestBed.inject(MarketingOfferService),http=TestBed.inject(HttpTestingController);s.load();http.expectOne(environment.apiBaseUrl+'/publico/plataforma/planes').flush({}, {status:503,statusText:'Unavailable'});expect(s.failed()).toBe(true);s.load();http.expectOne(environment.apiBaseUrl+'/publico/plataforma/planes').flush([]);expect(s.failed()).toBe(false);expect(s.loading()).toBe(false);http.verify();});

});
