import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AdminService } from '../../../core/admin/admin.service';
import { AdminContactsFacade } from './admin-contacts.facade';

describe('AdminContactsFacade', () => {
  it('delega la actualización al servicio administrativo', async () => {
    const adminService = { actualizarEstadoContacto: vi.fn().mockReturnValue(of({ id: 4 })) };
    TestBed.configureTestingModule({
      providers: [AdminContactsFacade, { provide: AdminService, useValue: adminService }]
    });
    await firstValueFrom(TestBed.inject(AdminContactsFacade).actualizarEstado(4, 'RESUELTO'));
    expect(adminService.actualizarEstadoContacto).toHaveBeenCalledWith(4, 'RESUELTO');
  });
});
