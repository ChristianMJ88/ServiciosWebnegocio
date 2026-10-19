import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { LegalPageComponent } from './legal-page.component';
import { environment } from '../../../environments/environment';
const url=environment.apiBaseUrl+'/publico/plataforma/contacto-legal';
function setup(document:string){TestBed.configureTestingModule({imports:[LegalPageComponent],providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting(),{provide:ActivatedRoute,useValue:{snapshot:{data:{legalDocument:document}}}}]});const fixture=TestBed.createComponent(LegalPageComponent);fixture.detectChanges();return {fixture,http:TestBed.inject(HttpTestingController)};}
describe('contacto legal separado',()=>{
 for(const document of ['privacy','terms','deletion'])it('muestra correo configurado en '+document,()=>{const {fixture,http}=setup(document);expect(fixture.nativeElement.textContent).toContain('Cargando contacto');expect(fixture.nativeElement.textContent).not.toContain('no disponible');http.expectNone(environment.apiBaseUrl+'/publico/plataforma/soporte');http.expectOne(url).flush({correo:'legal@example.test'});fixture.detectChanges();const links=Array.from(fixture.nativeElement.querySelectorAll('a[href^="mailto:"]')) as HTMLAnchorElement[];expect(links.length).toBe(document==='privacy'?2:1);for(const link of links)expect(link.href).toContain('mailto:legal@example.test');if(document==='deletion')expect(links[0].href).toContain('subject='+encodeURIComponent('Solicitud de eliminación de datos'));http.verify();});
 it('sin datos no inventa destinatario ni remite al mismo correo',()=>{const {fixture,http}=setup('deletion');http.expectOne(url).flush({}, {status:404,statusText:'Not found'});fixture.detectChanges();expect(fixture.nativeElement.querySelector('a[href^="mailto:"]')).toBeNull();expect(fixture.nativeElement.textContent).toContain('Contacto legal no disponible');expect(fixture.nativeElement.textContent).not.toContain('escribiendo al mismo correo');http.verify();});
});
