import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CitaCliente } from '../auth/client-appointments.service';

export interface ResumenAdmin {
  totalCitas: number;
  pendientes: number;
  confirmadas: number;
  finalizadas: number;
  canceladas: number;
  noAsistio: number;
  citasHoy: number;
  ingresosProgramados: number;
  ingresosFinalizados: number;
}

export interface PeriodoReporteAdmin {
  codigo: string;
  etiqueta: string;
  desde: string | null;
  hasta: string | null;
  predeterminado: boolean;
}

export interface OpcionTextoDisponibilidadAdmin {
  valor: string;
  etiqueta: string;
}

export interface OpcionNumeroDisponibilidadAdmin {
  valor: number;
  etiqueta: string;
}

export interface MetadatosDisponibilidadAdmin {
  tiposSujeto: OpcionTextoDisponibilidadAdmin[];
  diasSemana: OpcionNumeroDisponibilidadAdmin[];
  tiposBloqueo: OpcionTextoDisponibilidadAdmin[];
  intervaloMinimoMinutos: number;
}

export interface SucursalAdmin {
  id: number;
  nombre: string;
  direccion: string | null;
  telefono: string | null;
  zonaHoraria: string;
  activa: boolean;
}

export interface GuardarSucursalPayload {
  nombre: string;
  direccion: string | null;
  telefono: string | null;
  zonaHoraria: string;
  activa: boolean;
}

export interface ServicioAdmin {
  id: number;
  sucursalId: number;
  sucursalNombre: string;
  sucursalIds: number[];
  sucursalNombres: string[];
  grupoId: number | null;
  grupoNombre: string | null;
  subgrupoId: number | null;
  subgrupoNombre: string | null;
  nombre: string;
  slug: string;
  descripcion: string | null;
  imagenUrl: string | null;
  duracionMinutos: number;
  bufferAntesMinutos: number;
  bufferDespuesMinutos: number;
  precio: number;
  moneda: string;
  ordenPublico: number;
  visiblePublico: boolean;
  requiereAnticipo: boolean;
  anticipoTipo: string | null;
  anticipoValor: number | null;
  activo: boolean;
}

export interface GuardarServicioPayload {
  sucursalId: number;
  sucursalIds: number[];
  grupoId: number | null;
  subgrupoId: number | null;
  nombre: string;
  slug: string | null;
  descripcion: string | null;
  imagenUrl: string | null;
  duracionMinutos: number;
  bufferAntesMinutos: number;
  bufferDespuesMinutos: number;
  precio: number;
  moneda: string;
  ordenPublico: number;
  visiblePublico: boolean;
  requiereAnticipo: boolean;
  anticipoTipo: string | null;
  anticipoValor: number | null;
  activo: boolean;
}

export interface GrupoServicioAdmin {
  id: number;
  nombre: string;
  slug: string;
  descripcion: string | null;
  imagenUrl: string | null;
  icono: string | null;
  ordenPublico: number;
  activo: boolean;
}

export interface GuardarGrupoServicioPayload {
  nombre: string;
  slug: string | null;
  descripcion: string | null;
  imagenUrl: string | null;
  icono: string | null;
  ordenPublico: number;
  activo: boolean;
}

export interface SubgrupoServicioAdmin {
  id: number;
  grupoId: number;
  grupoNombre: string;
  nombre: string;
  slug: string;
  descripcion: string | null;
  ordenPublico: number;
  activo: boolean;
}

export interface GuardarSubgrupoServicioPayload {
  grupoId: number | null;
  nombre: string;
  slug: string | null;
  descripcion: string | null;
  ordenPublico: number;
  activo: boolean;
}

export interface CatalogoSugeridoAdmin {
  id: string;
  nombre: string;
  descripcion: string;
  grupos: number;
  subgrupos: number;
  servicios: number;
}

export interface ImportarCatalogoSugeridoPayload {
  sugerenciaId: string;
  sucursalId: number | null;
}

export interface ImportarCatalogoSugeridoResponse {
  sugerenciaId: string;
  sugerenciaNombre: string;
  gruposCreados: number;
  subgruposCreados: number;
  serviciosCreados: number;
  serviciosReutilizados: number;
  mensaje: string;
}

export interface PrestadorAdmin {
  usuarioId: number;
  sucursalId: number;
  sucursalNombre: string;
  correo: string;
  nombreMostrar: string;
  biografia: string | null;
  colorAgenda: string | null;
  activo: boolean;
  servicioIds: number[];
  servicioNombres: string[];
}

export interface GuardarPrestadorPayload {
  sucursalId: number;
  correo: string;
  contrasenaTemporal: string | null;
  nombreMostrar: string;
  biografia: string | null;
  colorAgenda: string | null;
  activo: boolean;
  servicioIds: number[];
}

