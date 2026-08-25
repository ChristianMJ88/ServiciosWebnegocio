package com.techprotech.agenda.compartido.whatsapp;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ServicioMensajesWhatsappCitas {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioMensajesWhatsappCitas.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy",
      new Locale("es", "MX"));
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm",
      new Locale("es", "MX"));
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
      new Locale("es", "MX"));

    private final CitaRepositorio citaRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final ClienteWhatsappTwilio clienteWhatsappTwilio;
    private final ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa;

    public ServicioMensajesWhatsappCitas(
            CitaRepositorio citaRepositorio,
            ClienteRepositorio clienteRepositorio,
            ServicioRepositorio servicioRepositorio,
            SucursalRepositorio sucursalRepositorio,
            EmpresaRepositorio empresaRepositorio,
            ClienteWhatsappTwilio clienteWhatsappTwilio,
            ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa
    ) {
        this.citaRepositorio = citaRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.empresaRepositorio = empresaRepositorio;
        this.clienteWhatsappTwilio = clienteWhatsappTwilio;
        this.servicioConfiguracionWhatsappEmpresa = servicioConfiguracionWhatsappEmpresa;
    }

    @Transactional(readOnly = true)
    public ResultadoEnvioWhatsapp enviarNotificacion(Long empresaId, String tipoEvento, MensajeCitaWhatsappPayload payload) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaId(payload.citaId(), empresaId).orElse(null);
        if (cita == null || !estadoCompatibleConEvento(tipoEvento, cita.getEstado())) {
            return null;
        }

        if (payload.inicioEsperado() != null && !payload.inicioEsperado().equals(cita.getInicio())) {
            return null;
        }

        ClienteEntidad cliente = clienteRepositorio.findById(cita.getClienteId()).orElse(null);
        if (cliente == null || !cliente.isAceptaWhatsapp() || cliente.getTelefono() == null || cliente.getTelefono().isBlank()) {
            return null;
        }

        String telefonoDestino = tieneTexto(payload.telefonoDestino()) ? payload.telefonoDestino() : cliente.getTelefono();

        ServicioEntidad servicio = servicioRepositorio.findById(cita.getServicioId()).orElse(null);
        SucursalEntidad sucursal = sucursalRepositorio.findById(cita.getSucursalId()).orElse(null);
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId).orElse(null);
        if (servicio == null || sucursal == null || empresa == null) {
            return null;
        }

        ZoneId zona = ZoneId.of(sucursal.getZonaHoraria());
        String fecha = cita.getInicio().atZone(zona).format(FORMATO_FECHA);
        String hora = cita.getInicio().atZone(zona).format(FORMATO_HORA);
        String fechaHora = cita.getInicio().atZone(zona).format(FORMATO_FECHA_HORA);
        String fechaHoraAmigable = "%s a las %s".formatted(fecha, hora);
        String totalServicios = String.valueOf(payload.totalServicios() != null && payload.totalServicios() > 0 ? payload.totalServicios() : 1);
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);

        if ("CITA_REGISTRADA_WHATSAPP".equals(tipoEvento)) {
            validarPlantillaConfigurada(tipoEvento, configuracion.plantillaSolicitudConfirmacionSid(), empresaId);
            LOGGER.info(
                    "Enviando WhatsApp con plantilla {} para empresa {} evento {} cita {}",
                    configuracion.plantillaSolicitudConfirmacionSid(),
                    empresaId,
                    tipoEvento,
                    cita.getId()
            );
            return clienteWhatsappTwilio.enviarPlantilla(
                    empresaId,
                    telefonoDestino,
                    configuracion.plantillaSolicitudConfirmacionSid(),
                    Map.of(
                            "1", cliente.getNombreCompleto(),
                            "2", empresa.getNombre(),
                            "3", fechaHoraAmigable,
                            "4", totalServicios
                    )
            );
        }

        if ("CITA_REPROGRAMADA_PENDIENTE_WHATSAPP".equals(tipoEvento) && tieneTexto(configuracion.plantillaReprogramadaPendienteSid())) {
            LOGGER.info(
                    "Enviando WhatsApp con plantilla {} para empresa {} evento {} cita {}",
                    configuracion.plantillaReprogramadaPendienteSid(),
                    empresaId,
                    tipoEvento,
                    cita.getId()
            );
            return clienteWhatsappTwilio.enviarPlantilla(
                    empresaId,
                    telefonoDestino,
                    configuracion.plantillaReprogramadaPendienteSid(),
                    Map.of(
                            "1", cliente.getNombreCompleto(),
                            "2", fecha,
                            "3", hora
                    )
            );
        }

        if ("CITA_RECORDATORIO_CONFIRMACION_WHATSAPP".equals(tipoEvento) && tieneTexto(configuracion.plantillaRecordatorioConfirmacionSid())) {
            LOGGER.info(
                    "Enviando WhatsApp con plantilla {} para empresa {} evento {} cita {}",
                    configuracion.plantillaRecordatorioConfirmacionSid(),
                    empresaId,
                    tipoEvento,
                    cita.getId()
            );
            return clienteWhatsappTwilio.enviarPlantilla(
                    empresaId,
                    telefonoDestino,
                    configuracion.plantillaRecordatorioConfirmacionSid(),
                    Map.of(
                            "1", cliente.getNombreCompleto(),
                            "2", fecha,
                            "3", hora
                    )
            );
        }

        if ("CITA_CONFIRMADA_WHATSAPP".equals(tipoEvento)) {
            validarPlantillaConfigurada(tipoEvento, configuracion.plantillaCitaConfirmadaSid(), empresaId);
            LOGGER.info(
                    "Enviando WhatsApp con plantilla {} para empresa {} evento {} cita {}",
                    configuracion.plantillaCitaConfirmadaSid(),
                    empresaId,
                    tipoEvento,
                    cita.getId()
            );
            return clienteWhatsappTwilio.enviarPlantilla(
                    empresaId,
                    telefonoDestino,
                    configuracion.plantillaCitaConfirmadaSid(),
                    Map.of(
                            "1", cliente.getNombreCompleto(),
                            "2", empresa.getNombre(),
                            "3", fecha,
                            "4", hora,
                            "5", textoSeguro(sucursal.getDireccion())
                    )
            );
        }

        if ("CITA_RECORDATORIO_WHATSAPP".equals(tipoEvento) && tieneTexto(configuracion.plantillaRecordatorioSid())) {
            LOGGER.info(
                    "Enviando WhatsApp con plantilla {} para empresa {} evento {} cita {}",
                    configuracion.plantillaRecordatorioSid(),
                    empresaId,
                    tipoEvento,
                    cita.getId()
            );
            return clienteWhatsappTwilio.enviarPlantilla(
                    empresaId,
                    telefonoDestino,
                    configuracion.plantillaRecordatorioSid(),
                    Map.of(
                            "1", cliente.getNombreCompleto(),
                            "2", fecha,
                            "3", hora
                    )
            );
        }

        if ("CITA_CANCELADA_NEGOCIO_WHATSAPP".equals(tipoEvento) && tieneTexto(configuracion.plantillaCancelacionSid())) {
            LOGGER.info(
                    "Enviando WhatsApp con plantilla {} para empresa {} evento {} cita {}",
                    configuracion.plantillaCancelacionSid(),
                    empresaId,
                    tipoEvento,
                    cita.getId()
            );
            return clienteWhatsappTwilio.enviarPlantilla(
                    empresaId,
                    telefonoDestino,
                    configuracion.plantillaCancelacionSid(),
                    Map.of(
                            "1", cliente.getNombreCompleto(),
                            "2", fecha,
                            "3", hora
                    )
            );
        }

        if ("CITA_LIBERADA_SIN_CONFIRMACION_WHATSAPP".equals(tipoEvento) && tieneTexto(configuracion.plantillaLiberadaSinConfirmacionSid())) {
            LOGGER.info(
                    "Enviando WhatsApp con plantilla {} para empresa {} evento {} cita {}",
                    configuracion.plantillaLiberadaSinConfirmacionSid(),
                    empresaId,
                    tipoEvento,
                    cita.getId()
            );
            return clienteWhatsappTwilio.enviarPlantilla(
                    empresaId,
                    telefonoDestino,
                    configuracion.plantillaLiberadaSinConfirmacionSid(),
                    Map.of(
                            "1", cliente.getNombreCompleto(),
                            "2", fecha,
                            "3", hora
                    )
            );
        }

        if ("CITA_GRACIAS_VISITA_WHATSAPP".equals(tipoEvento) && tieneTexto(configuracion.plantillaGraciasVisitaSid())) {
            LOGGER.info(
                    "Enviando WhatsApp con plantilla {} para empresa {} evento {} cita {}",
                    configuracion.plantillaGraciasVisitaSid(),
                    empresaId,
                    tipoEvento,
                    cita.getId()
            );
            return clienteWhatsappTwilio.enviarPlantilla(
                    empresaId,
                    telefonoDestino,
                    configuracion.plantillaGraciasVisitaSid(),
                    Map.of(
                            "1", cliente.getNombreCompleto(),
                            "2", fecha,
                            "3", hora
                    )
            );
        }

        String mensaje = switch (tipoEvento) {
          case "CITA_RECORDATORIO_WHATSAPP" -> """
            Recordatorio de cita
            Folio: %d
            Servicio: %s
            Fecha: %s
            Sucursal: %s
            Direccion: %s

            Responde:
            CONFIRMAR %d
            CANCELAR %d
            REAGENDAR|%d|AAAA-MM-DD HH:mm
            """.formatted(
            cita.getId(),
            servicio.getNombre(),
            fechaHora,
            sucursal.getNombre(),
            textoSeguro(sucursal.getDireccion()),
            cita.getId(),
            cita.getId(),
            cita.getId()
          );
          case "CITA_RECORDATORIO_CONFIRMACION_WHATSAPP" -> """
            Hola %s, te recordamos tu cita en %s para el %s a las %s.

            Aun esta pendiente de confirmacion.
            Para asegurar tu lugar responde:
            CONFIRMAR %d

            Si deseas revisarla, responde MIS CITAS.
            """.formatted(
            cliente.getNombreCompleto(),
            sucursal.getNombre(),
            fecha,
            hora,
            cita.getId()
          );
          case "CITA_REPROGRAMADA_PENDIENTE_WHATSAPP" -> """
            Hola %s, tu cita en %s fue reprogramada para el %s a las %s.

            Direccion: %s.
            Para asegurar tu lugar responde:
            CONFIRMAR %d

            Si necesitas revisarla, tambien puedes responder MIS CITAS.
            """.formatted(
            cliente.getNombreCompleto(),
            sucursal.getNombre(),
            fecha,
            hora,
            textoSeguro(sucursal.getDireccion()),
            cita.getId()
          );
          case "CITA_CANCELADA_NEGOCIO_WHATSAPP" -> """
            Hola %s, tu cita en %s programada para el %s a las %s fue cancelada por el negocio.

            Si deseas reagendar, responde MIS CITAS o escribe Quiero agendar.
            """.formatted(
            cliente.getNombreCompleto(),
            sucursal.getNombre(),
            fecha,
            hora
          );
          case "CITA_LIBERADA_SIN_CONFIRMACION_WHATSAPP" -> """
            Hola %s, como no recibimos tu confirmación para la cita del %s a las %s, ya no nos es posible respetar ese lugar.

            Si deseas atenderte, por favor revisa disponibilidad en sucursal o agenda una nueva cita respondiendo a este chat.
            """.formatted(
            cliente.getNombreCompleto(),
            fecha,
            hora
          );
          case "CITA_GRACIAS_VISITA_WHATSAPP" -> """
            Hola %s, gracias por tu visita a %s el %s a las %s.

            Agradecemos tu preferencia y será un gusto atenderte nuevamente.
            """.formatted(
            cliente.getNombreCompleto(),
            sucursal.getNombre(),
            fecha,
            hora
          );
          case null, default -> """
            Tu cita fue registrada
            Folio: %d
            Servicio: %s
            Fecha: %s
            Sucursal: %s

            Responde:
            CONFIRMAR %d
            CANCELAR %d
            REAGENDAR|%d|AAAA-MM-DD HH:mm
            También puedes consultar: MIS CITAS
            """.formatted(
            cita.getId(),
            servicio.getNombre(),
            fecha,
            sucursal.getNombre(),
            cita.getId(),
            cita.getId(),
            cita.getId()
          );
        };

      return clienteWhatsappTwilio.enviarMensaje(empresaId, telefonoDestino, mensaje.trim());
    }

    private void validarPlantillaConfigurada(String tipoEvento, String plantillaSid, Long empresaId) {
        if (tieneTexto(plantillaSid)) {
            return;
        }
        throw new IllegalStateException(
                "No hay plantilla configurada para el evento %s en la empresa %d".formatted(tipoEvento, empresaId)
        );
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private boolean estadoCompatibleConEvento(String tipoEvento, String estado) {
        if ("CITA_LIBERADA_SIN_CONFIRMACION_WHATSAPP".equals(tipoEvento)) {
            return "LIBERADA_SIN_CONFIRMACION".equalsIgnoreCase(estado);
        }
        if ("CITA_GRACIAS_VISITA_WHATSAPP".equals(tipoEvento)) {
            return "FINALIZADA".equalsIgnoreCase(estado);
        }
        return List.of("PENDIENTE", "CONFIRMADA").contains(estado);
    }

    private String textoSeguro(String valor) {
        return valor == null || valor.isBlank() ? "Por definir" : valor;
    }
}
