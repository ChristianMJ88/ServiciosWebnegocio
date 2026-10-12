import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { StaffBlocksSectionComponent } from './staff-blocks-section.component';

describe('StaffBlocksSectionComponent', () => {
  beforeEach(() => TestBed.configureTestingModule({imports: [StaffBlocksSectionComponent]}));
  function crearFormulario() {
    const fixture = TestBed.createComponent(StaffBlocksSectionComponent);
    const formulario = {tipoSujeto: 'PRESTADOR', sujetoId: 0, fechaExcepcion: '2026-10-12',
      horaInicio: '12:00', horaFin: '13:00', tipoBloqueo: 'DESCANSO', motivo: null};
    fixture.componentRef.setInput('formularioExcepcion', formulario);
    fixture.componentRef.setInput('tiposBloqueo', [{valor: 'DESCANSO', etiqueta: 'Descanso'}]);
    fixture.detectChanges();
    return {fixture, formulario, component: fixture.componentInstance};
  }
  it('edita un intervalo conservando ambas horas', () => {
    const {component, formulario} = crearFormulario();
    const guardar = vi.spyOn(component.save, 'emit');
    expect(component.todoElDia()).toBe(false);
    component.guardar();
    expect(formulario.horaInicio).toBe('12:00');
    expect(formulario.horaFin).toBe('13:00');
    expect(guardar).toHaveBeenCalledOnce();
  });
  it('envía ambas horas vacías cuando el usuario elige todo el día', () => {
    const {component, formulario} = crearFormulario();
    component.todoElDia.set(true);
    component.guardar();
    expect(formulario.horaInicio).toBeNull();
    expect(formulario.horaFin).toBeNull();
    expect(component.etiquetaTipo('DESCANSO')).toBe('Descanso');
  });
});