export interface UsuarioInternoAdmin {
  usuarioId: number;
  sucursalId: number | null;
  sucursalNombre: string | null;
  sucursalIds: number[];
  sucursalNombresScope: string[];
  correo: string;
  nombreCompleto: string;
  telefono: string | null;
  puesto: string | null;
  rolEmpresaId: number | null;
  rolCodigo: string;
  rolNombre: string | null;
  permisosRol: string[];
  permisosDirectos: string[];
  permisosEfectivos: string[];
  activo: boolean;
  notas: string | null;
}

export interface RolInternoAdmin {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  editable: boolean;
  usuariosAsignados: number;
  sePuedeEliminar: boolean;
  permisos: string[];
}

export interface PlantillaRolInternoAdmin {
  codigoSugerido: string;
  nombreSugerido: string;
  descripcion: string;
  categoria: string;
  permisos: string[];
}

export interface AuditoriaRolInternoAdmin {
  id: number;
  accion: string;
  resumen: string;
  actorCorreo: string | null;
  rolCodigo: string | null;
  rolNombre: string | null;
  creadoEn: string;
}

export interface AuditoriaConfiguracionAdmin {
  id: number;
  modulo: string;
  accion: string;
  resumen: string;
  actorCorreo: string | null;
  creadoEn: string;
}

export interface GuardarRolInternoPayload {
  codigo: string;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  permisos: string[];
}

export interface PermisoAdmin {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
}

export interface GuardarUsuarioInternoPayload {
  sucursalId: number | null;
  sucursalIds: number[];
  correo: string;
  contrasenaTemporal: string | null;
  nombreCompleto: string;
  telefono: string | null;
  puesto: string | null;
  rolEmpresaId: number | null;
  permisosDirectos: string[];
  activo: boolean;
  notas: string | null;
}

export interface ReglaDisponibilidadAdmin {
  id: number;
  tipoSujeto: string;
  sujetoId: number;
  sujetoNombre: string;
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
  intervaloMinutos: number;
  vigenteDesde: string | null;
  vigenteHasta: string | null;
}

export interface GuardarReglaDisponibilidadPayload {
  tipoSujeto: string;
  sujetoId: number;
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
  intervaloMinutos: number;
  vigenteDesde: string | null;
  vigenteHasta: string | null;
}

export interface ExcepcionDisponibilidadAdmin {
  id: number;
  tipoSujeto: string;
  sujetoId: number;
  sujetoNombre: string;
  fechaExcepcion: string;
  horaInicio: string | null;
  horaFin: string | null;
  tipoBloqueo: string;
  motivo: string | null;
}

export interface GuardarExcepcionDisponibilidadPayload {
  tipoSujeto: string;
  sujetoId: number;
  fechaExcepcion: string;
  horaInicio: string | null;
  horaFin: string | null;
  tipoBloqueo: string;
  motivo: string | null;
}

export interface ReporteServicioAdmin {
  servicioId: number;
  servicioNombre: string;
  totalCitas: number;
  pendientes: number;
  confirmadas: number;
  finalizadas: number;
  ingresosProgramados: number;
  ingresosFinalizados: number;
}

export interface ReportePrestadorAdmin {
  prestadorId: number;
  prestadorNombre: string;
  totalCitas: number;
  pendientes: number;
  confirmadas: number;
  finalizadas: number;
  canceladas: number;
  noAsistio: number;
  ingresosProgramados: number;
  ingresosFinalizados: number;
  ticketPromedio: number;
}

export interface ConfiguracionSitioAdmin {
  empresaId: number;
  slug: string;
  nombreComercial: string;
  dominioPrincipal: string | null;
  logoUrl: string | null;
  descripcionCorta: string | null;
  colorPrimario: string | null;
  colorSecundario: string | null;
  fuenteTitulos: string | null;
  fuenteCuerpo: string | null;
  heroTitulo: string | null;
  heroSubtitulo: string | null;
  heroImagenUrl: string | null;
  whatsapp: string | null;
  telefono: string | null;
  correo: string | null;
  direccion: string | null;
  instagramUrl: string | null;
  facebookUrl: string | null;
  tema: string | null;
  publicado: boolean;
}

export interface GuardarConfiguracionSitioPayload {
  slug: string;
  nombreComercial: string;
  dominioPrincipal: string | null;
  logoUrl: string | null;
  descripcionCorta: string | null;
  colorPrimario: string | null;
  colorSecundario: string | null;
  fuenteTitulos: string | null;
  fuenteCuerpo: string | null;
  heroTitulo: string | null;
  heroSubtitulo: string | null;
  heroImagenUrl: string | null;
  whatsapp: string | null;
  telefono: string | null;
  correo: string | null;
  direccion: string | null;
  instagramUrl: string | null;
  facebookUrl: string | null;
  tema: string | null;
  publicado: boolean;
}

