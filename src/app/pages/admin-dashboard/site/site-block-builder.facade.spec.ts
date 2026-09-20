import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminService, BloqueSitioAdmin } from '../../../core/admin/admin.service';
import { SiteBlockBuilderFacade } from './site-block-builder.facade';

describe('SiteBlockBuilderFacade', () => {
  const adminService = {
    getBloquesSitio: vi.fn(),
    actualizarBloquesSitio: vi.fn()
  };
  let facade: SiteBlockBuilderFacade;

  const bloques: BloqueSitioAdmin[] = [
    { tipo: 'CONTACTO', nombre: 'Contacto', descripcion: 'Datos', icono: 'icon', habilitado: true, orden: 8, disponible: true, superficie: 'SUAVE', presentacion: 'PANEL', anchoContenido: 'NORMAL', alineacion: 'IZQUIERDA', separador: 'LINEA', espaciado: 'NORMAL', etiqueta: 'Contacto', titulo: null, descripcionVisual: null },
    { tipo: 'PORTADA', nombre: 'Portada', descripcion: 'Hero', icono: 'icon', habilitado: true, orden: 0, disponible: true, superficie: 'BASE', presentacion: 'ABIERTA', anchoContenido: 'AMPLIO', alineacion: 'IZQUIERDA', separador: 'NINGUNO', espaciado: 'NORMAL', etiqueta: null, titulo: null, descripcionVisual: null }
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [SiteBlockBuilderFacade, { provide: AdminService, useValue: adminService }]
    });
    facade = TestBed.inject(SiteBlockBuilderFacade);
  });

  it('delega la carga al servicio administrativo', async () => {
    adminService.getBloquesSitio.mockReturnValue(of(bloques));
    await expect(firstValueFrom(facade.cargar())).resolves.toEqual(bloques);
  });

  it('envía estructura y presentación calculadas por bloque', async () => {
    adminService.actualizarBloquesSitio.mockReturnValue(of(bloques));
    await firstValueFrom(facade.guardar(bloques));

    expect(adminService.actualizarBloquesSitio).toHaveBeenCalledWith({
      bloques: [
        { tipo: 'CONTACTO', habilitado: true, orden: 0, superficie: 'SUAVE', presentacion: 'PANEL', anchoContenido: 'NORMAL', alineacion: 'IZQUIERDA', separador: 'LINEA', espaciado: 'NORMAL', etiqueta: 'Contacto', titulo: null, descripcionVisual: null },
        { tipo: 'PORTADA', habilitado: true, orden: 1, superficie: 'BASE', presentacion: 'ABIERTA', anchoContenido: 'AMPLIO', alineacion: 'IZQUIERDA', separador: 'NINGUNO', espaciado: 'NORMAL', etiqueta: null, titulo: null, descripcionVisual: null }
      ]
    });
  });
});
