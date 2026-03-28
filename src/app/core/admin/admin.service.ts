import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
  nombre: string;
  descripcion: string | null;
  duracionMinutos: number;
  bufferAntesMinutos: number;
  bufferDespuesMinutos: number;
  precio: number;
  moneda: string;
  activo: boolean;
}

export interface GuardarServicioPayload {
  sucursalId: number;
  nombre: string;
  descripcion: string | null;
  duracionMinutos: number;
  bufferAntesMinutos: number;
  bufferDespuesMinutos: number;
  precio: number;
  moneda: string;
  activo: boolean;
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
  correo: string;
  nombreCompleto: string;
  telefono: string | null;
  puesto: string | null;
  rolCodigo: string;
  activo: boolean;
  notas: string | null;
}

export interface GuardarUsuarioInternoPayload {
  sucursalId: number | null;
  correo: string;
  contrasenaTemporal: string | null;
  nombreCompleto: string;
  telefono: string | null;
  puesto: string | null;
  rolCodigo: string;
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

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly http = inject(HttpClient);

  getResumen(): Observable<ResumenAdmin> {
    return this.http.get<ResumenAdmin>(`${environment.apiBaseUrl}/admin/resumen`);
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

  getReporteServicios(): Observable<ReporteServicioAdmin[]> {
    return this.http.get<ReporteServicioAdmin[]>(`${environment.apiBaseUrl}/admin/reportes/servicios`);
  }

  getConfiguracionCorreo(): Observable<ConfiguracionCorreoAdmin> {
    return this.http.get<ConfiguracionCorreoAdmin>(`${environment.apiBaseUrl}/admin/configuracion-correo`);
  }

  actualizarConfiguracionCorreo(payload: GuardarConfiguracionCorreoPayload): Observable<ConfiguracionCorreoAdmin> {
    return this.http.patch<ConfiguracionCorreoAdmin>(`${environment.apiBaseUrl}/admin/configuracion-correo`, payload);
  }

  migrarSecretosCorreo(): Observable<MigracionSecretosCorreoResponse> {
    return this.http.post<MigracionSecretosCorreoResponse>(`${environment.apiBaseUrl}/admin/configuracion-correo/migrar-secretos`, {});
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

  getLogsWhatsapp(): Observable<LogMensajeWhatsappAdmin[]> {
    return this.http.get<LogMensajeWhatsappAdmin[]>(`${environment.apiBaseUrl}/admin/configuracion-whatsapp/logs`);
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

  getContactos(): Observable<SolicitudContactoAdmin[]> {
    return this.http.get<SolicitudContactoAdmin[]>(`${environment.apiBaseUrl}/admin/contactos`);
  }

  actualizarEstadoContacto(id: number, estado: string): Observable<SolicitudContactoAdmin> {
    return this.http.patch<SolicitudContactoAdmin>(`${environment.apiBaseUrl}/admin/contactos/${id}/estado`, { estado });
  }
}
