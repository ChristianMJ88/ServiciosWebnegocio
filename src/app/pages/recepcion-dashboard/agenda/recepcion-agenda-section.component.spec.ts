import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import esMx from '@angular/common/locales/es-MX';
import { describe, it, expect, vi } from 'vitest';
import { RecepcionAgendaSectionComponent } from './recepcion-agenda-section.component';
import { CitaRecepcion } from '../../../core/recepcion/recepcion.service';

describe('Elegibilidad de cierre en Recepción', () => {
  it('separa por decisión backend y conserva el cierre de las pagadas',()=>{
    registerLocaleData(esMx);TestBed.configureTestingModule({imports:[RecepcionAgendaSectionComponent],providers:[provideRouter([])]});const f=TestBed.createComponent(RecepcionAgendaSectionComponent);
    const base={id:1,inicio:'2026-10-17T10:00:00-06:00',clienteNombre:'Cliente',servicioNombre:'Servicio',estado:'CONFIRMADA',estadoCobro:'PAGADA',pendiente:0,puedeCerrar:true};
    Object.assign(f.componentInstance,{citas:[{...base,seccionAgenda:'ACTIVA'},{...base,id:2,seccionAgenda:'FINALIZADA'},{...base,id:3,seccionAgenda:'CANCELADA'}],guardando:false,puedeGestionarCitas:true,puedeRegistrarCheckIn:false,estadosCita:[],estadoPendiente:'PENDIENTE'});f.detectChanges();
    expect(f.componentInstance.seccion).toBe('ACTIVA');expect(f.nativeElement.querySelectorAll('.recepcion-appointment-card').length).toBe(1);
    const tabs=[...f.nativeElement.querySelectorAll('.agenda-segments button')] as HTMLButtonElement[];expect(tabs.map(t=>t.textContent)).toEqual(['Activas 1','Finalizadas 1','Canceladas 1']);tabs[1].click();f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Pagada · por cerrar');expect([...f.nativeElement.querySelectorAll('button')].find((b:any)=>b.textContent.trim()==='Finalizar').disabled).toBe(false);
  });
  it('distingue una atención sin costo de una cita parcialmente pagada',()=>{
    registerLocaleData(esMx);TestBed.configureTestingModule({imports:[RecepcionAgendaSectionComponent],providers:[provideRouter([])]});const f=TestBed.createComponent(RecepcionAgendaSectionComponent);const c=f.componentInstance;
    Object.assign(c,{citas:[{id:1,seccionAgenda:'FINALIZADA',estadoCobro:'SIN_COSTO',puedeCerrar:true,inicio:'2026-10-17T10:00:00-06:00'},{id:2,seccionAgenda:'ACTIVA',estadoCobro:'PARCIAL',puedeCerrar:true,inicio:'2026-10-17T11:00:00-06:00'}],guardando:false,puedeGestionarCitas:true,puedeRegistrarCheckIn:false,estadosCita:[],estadoPendiente:'PENDIENTE'});
    c.seccion='FINALIZADA';f.detectChanges();expect(f.nativeElement.textContent).toContain('Atendida · por cerrar');expect(f.nativeElement.textContent).not.toContain('Pagada · por cerrar');(f.nativeElement.querySelector('.agenda-segments button') as HTMLButtonElement).click();f.detectChanges();expect(f.nativeElement.querySelectorAll('.recepcion-appointment-card').length).toBe(1);expect(f.nativeElement.textContent).not.toContain('Pagada · por cerrar');
  });
  it('habilita Finalizar solo con puedeCerrar, sin comparar la fecha en el navegador', () => {
    registerLocaleData(esMx);
    TestBed.configureTestingModule({imports:[RecepcionAgendaSectionComponent],providers:[provideRouter([])]});
    const fixture=TestBed.createComponent(RecepcionAgendaSectionComponent);
    const cita={id:1,seccionAgenda:'ACTIVA',estado:'CONFIRMADA',inicio:'2030-10-15T10:00:00-06:00',clienteNombre:'Cliente',servicioNombre:'Servicio',prestadorNombre:'Staff',sucursalNombre:'Sucursal',puedeCerrar:false} as CitaRecepcion;
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
