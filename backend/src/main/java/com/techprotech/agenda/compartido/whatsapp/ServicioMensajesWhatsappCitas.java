package com.techprotech.agenda.compartido.whatsapp;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ServicioMensajesWhatsappCitas {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "MX"));
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm", new Locale("es", "MX"));
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "MX"));

    private final CitaRepositorio citaRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final ClienteWhatsappTwilio clienteWhatsappTwilio;
    private final ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa;

    public ServicioMensajesWhatsappCitas(
            CitaRepositorio citaRepositorio,
            ClienteRepositorio clienteRepositorio,
            ServicioRepositorio servicioRepositorio,
            SucursalRepositorio sucursalRepositorio,
            ClienteWhatsappTwilio clienteWhatsappTwilio,
            ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa
    ) {
        this.citaRepositorio = citaRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
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
        if (servicio == null || sucursal == null) {
            return null;
        }

        ZoneId zona = ZoneId.of(sucursal.getZonaHoraria());
        String fecha = cita.getInicio().atZone(zona).format(FORMATO_FECHA);
        String hora = cita.getInicio().atZone(zona).format(FORMATO_HORA);
        String fechaHora = cita.getInicio().atZone(zona).format(FORMATO_FECHA_HORA);
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);

        if ("CITA_REGISTRADA_WHATSAPP".equals(tipoEvento) && tieneTexto(configuracion.plantillaSolicitudConfirmacionSid())) {
            return clienteWhatsappTwilio.enviarPlantilla(
                    empresaId,
                    telefonoDestino,
                    configuracion.plantillaSolicitudConfirmacionSid(),
                    Map.of(
                            "1", cliente.getNombreCompleto(),
                            "2", fecha,
                            "3", hora
                    )
            );
        }

        if ("CITA_REPROGRAMADA_PENDIENTE_WHATSAPP".equals(tipoEvento) && tieneTexto(configuracion.plantillaReprogramadaPendienteSid())) {
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

        if ("CITA_CONFIRMADA_WHATSAPP".equals(tipoEvento) && tieneTexto(configuracion.plantillaCitaConfirmadaSid())) {
            return clienteWhatsappTwilio.enviarPlantilla(
                    empresaId,
                    telefonoDestino,
                    configuracion.plantillaCitaConfirmadaSid(),
                    Map.of(
                            "1", cliente.getNombreCompleto(),
                            "2", fecha,
                            "3", hora
                    )
            );
        }

        if ("CITA_RECORDATORIO_WHATSAPP".equals(tipoEvento) && tieneTexto(configuracion.plantillaRecordatorioSid())) {
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

        String mensaje;
        if ("CITA_RECORDATORIO_WHATSAPP".equals(tipoEvento)) {
            mensaje = """
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
                    textoSeguro(sucursal.getDireccion(), "Por definir"),
                    cita.getId(),
                    cita.getId(),
                    cita.getId()
            );
        } else if ("CITA_RECORDATORIO_CONFIRMACION_WHATSAPP".equals(tipoEvento)) {
            mensaje = """
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
        } else if ("CITA_CONFIRMADA_WHATSAPP".equals(tipoEvento)) {
            mensaje = """
                    Hola %s, tu cita en %s esta confirmada para el %s a las %s.

                    Direccion: %s.
                    Si necesitas cambiarla, responde a este mensaje.
                    """.formatted(
                    cliente.getNombreCompleto(),
                    sucursal.getNombre(),
                    fecha,
                    hora,
                    textoSeguro(sucursal.getDireccion(), "Por definir")
            );
        } else if ("CITA_REGISTRADA_WHATSAPP".equals(tipoEvento)) {
            mensaje = """
                    Hola %s, registramos tu cita en %s para el %s a las %s.

                    Direccion: %s.
                    Para asegurar tu lugar responde:
                    CONFIRMAR %d

                    Si necesitas cambiarla, también puedes responder:
                    CANCELAR %d
                    REAGENDAR|%d|AAAA-MM-DD HH:mm
                    """.formatted(
                    cliente.getNombreCompleto(),
                    sucursal.getNombre(),
                    fecha,
                    hora,
                    textoSeguro(sucursal.getDireccion(), "Por definir"),
                    cita.getId(),
                    cita.getId(),
                    cita.getId()
            );
        } else if ("CITA_REPROGRAMADA_PENDIENTE_WHATSAPP".equals(tipoEvento)) {
            mensaje = """
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
                    textoSeguro(sucursal.getDireccion(), "Por definir"),
                    cita.getId()
            );
        } else if ("CITA_CANCELADA_NEGOCIO_WHATSAPP".equals(tipoEvento)) {
            mensaje = """
                    Hola %s, tu cita en %s programada para el %s a las %s fue cancelada por el negocio.

                    Si deseas reagendar, responde MIS CITAS o escribe Quiero agendar.
                    """.formatted(
                    cliente.getNombreCompleto(),
                    sucursal.getNombre(),
                    fecha,
                    hora
            );
        } else if ("CITA_LIBERADA_SIN_CONFIRMACION_WHATSAPP".equals(tipoEvento)) {
            mensaje = """
                    Hola %s, como no recibimos tu confirmación para la cita del %s a las %s, ya no nos es posible respetar ese lugar.

                    Si deseas atenderte, por favor revisa disponibilidad en sucursal o agenda una nueva cita respondiendo a este chat.
                    """.formatted(
                    cliente.getNombreCompleto(),
                    fecha,
                    hora
            );
        } else if ("CITA_GRACIAS_VISITA_WHATSAPP".equals(tipoEvento)) {
            mensaje = """
                    Hola %s, gracias por tu visita a %s el %s a las %s.

                    Agradecemos tu preferencia y será un gusto atenderte nuevamente.
                    """.formatted(
                    cliente.getNombreCompleto(),
                    sucursal.getNombre(),
                    fecha,
                    hora
            );
        } else {
            mensaje = """
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
        }

        return clienteWhatsappTwilio.enviarMensaje(empresaId, telefonoDestino, mensaje.trim());
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

    private String textoSeguro(String valor, String fallback) {
        return valor == null || valor.isBlank() ? fallback : valor;
    }
}
