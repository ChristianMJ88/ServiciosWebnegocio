import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NavigationEnd, Router, provideRouter } from '@angular/router';
import { FooterComponent } from './footer.component';
import { PlatformHostService } from '../../core/platform/platform-host.service';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { environment } from '../../../environments/environment';
import { Subject } from 'rxjs';
import { vi } from 'vitest';
const endpoint=environment.apiBaseUrl+'/publico/plataforma/soporte';
function setup(custom=false,url='/',navigated=true){
 TestBed.configureTestingModule({imports:[FooterComponent],providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting()]});
 TestBed.inject(Router).navigated=navigated;
 vi.spyOn(TestBed.inject(Router),'url','get').mockReturnValue(url);
 vi.spyOn(TestBed.inject(PlatformHostService),'isCustomTenantHost').mockReturnValue(custom);
 const fixture=TestBed.createComponent(FooterComponent);return {fixture,http:TestBed.inject(HttpTestingController)};
}
describe('contacto en footer',()=>{
 afterEach(()=>TestBed.inject(HttpTestingController).verify());
 it('muestra solo los datos globales de la API',()=>{const {fixture,http}=setup();fixture.detectChanges();http.expectOne(endpoint).flush({correo:'help@example.test',telefono:'+12025550189',dias:[6,7],horaInicio:'10:00',horaFin:'16:00',zonaHoraria:'Europe/Madrid'});fixture.detectChanges();expect(fixture.nativeElement.querySelector('[data-fluora-support]').textContent).toContain('de sábado a domingo');expect(fixture.nativeElement.querySelector('a[href="mailto:help@example.test"]')).not.toBeNull();http.expectNone(environment.apiBaseUrl+'/publico/plataforma/contacto-legal');});
 it('espera la primera navegación aunque router.url siga en /',()=>{const {fixture,http}=setup(false,'/',false);fixture.detectChanges();http.expectNone(endpoint);expect(fixture.nativeElement.querySelector('[data-fluora-support]')).toBeNull();(TestBed.inject(Router).events as Subject<unknown>).next(new NavigationEnd(1,'/','/'));fixture.detectChanges();http.expectOne(endpoint).flush({}, {status:404,statusText:'Not found'});});
 it('oculta el bloque sin configuración',()=>{const {fixture,http}=setup();fixture.detectChanges();http.expectOne(endpoint).flush({}, {status:404,statusText:'Not found'});fixture.detectChanges();expect(fixture.nativeElement.querySelector('[data-fluora-support]')).toBeNull();});
 it('no solicita soporte cuando el host es de tenant y aún no carga el contexto',()=>{const {fixture,http}=setup(true);fixture.detectChanges();http.expectNone(endpoint);expect(fixture.nativeElement.querySelector('[data-fluora-support]')).toBeNull();});
 it('no solicita soporte en /e/x antes de resolver el tenant',()=>{const {fixture,http}=setup(false,'/e/x');fixture.detectChanges();http.expectNone(endpoint);expect(fixture.nativeElement.querySelector('[data-fluora-support]')).toBeNull();});
 it('conserva el contacto del negocio y no mezcla soporte',()=>{const {fixture,http}=setup();const tenant=TestBed.inject(TenantContextService);vi.spyOn(tenant,'activo').mockReturnValue(true);vi.spyOn(tenant,'correo').mockReturnValue('negocio@example.test');vi.spyOn(tenant,'footerMostrarContacto').mockReturnValue(true);fixture.detectChanges();http.expectNone(endpoint);expect(fixture.nativeElement.querySelector('a[href="mailto:negocio@example.test"]')).not.toBeNull();expect(fixture.nativeElement.querySelector('[data-fluora-support]')).toBeNull();});
});