export interface ConfiguracionCorreoAdmin {
  habilitado: boolean;
  proveedor: 'SMTP' | 'GRAPH' | string | null;
  remitente: string | null;
  nombreRemitente: string | null;
  responderA: string | null;
  smtpHost: string | null;
  smtpPort: number | null;
  smtpUsername: string | null;
  smtpPasswordConfigurada: boolean;
  smtpPasswordCifrada: boolean;
  requiereMigracionSecretos: boolean;
  smtpAuth: boolean | null;
  smtpStartTls: boolean | null;
  graphTenantId: string | null;
  graphClientId: string | null;
  graphUserId: string | null;
  graphClientSecretConfigurado: boolean;
  graphClientSecretCifrado: boolean;
  graphCertificateThumbprint: string | null;
  graphPrivateKeyConfigurada: boolean;
  graphPrivateKeyCifrada: boolean;
  graphOauthConectado?: boolean;
  graphOauthConectadoEn?: string | null;
}

export interface GuardarConfiguracionCorreoPayload {
  habilitado: boolean;
  proveedor: 'SMTP' | 'GRAPH' | string | null;
  remitente: string | null;
  nombreRemitente: string | null;
  responderA: string | null;
  smtpHost: string | null;
  smtpPort: number | null;
  smtpUsername: string | null;
  smtpPassword: string | null;
  smtpAuth: boolean | null;
  smtpStartTls: boolean | null;
  graphTenantId: string | null;
  graphClientId: string | null;
  graphClientSecret: string | null;
  graphUserId: string | null;
  graphCertificateThumbprint: string | null;
  graphPrivateKeyPem: string | null;
}

export interface MigracionSecretosCorreoResponse {
  actualizada: boolean;
  mensaje: string;
}

export interface ConfiguracionWhatsappAdmin {
  habilitado: boolean;
  accountSid: string | null;
  authTokenConfigurado: boolean;
  tipoCuentaTwilio: string | null;
  subaccountSid: string | null;
  numeroRemitente: string | null;
  messagingServiceSid: string | null;
  channelSenderSid: string | null;
  statusCallbackUrl: string | null;
  plantillaSolicitudConfirmacionSid: string | null;
  plantillaReprogramadaPendienteSid: string | null;
  plantillaRecordatorioConfirmacionSid: string | null;
  plantillaCitaConfirmadaSid: string | null;
  plantillaRecordatorioSid: string | null;
  plantillaCancelacionSid: string | null;
  plantillaLiberadaSinConfirmacionSid: string | null;
  plantillaGraciasVisitaSid: string | null;
  plantillaRecordatorioRegresoSid: string | null;
  plantillaEspacioDisponibleWalkinSid: string | null;
  plantillaMenuBienvenidaSid: string | null;
  plantillasListPickerSids: string | null;
  senderDisplayName: string | null;
  senderPhoneNumber: string | null;
  senderStatus: string | null;
  qualityRating: string | null;
  throughputMps: number | null;
  wabaId: string | null;
  metaBusinessManagerId: string | null;
}

export interface GuardarConfiguracionWhatsappPayload {
  habilitado: boolean;
  accountSid: string | null;
  authToken: string | null;
  tipoCuentaTwilio: string | null;
  subaccountSid: string | null;
  numeroRemitente: string | null;
  messagingServiceSid: string | null;
  channelSenderSid: string | null;
  statusCallbackUrl: string | null;
  plantillaSolicitudConfirmacionSid: string | null;
  plantillaReprogramadaPendienteSid: string | null;
  plantillaRecordatorioConfirmacionSid: string | null;
  plantillaCitaConfirmadaSid: string | null;
  plantillaRecordatorioSid: string | null;
  plantillaCancelacionSid: string | null;
  plantillaLiberadaSinConfirmacionSid: string | null;
  plantillaGraciasVisitaSid: string | null;
  plantillaRecordatorioRegresoSid: string | null;
  plantillaEspacioDisponibleWalkinSid: string | null;
  plantillaMenuBienvenidaSid: string | null;
  plantillasListPickerSids: string | null;
  senderDisplayName: string | null;
  senderPhoneNumber: string | null;
  senderStatus: string | null;
  qualityRating: string | null;
  throughputMps: number | null;
  wabaId: string | null;
  metaBusinessManagerId: string | null;
}

export interface PlantillaWhatsappAdmin {
  sid: string;
  nombre: string;
  idioma: string | null;
  categoria: string | null;
  estado: string | null;
  tipoPlantilla: string | null;
}

