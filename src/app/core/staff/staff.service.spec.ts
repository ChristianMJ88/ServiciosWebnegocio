import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { StaffService } from './staff.service';
import { environment } from '../../../environments/environment';

describe('StaffService: horario semanal', () => {
  let service: StaffService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    service = TestBed.inject(StaffService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('envía los días elegidos en una sola operación, sin enviar una empresa', () => {
    const horario = {tipoSujeto: 'PRESTADOR', sujetoId: 0, diaSemana: 1,
      horaInicio: '10:00', horaFin: '17:00', intervaloMinutos: 30,
      vigenteDesde: null, vigenteHasta: null};
    service.guardarHorarioSemanal(horario, [1, 3, 5], false).subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/staff/disponibilidad/reglas/semana`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({horario, diasSemana: [1, 3, 5], reemplazar: false});
    expect(request.request.body.empresaId).toBeUndefined();
    request.flush([]);
  });
});
