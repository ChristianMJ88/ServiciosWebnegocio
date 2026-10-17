import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import esMx from '@angular/common/locales/es-MX';
import { describe, it, expect, vi } from 'vitest';
import { RecepcionAgendaSectionComponent } from './recepcion-agenda-section.component';
import { CitaRecepcion } from '../../../core/recepcion/recepcion.service';

describe('Elegibilidad de cierre en Recepción', () => {
  it('habilita Finalizar solo con puedeCerrar, sin comparar la fecha en el navegador', () => {
    registerLocaleData(esMx);
    TestBed.configureTestingModule({imports:[RecepcionAgendaSectionComponent],providers:[provideRouter([])]});
    const fixture=TestBed.createComponent(RecepcionAgendaSectionComponent);
    const cita={id:1,estado:'CONFIRMADA',inicio:'2030-10-15T10:00:00-06:00',clienteNombre:'Cliente',servicioNombre:'Servicio',prestadorNombre:'Staff',sucursalNombre:'Sucursal',puedeCerrar:false} as CitaRecepcion;
    Object.assign(fixture.componentInstance,{citas:[cita],guardando:false,puedeGestionarCitas:true,puedeRegistrarCheckIn:false,estadosCita:[],estadoPendiente:'PENDIENTE'});
    fixture.detectChanges();
    const finalizar=() => [...fixture.nativeElement.querySelectorAll('button')].find((b:any) => b.textContent.trim()==='Finalizar') as HTMLButtonElement;
    const noAsistio=() => [...fixture.nativeElement.querySelectorAll('button')].find((b:any) => b.textContent.trim()==='No asistió') as HTMLButtonElement;
    const cancelar=() => [...fixture.nativeElement.querySelectorAll('button')].find((b:any) => b.textContent.trim()==='Cancelar') as HTMLButtonElement;
    const noShow=vi.fn();fixture.componentInstance.noShow.subscribe(noShow);
    expect(noAsistio().disabled).toBe(true);
    expect(cancelar().disabled).toBe(true);
    expect(finalizar().disabled).toBe(true);
    fixture.componentRef.setInput('citas',[{...cita,puedeCerrar:true}]);fixture.detectChanges();
    expect(finalizar().disabled).toBe(false);
    expect(noAsistio().disabled).toBe(false);
    noAsistio().click();expect(noShow).toHaveBeenCalledWith(1);
    expect(cancelar().disabled).toBe(true);
    fixture.componentRef.setInput('citas',[{...cita,puedeCerrar:false,puedeCancelar:true}]);fixture.detectChanges();
    expect(cancelar().disabled).toBe(false);
    fixture.componentRef.setInput('guardando',true);fixture.detectChanges();
    expect(cancelar().disabled).toBe(true);
  });
});