export interface PlantillaWhatsappEmpresaAdmin {
  id: number;
  nombre: string;
  uso: string;
  contentSid: string;
  tipoContenido: string | null;
  categoria: string | null;
  estado: string | null;
  activa: boolean;
  creadaEn: string | null;
  actualizadaEn: string | null;
}

export interface GuardarPlantillaWhatsappEmpresaPayload {
  nombre: string;
  uso: string;
  contentSid: string;
  tipoContenido: string | null;
  categoria: string | null;
  estado: string | null;
  activa: boolean;
}

export interface LogMensajeWhatsappAdmin {
  id: number;
  agregadoId: number;
  tipoEvento: string;
  estado: string;
  estadoEntrega: string | null;
  proveedorMensajeId: string | null;
  destinatario: string | null;
  plantillaSid: string | null;
  codigoErrorProveedor: string | null;
  detalleErrorProveedor: string | null;
  enviadaEn: string | null;
  estadoEntregaActualizadoEn: string | null;
}

export interface MensajeWhatsappAdmin {
  id: number;
  telefono: string;
  direccion: 'ENTRANTE' | 'SALIENTE' | string;
  cuerpo: string | null;
  contentSid: string | null;
  proveedorMensajeId: string | null;
  estado: string | null;
  codigoErrorProveedor: string | null;
  detalleErrorProveedor: string | null;
  creadoEn: string;
}

export interface EnviarMensajeWhatsappPayload {
  telefono: string;
  mensaje: string;
}

export interface ProbarPlantillaWhatsappPayload {
  telefonoDestino: string;
  nombreCliente: string;
  fecha: string;
  hora: string;
  plantillaSid: string | null;
}

export interface PruebaWhatsappResponse {
  programado: boolean;
  mensaje: string;
}

export interface ProvisionarSubcuentaWhatsappPayload {
  friendlyName: string | null;
}

export interface ProvisionarSubcuentaWhatsappResponse {
  creada: boolean;
  mensaje: string;
  friendlyName: string | null;
  subaccountSid: string | null;
  estado: string | null;
  configuracion: ConfiguracionWhatsappAdmin;
}

export interface ProvisionarMessagingServiceWhatsappPayload {
  friendlyName: string | null;
  inboundRequestUrl: string | null;
}

export interface ProvisionarMessagingServiceWhatsappResponse {
  creado: boolean;
  mensaje: string;
  friendlyName: string | null;
  messagingServiceSid: string | null;
  inboundRequestUrl: string | null;
  configuracion: ConfiguracionWhatsappAdmin;
}

export interface AsociarChannelSenderWhatsappPayload {
  channelSenderSid: string;
}

export interface AsociarChannelSenderWhatsappResponse {
  asociado: boolean;
  mensaje: string;
  messagingServiceSid: string | null;
  channelSenderSid: string | null;
  configuracion: ConfiguracionWhatsappAdmin;
}

export interface DetectarChannelSenderWhatsappResponse {
  encontrado: boolean;
  mensaje: string;
  channelSenderSid: string | null;
  senderId: string | null;
  senderStatus: string | null;
  displayName: string | null;
  wabaId: string | null;
  configuracion: ConfiguracionWhatsappAdmin;
}

export interface OnboardingWhatsappResponse {
  onboardingId: string;
  estado: string;
  pasoActual: string | null;
  telefonoE164: string | null;
  displayName: string | null;
  wabaId: string | null;
  phoneNumberId: string | null;
  channelSenderSid: string | null;
  ultimoError: string | null;
  actualizadoEn: string | null;
  embeddedSignupDisponible: boolean;
  metaAppId: string | null;
  configurationId: string | null;
  partnerSolutionId: string | null;
}

export interface IniciarOnboardingWhatsappPayload {
  telefonoE164: string;
  displayName: string;
}

export interface CompletarOnboardingWhatsappPayload {
  onboardingId: string;
  wabaId: string;
  phoneNumberId: string | null;
  telefonoE164: string;
}

export interface SolicitudContactoAdmin {
  id: number;
  empresaId: number;
  nombreCompleto: string;
  telefono: string | null;
  correo: string;
  asunto: string;
  mensaje: string;
  canal: string;
  estado: 'NUEVO' | 'EN_PROCESO' | 'ATENDIDO' | 'CERRADO' | string;
  notificacionCorreoProgramada: boolean;
  notificadaEn: string | null;
  creadaEn: string;
}

export interface EstadoContactoAdmin {
  codigo: string;
  etiqueta: string;
}

export interface MetadatosContactosAdmin {
  estados: EstadoContactoAdmin[];
  estadoNuevo: string;
  estadoEnProceso: string;
  estadoAtendido: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly http = inject(HttpClient);

