import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AdminService, ServiciosDestacadosAdmin } from '../../../core/admin/admin.service';
import { SiteFeaturedServicesFacade } from './site-featured-services.facade';

describe('SiteFeaturedServicesFacade', () => {
  const configuracion: ServiciosDestacadosAdmin = {
    vista: 'TARJETAS', mostrarPrecio: true, mostrarDuracion: true,
    mostrarDescripcion: true, limite: 3, seleccionAutomatica: true,
    vistasDisponibles: [], elementos: []
  };
  const adminService = {
    getServiciosDestacadosSitio: vi.fn(() => of(configuracion)),
    getGruposServicio: vi.fn(() => of([{ id: 1, nombre: 'Uñas' }])),
    getSubgruposServicio: vi.fn(() => of([{ id: 2, nombre: 'Manos' }])),
    getServicios: vi.fn(() => of([{ id: 3, nombre: 'Gel' }])),
    actualizarServiciosDestacadosSitio: vi.fn(payload => of({ ...configuracion, ...payload }))
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [SiteFeaturedServicesFacade, { provide: AdminService, useValue: adminService }]
    });
  });

  it('carga configuración y catálogo del tenant en una sola operación', () => {
    TestBed.inject(SiteFeaturedServicesFacade).cargar().subscribe(respuesta => {
      expect(respuesta.configuracion).toBe(configuracion);
      expect(respuesta.grupos[0].nombre).toBe('Uñas');
      expect(respuesta.subgrupos[0].nombre).toBe('Manos');
      expect(respuesta.servicios[0].nombre).toBe('Gel');
    });
  });

  it('delega el guardado del orden y la vista al backend', () => {
    const payload = {
      vista: 'LISTA', mostrarPrecio: true, mostrarDuracion: false,
      mostrarDescripcion: true, limite: 4,
      elementos: [{ tipo: 'SERVICIO' as const, referenciaId: 3, etiqueta: 'Popular', orden: 0 }]
    };
    TestBed.inject(SiteFeaturedServicesFacade).guardar(payload).subscribe();
    expect(adminService.actualizarServiciosDestacadosSitio).toHaveBeenCalledWith(payload);
  });
});
