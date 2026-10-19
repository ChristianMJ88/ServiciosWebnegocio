import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SupportEditorComponent } from './support-editor.component';
import { environment } from '../../../environments/environment';
const url=environment.apiBaseUrl+'/plataforma/marketing/soporte';
const value={version:3,correo:'help@example.test',correoLegal:'legal@example.test',telefono:'+12025550189',dias:[2,4],horaInicio:'10:00',horaFin:'16:00',zonaHoraria:'Europe/Madrid',visible:false,actualizadoPor:12};
describe('editor de contacto de plataforma',()=>{
 it('envía ambos contactos y la versión, sin campos de auditoría; muestra el error backend',()=>{
  TestBed.configureTestingModule({imports:[SupportEditorComponent],providers:[provideHttpClient(),provideHttpClientTesting()]});
  const fixture=TestBed.createComponent(SupportEditorComponent);const http=TestBed.inject(HttpTestingController);http.expectOne(url).flush(value);fixture.detectChanges();
  fixture.componentInstance.draft!.correoLegal='new@example.test';fixture.componentInstance.save();
  const put=http.expectOne(url);expect(put.request.method).toBe('PUT');expect(put.request.body.correoLegal).toBe('new@example.test');expect(put.request.body.version).toBe(3);expect(put.request.body.actualizadoPor).toBeUndefined();
  put.flush({mensaje:'La configuración cambió'}, {status:409,statusText:'Conflict'});fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('La configuración cambió');expect(fixture.componentInstance.saving()).toBe(false);http.verify();
 });
});
