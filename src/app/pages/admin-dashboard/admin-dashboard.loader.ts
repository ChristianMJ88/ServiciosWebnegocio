import { Injectable, inject } from '@angular/core';
import { Observable, catchError, forkJoin, of } from 'rxjs';
import { AdminService } from '../../core/admin/admin.service';
import { AuthService } from '../../core/auth/auth.service';
import { PERMISOS } from '../../core/auth/permissions';
import { SystemParametersService } from '../../core/admin/system-parameters.service';

export type AdminDashboardLoadErrorHandler = (error: unknown, fallbackMessage: string) => void;

@Injectable({ providedIn: 'root' })
export class AdminDashboardLoader {
  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly systemParameters = inject(SystemParametersService);

  cargar(desdeAgenda: string, hastaAgenda: string, onError: AdminDashboardLoadErrorHandler) {
    const puedeGestionarUsuarios = this.authService.puedeGestionarUsuariosInternos();
    const puedeGestionarPrestadores = this.authService.puedeGestionarPrestadores();
    const puedeGestionarWhatsapp = this.authService.puedeGestionarWhatsapp();
    const puedeGestionarConfiguracion = this.authService.puedeGestionarConfiguracionEmpresa();
    const puedeVerReportes = this.authService.puedeVerReportesAdmin();

    return forkJoin({
      // El resumen y los reportes se cargan juntos cuando se resuelve el periodo activo.
      // Evita solicitar dos veces los mismos datos durante la carga inicial.
      resumen: puedeVerReportes
        ? of(null)
        : this.conFallback(this.adminService.getResumen(), null, 'No se pudo cargar el resumen.', onError),
      citas: this.cargarSi(
        this.authService.puedeGestionarCitasAdmin(),
        this.adminService.getCitas(desdeAgenda, hastaAgenda),
        [],
        'No se pudieron cargar las citas.',
        onError
      ),
      resumenContactos: this.cargarSi(
        this.authService.puedeVerContactosAdmin(),
        this.adminService.getResumenContactos(),
        null,
        'No se pudo cargar el resumen de contactos.',
        onError
      ),
      contactos: of([]),
      metadatosContactos: this.cargarSi(
        this.authService.puedeVerContactosAdmin(),
        this.adminService.getMetadatosContactos(),
        null,
        'No se pudo cargar el catálogo de estados de contacto.',
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
      gruposServicio: of([]),
      subgruposServicio: of([]),
      catalogosSugeridos: of([]),
      servicios: this.cargarSi(
        this.authService.puedeGestionarServicios() || puedeGestionarPrestadores,
        this.adminService.getServicios(),
        [],
        'No se pudieron cargar los servicios.',
        onError
      ),
      prestadores: this.cargarSi(puedeGestionarPrestadores, this.adminService.getPrestadores(), [], 'No se pudieron cargar los prestadores.', onError),
      rolesInternos: of([]),
      plantillasRolesInternos: of([]),
      auditoriaRolesInternos: of([]),
      permisos: of([]),
      usuariosInternos: of([]),
      reglas: of([]),
      metadatosDisponibilidad: of(null),
      excepciones: of([]),
      reporteServicios: of([]),
      reportePrestadores: of([]),
      periodosReporte: this.cargarSi(puedeVerReportes, this.adminService.getPeriodosReporte(), [], 'No se pudieron cargar los periodos de reporte.', onError),
      configuracionSitio: of(null),
      configuracionCorreo: of(null),
      auditoriaConfiguracion: this.cargarSi(
        puedeGestionarConfiguracion || puedeGestionarWhatsapp,
        this.adminService.getAuditoriaConfiguracion(),
        [],
        'No se pudo cargar la auditoría de configuración.',
        onError
      ),
      // WhatsApp se carga al entrar a sus secciones para no descargar configuración,
      // trazabilidad y conversaciones durante cada apertura del panel.
      configuracionWhatsapp: of(null),
      plantillasWhatsapp: of([]),
      plantillasWhatsappEmpresa: of([]),
      logsWhatsapp: of([]),
      mensajesWhatsapp: of([]),
      parametrosSistema: this.cargarSi(
        this.authService.tienePermiso(PERMISOS.parametrosSistemaGestionar),
        this.systemParameters.listar(),
        [],
        'No se pudieron cargar los parámetros del negocio.',
        onError
      )
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