  getResumen(periodo?: PeriodoReporteAdmin | null): Observable<ResumenAdmin> {
    return this.http.get<ResumenAdmin>(`${environment.apiBaseUrl}/admin/resumen`, { params: this.paramsPeriodo(periodo) });
  }

  getCitas(): Observable<CitaCliente[]> {
    return this.http.get<CitaCliente[]>(`${environment.apiBaseUrl}/admin/citas`);
  }

  confirmarCita(citaId: number): Observable<void> {
    return this.http.patch<void>(`${environment.apiBaseUrl}/admin/citas/${citaId}/confirmar`, {});
  }

  finalizarCita(citaId: number): Observable<void> {
    return this.http.patch<void>(`${environment.apiBaseUrl}/admin/citas/${citaId}/finalizar`, {});
  }

  marcarNoAsistio(citaId: number): Observable<void> {
    return this.http.patch<void>(`${environment.apiBaseUrl}/admin/citas/${citaId}/no-asistio`, {});
  }

  cancelarCita(citaId: number): Observable<void> {
    return this.http.patch<void>(`${environment.apiBaseUrl}/admin/citas/${citaId}/cancelar`, {});
  }

  getSucursales(): Observable<SucursalAdmin[]> {
    return this.http.get<SucursalAdmin[]>(`${environment.apiBaseUrl}/admin/sucursales`);
  }

  crearSucursal(payload: GuardarSucursalPayload): Observable<SucursalAdmin> {
    return this.http.post<SucursalAdmin>(`${environment.apiBaseUrl}/admin/sucursales`, payload);
  }

  actualizarSucursal(id: number, payload: GuardarSucursalPayload): Observable<SucursalAdmin> {
    return this.http.patch<SucursalAdmin>(`${environment.apiBaseUrl}/admin/sucursales/${id}`, payload);
  }

  getServicios(): Observable<ServicioAdmin[]> {
    return this.http.get<ServicioAdmin[]>(`${environment.apiBaseUrl}/admin/servicios`);
  }

  getGruposServicio(): Observable<GrupoServicioAdmin[]> {
    return this.http.get<GrupoServicioAdmin[]>(`${environment.apiBaseUrl}/admin/grupos-servicio`);
  }

  crearGrupoServicio(payload: GuardarGrupoServicioPayload): Observable<GrupoServicioAdmin> {
    return this.http.post<GrupoServicioAdmin>(`${environment.apiBaseUrl}/admin/grupos-servicio`, payload);
  }

  actualizarGrupoServicio(id: number, payload: GuardarGrupoServicioPayload): Observable<GrupoServicioAdmin> {
    return this.http.patch<GrupoServicioAdmin>(`${environment.apiBaseUrl}/admin/grupos-servicio/${id}`, payload);
  }

  getSubgruposServicio(): Observable<SubgrupoServicioAdmin[]> {
    return this.http.get<SubgrupoServicioAdmin[]>(`${environment.apiBaseUrl}/admin/subgrupos-servicio`);
  }

  crearSubgrupoServicio(payload: GuardarSubgrupoServicioPayload): Observable<SubgrupoServicioAdmin> {
    return this.http.post<SubgrupoServicioAdmin>(`${environment.apiBaseUrl}/admin/subgrupos-servicio`, payload);
  }

  actualizarSubgrupoServicio(id: number, payload: GuardarSubgrupoServicioPayload): Observable<SubgrupoServicioAdmin> {
    return this.http.patch<SubgrupoServicioAdmin>(`${environment.apiBaseUrl}/admin/subgrupos-servicio/${id}`, payload);
  }

  getCatalogosSugeridos(): Observable<CatalogoSugeridoAdmin[]> {
    return this.http.get<CatalogoSugeridoAdmin[]>(`${environment.apiBaseUrl}/admin/catalogos-sugeridos`);
  }

  importarCatalogoSugerido(payload: ImportarCatalogoSugeridoPayload): Observable<ImportarCatalogoSugeridoResponse> {
    return this.http.post<ImportarCatalogoSugeridoResponse>(`${environment.apiBaseUrl}/admin/catalogos-sugeridos/importar`, payload);
  }

  crearServicio(payload: GuardarServicioPayload): Observable<ServicioAdmin> {
    return this.http.post<ServicioAdmin>(`${environment.apiBaseUrl}/admin/servicios`, payload);
  }

  actualizarServicio(id: number, payload: GuardarServicioPayload): Observable<ServicioAdmin> {
    return this.http.patch<ServicioAdmin>(`${environment.apiBaseUrl}/admin/servicios/${id}`, payload);
  }

