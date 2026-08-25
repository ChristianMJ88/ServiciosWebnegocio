import { Injectable, inject } from '@angular/core';
import { Observable, catchError, forkJoin, of } from 'rxjs';
import { AdminService } from '../../core/admin/admin.service';
import { AuthService } from '../../core/auth/auth.service';

export type AdminDashboardLoadErrorHandler = (error: unknown, fallbackMessage: string) => void;

@Injectable({ providedIn: 'root' })
export class AdminDashboardLoader {
  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);

  cargar(onError: AdminDashboardLoadErrorHandler) {
    const puedeGestionarUsuarios = this.authService.puedeGestionarUsuariosInternos();
    const puedeGestionarPrestadores = this.authService.puedeGestionarPrestadores();
    const puedeGestionarWhatsapp = this.authService.puedeGestionarWhatsapp();
    const puedeGestionarConfiguracion = this.authService.puedeGestionarConfiguracionEmpresa();

    return forkJoin({
      resumen: this.conFallback(this.adminService.getResumen(), null, 'No se pudo cargar el resumen.', onError),
      citas: this.cargarSi(
        this.authService.puedeGestionarCitasAdmin(),
        this.adminService.getCitas(),
        [],
        'No se pudieron cargar las citas.',
        onError
      ),
      contactos: this.cargarSi(
        this.authService.puedeVerContactosAdmin(),
        this.adminService.getContactos(),
        [],
        'No se pudieron cargar los contactos.',
        onError
      ),
      sucursales: this.cargarSi(
        this.authService.puedeGestionarSucursales()
          || this.authService.puedeGestionarServicios()
          || puedeGestionarPrestadores
          || puedeGestionarUsuarios,
        this.adminService.getSucursales(),
        [],
        'No se pudieron cargar las sucursales.',
        onError
      ),
      gruposServicio: this.cargarSi(
        this.authService.puedeGestionarServicios(),
        this.adminService.getGruposServicio(),
        [],
        'No se pudieron cargar los grupos de servicios.',
        onError
      ),
      subgruposServicio: this.cargarSi(
        this.authService.puedeGestionarServicios(),
        this.adminService.getSubgruposServicio(),
        [],
        'No se pudieron cargar los subgrupos de servicios.',
        onError
      ),
      catalogosSugeridos: this.cargarSi(
        this.authService.puedeGestionarServicios(),
        this.adminService.getCatalogosSugeridos(),
        [],
        'No se pudieron cargar las sugerencias de catálogo.',
        onError
      ),
      servicios: this.cargarSi(
        this.authService.puedeGestionarServicios() || puedeGestionarPrestadores,
        this.adminService.getServicios(),
        [],
        'No se pudieron cargar los servicios.',
        onError
      ),
      prestadores: this.cargarSi(puedeGestionarPrestadores, this.adminService.getPrestadores(), [], 'No se pudieron cargar los prestadores.', onError),
      rolesInternos: this.cargarSi(puedeGestionarUsuarios, this.adminService.getRolesInternos(), [], 'No se pudieron cargar los roles internos.', onError),
      plantillasRolesInternos: this.cargarSi(puedeGestionarUsuarios, this.adminService.getPlantillasRolesInternos(), [], 'No se pudieron cargar las plantillas de roles.', onError),
      auditoriaRolesInternos: this.cargarSi(puedeGestionarUsuarios, this.adminService.getAuditoriaRolesInternos(), [], 'No se pudo cargar la auditoría de roles.', onError),
      permisos: this.cargarSi(puedeGestionarUsuarios, this.adminService.getPermisos(), [], 'No se pudo cargar el catálogo de permisos.', onError),
      usuariosInternos: this.cargarSi(puedeGestionarUsuarios, this.adminService.getUsuariosInternos(), [], 'No se pudieron cargar los usuarios internos.', onError),
      reglas: this.cargarSi(puedeGestionarPrestadores, this.adminService.getReglasDisponibilidad(), [], 'No se pudieron cargar las reglas de disponibilidad.', onError),
      excepciones: this.cargarSi(puedeGestionarPrestadores, this.adminService.getExcepcionesDisponibilidad(), [], 'No se pudieron cargar las excepciones.', onError),
      reporteServicios: this.cargarSi(this.authService.puedeVerReportesAdmin(), this.adminService.getReporteServicios(), [], 'No se pudo cargar el reporte de servicios.', onError),
      reportePrestadores: this.cargarSi(this.authService.puedeVerReportesAdmin(), this.adminService.getReportePrestadores(), [], 'No se pudo cargar el reporte de prestadores.', onError),
      configuracionSitio: this.cargarSi(puedeGestionarConfiguracion, this.adminService.getConfiguracionSitio(), null, 'No se pudo cargar la configuración del sitio web.', onError),
      configuracionCorreo: this.cargarSi(puedeGestionarConfiguracion, this.adminService.getConfiguracionCorreo(), null, 'No se pudo cargar la configuración de correo.', onError),
      auditoriaConfiguracion: this.cargarSi(
        puedeGestionarConfiguracion || puedeGestionarWhatsapp,
        this.adminService.getAuditoriaConfiguracion(),
        [],
        'No se pudo cargar la auditoría de configuración.',
        onError
      ),
      configuracionWhatsapp: this.cargarSi(puedeGestionarWhatsapp, this.adminService.getConfiguracionWhatsapp(), null, 'No se pudo cargar la configuración de WhatsApp.', onError),
      plantillasWhatsapp: this.cargarSi(puedeGestionarWhatsapp, this.adminService.getPlantillasWhatsapp(), [], 'No se pudieron cargar las plantillas de WhatsApp.', onError),
      plantillasWhatsappEmpresa: this.cargarSi(puedeGestionarWhatsapp, this.adminService.getPlantillasWhatsappEmpresa(), [], 'No se pudieron cargar las plantillas configuradas del tenant.', onError),
      logsWhatsapp: this.cargarSi(puedeGestionarWhatsapp, this.adminService.getLogsWhatsapp(), [], 'No se pudieron cargar los logs de WhatsApp.', onError),
      mensajesWhatsapp: this.cargarSi(puedeGestionarWhatsapp, this.adminService.getMensajesWhatsapp(), [], 'No se pudieron cargar las conversaciones de WhatsApp.', onError)
    });
  }

  private cargarSi<T>(
    permitido: boolean,
    peticion$: Observable<T>,
    fallback: T,
    mensaje: string,
    onError: AdminDashboardLoadErrorHandler
  ): Observable<T> {
    return permitido ? this.conFallback(peticion$, fallback, mensaje, onError) : of(fallback);
  }

  private conFallback<T>(
    peticion$: Observable<T>,
    fallback: T,
    mensaje: string,
    onError: AdminDashboardLoadErrorHandler
  ): Observable<T> {
    return peticion$.pipe(
      catchError(error => {
        onError(error, mensaje);
        return of(fallback);
      })
    );
  }
}
