import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { RecepcionService } from '../../../core/recepcion/recepcion.service';
import { RecepcionQuickAppointmentFacade } from './recepcion-quick-appointment.facade';

describe('RecepcionQuickAppointmentFacade', () => {
  it('normaliza la búsqueda antes de delegarla al servicio', async () => {
    const recepcionService = {
      buscarClientes: vi.fn().mockReturnValue(of([]))
    };
    TestBed.configureTestingModule({
      providers: [RecepcionQuickAppointmentFacade, { provide: RecepcionService, useValue: recepcionService }]
    });

    await firstValueFrom(TestBed.inject(RecepcionQuickAppointmentFacade).buscarClientes('  Ana  '));

    expect(recepcionService.buscarClientes).toHaveBeenCalledWith('Ana');
  });

  it('consulta disponibilidad con el contexto recibido del backend', async () => {
    const recepcionService = {
      getFranjasDisponibles: vi.fn().mockReturnValue(of([]))
    };
    TestBed.configureTestingModule({
      providers: [RecepcionQuickAppointmentFacade, { provide: RecepcionService, useValue: recepcionService }]
    });

    await firstValueFrom(TestBed.inject(RecepcionQuickAppointmentFacade).cargarFranjas(4, 9, '2026-08-25'));

    expect(recepcionService.getFranjasDisponibles).toHaveBeenCalledWith(4, 9, '2026-08-25');
  });

  it('delega la creación de citas sin alterar el payload', async () => {
    const payload = {
      sucursalId: 4,
      servicioId: 9,
      prestadorId: null,
      nombreCliente: 'Ana',
      correoCliente: '',
      telefonoCliente: '555',
      inicio: '2026-08-25T16:00:00.000Z',
      notas: null
    };
    const recepcionService = {
      crearCita: vi.fn().mockReturnValue(of({ id: 31 }))
    };
    TestBed.configureTestingModule({
      providers: [RecepcionQuickAppointmentFacade, { provide: RecepcionService, useValue: recepcionService }]
    });

    await firstValueFrom(TestBed.inject(RecepcionQuickAppointmentFacade).crearCita(payload));

    expect(recepcionService.crearCita).toHaveBeenCalledWith(payload);
  });
});