  getPrestadores(): Observable<PrestadorAdmin[]> {
    return this.http.get<PrestadorAdmin[]>(`${environment.apiBaseUrl}/admin/prestadores`);
  }

  getUsuariosInternos(): Observable<UsuarioInternoAdmin[]> {
    return this.http.get<UsuarioInternoAdmin[]>(`${environment.apiBaseUrl}/admin/usuarios-internos`);
  }

  getRolesInternos(): Observable<RolInternoAdmin[]> {
    return this.http.get<RolInternoAdmin[]>(`${environment.apiBaseUrl}/admin/roles-internos`);
  }

  getPlantillasRolesInternos(): Observable<PlantillaRolInternoAdmin[]> {
    return this.http.get<PlantillaRolInternoAdmin[]>(`${environment.apiBaseUrl}/admin/roles-internos/plantillas`);
  }

  getAuditoriaRolesInternos(): Observable<AuditoriaRolInternoAdmin[]> {
    return this.http.get<AuditoriaRolInternoAdmin[]>(`${environment.apiBaseUrl}/admin/roles-internos/auditoria`);
  }

  getAuditoriaConfiguracion(): Observable<AuditoriaConfiguracionAdmin[]> {
    return this.http.get<AuditoriaConfiguracionAdmin[]>(`${environment.apiBaseUrl}/admin/configuracion/auditoria`);
  }

  getPermisos(): Observable<PermisoAdmin[]> {
    return this.http.get<PermisoAdmin[]>(`${environment.apiBaseUrl}/admin/permisos`);
  }

  crearRolInterno(payload: GuardarRolInternoPayload): Observable<RolInternoAdmin> {
    return this.http.post<RolInternoAdmin>(`${environment.apiBaseUrl}/admin/roles-internos`, payload);
  }

  actualizarRolInterno(id: number, payload: GuardarRolInternoPayload): Observable<RolInternoAdmin> {
    return this.http.patch<RolInternoAdmin>(`${environment.apiBaseUrl}/admin/roles-internos/${id}`, payload);
  }

  clonarRolInterno(id: number, payload: GuardarRolInternoPayload): Observable<RolInternoAdmin> {
    return this.http.post<RolInternoAdmin>(`${environment.apiBaseUrl}/admin/roles-internos/${id}/clonar`, payload);
  }

  eliminarRolInterno(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/admin/roles-internos/${id}`);
  }

  crearUsuarioInterno(payload: GuardarUsuarioInternoPayload): Observable<UsuarioInternoAdmin> {
    return this.http.post<UsuarioInternoAdmin>(`${environment.apiBaseUrl}/admin/usuarios-internos`, payload);
  }

  actualizarUsuarioInterno(id: number, payload: GuardarUsuarioInternoPayload): Observable<UsuarioInternoAdmin> {
    return this.http.patch<UsuarioInternoAdmin>(`${environment.apiBaseUrl}/admin/usuarios-internos/${id}`, payload);
  }

  crearPrestador(payload: GuardarPrestadorPayload): Observable<PrestadorAdmin> {
    return this.http.post<PrestadorAdmin>(`${environment.apiBaseUrl}/admin/prestadores`, payload);
  }

  actualizarPrestador(id: number, payload: GuardarPrestadorPayload): Observable<PrestadorAdmin> {
    return this.http.patch<PrestadorAdmin>(`${environment.apiBaseUrl}/admin/prestadores/${id}`, payload);
  }

  getReglasDisponibilidad(): Observable<ReglaDisponibilidadAdmin[]> {
    return this.http.get<ReglaDisponibilidadAdmin[]>(`${environment.apiBaseUrl}/admin/disponibilidad/reglas`);
  }

  getMetadatosDisponibilidad(): Observable<MetadatosDisponibilidadAdmin> {
    return this.http.get<MetadatosDisponibilidadAdmin>(`${environment.apiBaseUrl}/admin/disponibilidad/metadatos`);
  }

  crearReglaDisponibilidad(payload: GuardarReglaDisponibilidadPayload): Observable<ReglaDisponibilidadAdmin> {
    return this.http.post<ReglaDisponibilidadAdmin>(`${environment.apiBaseUrl}/admin/disponibilidad/reglas`, payload);
  }

  actualizarReglaDisponibilidad(id: number, payload: GuardarReglaDisponibilidadPayload): Observable<ReglaDisponibilidadAdmin> {
    return this.http.patch<ReglaDisponibilidadAdmin>(`${environment.apiBaseUrl}/admin/disponibilidad/reglas/${id}`, payload);
  }

  getExcepcionesDisponibilidad(): Observable<ExcepcionDisponibilidadAdmin[]> {
    return this.http.get<ExcepcionDisponibilidadAdmin[]>(`${environment.apiBaseUrl}/admin/disponibilidad/excepciones`);
  }

  crearExcepcionDisponibilidad(payload: GuardarExcepcionDisponibilidadPayload): Observable<ExcepcionDisponibilidadAdmin> {
    return this.http.post<ExcepcionDisponibilidadAdmin>(`${environment.apiBaseUrl}/admin/disponibilidad/excepciones`, payload);
  }

  actualizarExcepcionDisponibilidad(id: number, payload: GuardarExcepcionDisponibilidadPayload): Observable<ExcepcionDisponibilidadAdmin> {
    return this.http.patch<ExcepcionDisponibilidadAdmin>(`${environment.apiBaseUrl}/admin/disponibilidad/excepciones/${id}`, payload);
  }

  getReporteServicios(periodo?: PeriodoReporteAdmin | null): Observable<ReporteServicioAdmin[]> {
    return this.http.get<ReporteServicioAdmin[]>(`${environment.apiBaseUrl}/admin/reportes/servicios`, { params: this.paramsPeriodo(periodo) });
  }

  getReportePrestadores(periodo?: PeriodoReporteAdmin | null): Observable<ReportePrestadorAdmin[]> {
    return this.http.get<ReportePrestadorAdmin[]>(`${environment.apiBaseUrl}/admin/reportes/prestadores`, { params: this.paramsPeriodo(periodo) });
  }

  getPeriodosReporte(): Observable<PeriodoReporteAdmin[]> {
    return this.http.get<PeriodoReporteAdmin[]>(`${environment.apiBaseUrl}/admin/reportes/periodos`);
  }

  getConfiguracionSitio(): Observable<ConfiguracionSitioAdmin> {
    return this.http.get<ConfiguracionSitioAdmin>(`${environment.apiBaseUrl}/admin/configuracion-sitio`);
  }

  actualizarConfiguracionSitio(payload: GuardarConfiguracionSitioPayload): Observable<ConfiguracionSitioAdmin> {
    return this.http.patch<ConfiguracionSitioAdmin>(`${environment.apiBaseUrl}/admin/configuracion-sitio`, payload);
  }

  private paramsPeriodo(periodo?: PeriodoReporteAdmin | null): HttpParams {
    let params = new HttpParams();
    if (periodo?.desde && periodo?.hasta) {
      params = params.set('desde', periodo.desde).set('hasta', periodo.hasta);
    }
    return params;
  }

  getConfiguracionCorreo(): Observable<ConfiguracionCorreoAdmin> {
    return this.http.get<ConfiguracionCorreoAdmin>(`${environment.apiBaseUrl}/admin/configuracion-correo`);
  }

  actualizarConfiguracionCorreo(payload: GuardarConfiguracionCorreoPayload): Observable<ConfiguracionCorreoAdmin> {
    return this.http.patch<ConfiguracionCorreoAdmin>(`${environment.apiBaseUrl}/admin/configuracion-correo`, payload);
  }

  usarCorreoPlataforma(): Observable<ConfiguracionCorreoAdmin> {
    return this.http.delete<ConfiguracionCorreoAdmin>(`${environment.apiBaseUrl}/admin/configuracion-correo`);
  }

  migrarSecretosCorreo(): Observable<MigracionSecretosCorreoResponse> {
    return this.http.post<MigracionSecretosCorreoResponse>(`${environment.apiBaseUrl}/admin/configuracion-correo/migrar-secretos`, {});
  }

  iniciarOAuthCorreoMicrosoft(): Observable<{ urlAutorizacion: string }> {
    return this.http.get<{ urlAutorizacion: string }>(`${environment.apiBaseUrl}/admin/correo/oauth/microsoft/iniciar`);
  }

  iniciarOAuthCorreoGoogle(): Observable<{ urlAutorizacion: string }> {
    return this.http.get<{ urlAutorizacion: string }>(`${environment.apiBaseUrl}/admin/correo/oauth/google/iniciar`);
  }

  getConfiguracionWhatsapp(): Observable<ConfiguracionWhatsappAdmin> {
    return this.http.get<ConfiguracionWhatsappAdmin>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp`);
  }

  actualizarConfiguracionWhatsapp(payload: GuardarConfiguracionWhatsappPayload): Observable<ConfiguracionWhatsappAdmin> {
    return this.http.patch<ConfiguracionWhatsappAdmin>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp`, payload);
  }

  getPlantillasWhatsapp(): Observable<PlantillaWhatsappAdmin[]> {
    return this.http.get<PlantillaWhatsappAdmin[]>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/plantillas`);
  }

  getPlantillasWhatsappEmpresa(): Observable<PlantillaWhatsappEmpresaAdmin[]> {
    return this.http.get<PlantillaWhatsappEmpresaAdmin[]>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/plantillas-empresa`);
  }

  crearPlantillaWhatsappEmpresa(payload: GuardarPlantillaWhatsappEmpresaPayload): Observable<PlantillaWhatsappEmpresaAdmin> {
    return this.http.post<PlantillaWhatsappEmpresaAdmin>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/plantillas-empresa`, payload);
  }

  actualizarPlantillaWhatsappEmpresa(id: number, payload: GuardarPlantillaWhatsappEmpresaPayload): Observable<PlantillaWhatsappEmpresaAdmin> {
    return this.http.patch<PlantillaWhatsappEmpresaAdmin>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/plantillas-empresa/${id}`, payload);
  }

  eliminarPlantillaWhatsappEmpresa(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/plantillas-empresa/${id}`);
  }

  getLogsWhatsapp(): Observable<LogMensajeWhatsappAdmin[]> {
    return this.http.get<LogMensajeWhatsappAdmin[]>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/logs`);
  }

  getMensajesWhatsapp(): Observable<MensajeWhatsappAdmin[]> {
    return this.http.get<MensajeWhatsappAdmin[]>(`${environment.apiBaseUrl}/admin/whatsapp/mensajes`);
  }

  enviarMensajeWhatsapp(payload: EnviarMensajeWhatsappPayload): Observable<MensajeWhatsappAdmin> {
    return this.http.post<MensajeWhatsappAdmin>(`${environment.apiBaseUrl}/admin/whatsapp/mensajes`, payload);
  }

  probarPlantillaWhatsapp(payload: ProbarPlantillaWhatsappPayload): Observable<PruebaWhatsappResponse> {
    return this.http.post<PruebaWhatsappResponse>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/probar-plantilla`, payload);
  }

  provisionarSubcuentaWhatsapp(payload: ProvisionarSubcuentaWhatsappPayload): Observable<ProvisionarSubcuentaWhatsappResponse> {
    return this.http.post<ProvisionarSubcuentaWhatsappResponse>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/provisionar-subcuenta`, payload);
  }

  provisionarMessagingServiceWhatsapp(payload: ProvisionarMessagingServiceWhatsappPayload): Observable<ProvisionarMessagingServiceWhatsappResponse> {
    return this.http.post<ProvisionarMessagingServiceWhatsappResponse>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/provisionar-messaging-service`, payload);
  }

  asociarChannelSenderWhatsapp(payload: AsociarChannelSenderWhatsappPayload): Observable<AsociarChannelSenderWhatsappResponse> {
    return this.http.post<AsociarChannelSenderWhatsappResponse>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/asociar-channel-sender`, payload);
  }

  detectarChannelSenderWhatsapp(): Observable<DetectarChannelSenderWhatsappResponse> {
    return this.http.post<DetectarChannelSenderWhatsappResponse>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/detectar-channel-sender`, {});
  }

  getEstadoOnboardingWhatsapp(): Observable<OnboardingWhatsappResponse> {
    return this.http.get<OnboardingWhatsappResponse>(`${environment.apiBaseUrl}/admin/whatsapp/onboarding/estado`);
  }

  iniciarOnboardingWhatsapp(payload: IniciarOnboardingWhatsappPayload): Observable<OnboardingWhatsappResponse> {
    return this.http.post<OnboardingWhatsappResponse>(`${environment.apiBaseUrl}/admin/whatsapp/onboarding/iniciar`, payload);
  }

  completarOnboardingWhatsapp(payload: CompletarOnboardingWhatsappPayload): Observable<OnboardingWhatsappResponse> {
    return this.http.post<OnboardingWhatsappResponse>(`${environment.apiBaseUrl}/admin/whatsapp/onboarding/completar`, payload);
  }

  reintentarOnboardingWhatsapp(): Observable<OnboardingWhatsappResponse> {
    return this.http.post<OnboardingWhatsappResponse>(`${environment.apiBaseUrl}/admin/whatsapp/onboarding/reintentar`, {});
  }

  getContactos(): Observable<SolicitudContactoAdmin[]> {
    return this.http.get<SolicitudContactoAdmin[]>(`${environment.apiBaseUrl}/admin/contactos`);
  }

  getMetadatosContactos(): Observable<MetadatosContactosAdmin> {
    return this.http.get<MetadatosContactosAdmin>(`${environment.apiBaseUrl}/admin/contactos/metadatos`);
  }

  actualizarEstadoContacto(id: number, estado: string): Observable<SolicitudContactoAdmin> {
    return this.http.patch<SolicitudContactoAdmin>(`${environment.apiBaseUrl}/admin/contactos/${id}/estado`, { estado });
  }
}
