package com.techprotech.agenda.modulos.whatsapp.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.whatsapp.NormalizadorTelefonoWhatsapp;
import com.techprotech.agenda.compartido.whatsapp.PropiedadesWhatsapp;
import com.techprotech.agenda.compartido.whatsapp.ClienteWhatsappTwilio;
import com.techprotech.agenda.compartido.whatsapp.ResultadoEnvioWhatsapp;
import com.techprotech.agenda.compartido.whatsapp.ServicioConfiguracionWhatsappEmpresa;
import com.techprotech.agenda.compartido.whatsapp.ContextoEmpresaWhatsapp;
import com.techprotech.agenda.compartido.whatsapp.ServicioPlantillasWhatsappEmpresa;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaRequest;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitas;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitasCliente;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.FranjaDisponibleResponse;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConsultaDisponibilidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.GrupoServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.SubgrupoServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.GrupoServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.SubgrupoServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.whatsapp.infraestructura.entidad.ConversacionWhatsappEntidad;
import com.techprotech.agenda.modulos.whatsapp.infraestructura.entidad.MensajeWhatsappEntidad;
import com.techprotech.agenda.modulos.whatsapp.infraestructura.repositorio.ConversacionWhatsappRepositorio;
import com.techprotech.agenda.modulos.whatsapp.infraestructura.repositorio.MensajeWhatsappRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class ServicioWhatsappCitas {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioWhatsappCitas.class);

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter FORMATO_RESPUESTA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "MX"));
    private static final DateTimeFormatter FORMATO_FECHA_AMIGABLE = DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM", new Locale("es", "MX"));
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMATO_HORA_DETALLE = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
    private static final Pattern SOLO_HORA = Pattern.compile("^\\d{2}:\\d{2}$");
    private static final long MINUTOS_EXPIRACION_CONVERSACION = 60L;
    private static final String PREFIJO_RESPUESTA_CONTENIDO = "__WA_CONTENT__";
    private static final String USO_SELECCIONAR_SUCURSAL = "seleccionar_sucursal";
    private static final String USO_CATEGORIA_SERVICIO = "categoria_servicio";
    private static final String USO_SUBCATEGORIA_GENERICO = "subcategoria_generico";
    private static final String USO_SUBCATEGORIA_GENERICA = "subcategoria_generica";
    private static final String USO_SERVICIO_GENERICO = "servicio_generico";
    private static final Long GRUPO_SIN_CATEGORIA_ID = 0L;
    private static final Map<String, String> LIST_PICKER_SIDS_POR_USO = Map.of(
            USO_SELECCIONAR_SUCURSAL, "HXb5123174a3f36d76ff186d94d6be5cfe",
            USO_CATEGORIA_SERVICIO, "HXd65399c8e9ea5e115b85715cc1c3c0b4",
            USO_SUBCATEGORIA_GENERICO, "HX14dee1638ae9526c2ef7fc923cd401c3",
            USO_SUBCATEGORIA_GENERICA, "HX14dee1638ae9526c2ef7fc923cd401c3",
            USO_SERVICIO_GENERICO, "HX7db152d9c27db81bfbddf7ce69ab6eed"
    );

    private final PropiedadesWhatsapp propiedadesWhatsapp;
    private final ContextoEmpresaWhatsapp contextoEmpresaWhatsapp;
    private final ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa;
    private final ServicioPlantillasWhatsappEmpresa servicioPlantillasWhatsappEmpresa;
    private final ClienteWhatsappTwilio clienteWhatsappTwilio;
    private final ObjectMapper objectMapper;
    private final ServicioCitas servicioCitas;
    private final ServicioCitasCliente servicioCitasCliente;
    private final ServicioConsultaDisponibilidad servicioConsultaDisponibilidad;
    private final ClienteRepositorio clienteRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final GrupoServicioRepositorio grupoServicioRepositorio;
    private final SubgrupoServicioRepositorio subgrupoServicioRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final ConversacionWhatsappRepositorio conversacionWhatsappRepositorio;
    private final MensajeWhatsappRepositorio mensajeWhatsappRepositorio;

    public ServicioWhatsappCitas(
            PropiedadesWhatsapp propiedadesWhatsapp,
            ContextoEmpresaWhatsapp contextoEmpresaWhatsapp,
            ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa,
            ServicioPlantillasWhatsappEmpresa servicioPlantillasWhatsappEmpresa,
            ClienteWhatsappTwilio clienteWhatsappTwilio,
            ObjectMapper objectMapper,
            ServicioCitas servicioCitas,
            ServicioCitasCliente servicioCitasCliente,
            ServicioConsultaDisponibilidad servicioConsultaDisponibilidad,
            ClienteRepositorio clienteRepositorio,
            EmpresaRepositorio empresaRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            SucursalRepositorio sucursalRepositorio,
            GrupoServicioRepositorio grupoServicioRepositorio,
            SubgrupoServicioRepositorio subgrupoServicioRepositorio,
            ServicioRepositorio servicioRepositorio,
            ConversacionWhatsappRepositorio conversacionWhatsappRepositorio,
            MensajeWhatsappRepositorio mensajeWhatsappRepositorio
    ) {
        this.propiedadesWhatsapp = propiedadesWhatsapp;
        this.contextoEmpresaWhatsapp = contextoEmpresaWhatsapp;
        this.servicioConfiguracionWhatsappEmpresa = servicioConfiguracionWhatsappEmpresa;
        this.servicioPlantillasWhatsappEmpresa = servicioPlantillasWhatsappEmpresa;
        this.clienteWhatsappTwilio = clienteWhatsappTwilio;
        this.objectMapper = objectMapper;
        this.servicioCitas = servicioCitas;
        this.servicioCitasCliente = servicioCitasCliente;
        this.servicioConsultaDisponibilidad = servicioConsultaDisponibilidad;
        this.clienteRepositorio = clienteRepositorio;
        this.empresaRepositorio = empresaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.grupoServicioRepositorio = grupoServicioRepositorio;
        this.subgrupoServicioRepositorio = subgrupoServicioRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.conversacionWhatsappRepositorio = conversacionWhatsappRepositorio;
        this.mensajeWhatsappRepositorio = mensajeWhatsappRepositorio;
    }

    public RespuestaWhatsapp procesarWebhook(Long empresaId, String telefonoRemitente, String mensajeOriginal) {
        return contextoEmpresaWhatsapp.ejecutar(empresaId, () -> procesarWebhookInterno(telefonoRemitente, mensajeOriginal));
    }

    private RespuestaWhatsapp procesarWebhookInterno(String telefonoRemitente, String mensajeOriginal) {
        String respuesta = procesarMensaje(telefonoRemitente, mensajeOriginal);
        RespuestaWhatsapp respuestaConContenido = deserializarRespuestaContenido(respuesta);
        if (respuestaConContenido != null) {
            return respuestaConContenido;
        }

        String bienvenidaTexto = bienvenidaTexto();
        if (Objects.equals(respuesta, bienvenidaTexto)) {
            return construirRespuestaBienvenida(bienvenidaTexto);
        }
        return RespuestaWhatsapp.texto(respuesta);
    }

    public boolean enviarContenidoInteractivo(Long empresaId, String telefonoRemitente, RespuestaWhatsapp respuesta) {
        return contextoEmpresaWhatsapp.ejecutar(empresaId, () -> enviarContenidoInteractivoInterno(telefonoRemitente, respuesta));
    }

    private boolean enviarContenidoInteractivoInterno(String telefonoRemitente, RespuestaWhatsapp respuesta) {
        if (respuesta == null || !respuesta.tieneContenidoInteractivo()) {
            return false;
        }

        try {
            ResultadoEnvioWhatsapp resultado = clienteWhatsappTwilio.enviarContenido(
                    obtenerEmpresaId(),
                    telefonoRemitente,
                    respuesta.contenidoInteractivo().contentSid(),
                    respuesta.contenidoInteractivo().variables()
            );
            registrarMensajeSaliente(
                    telefonoRemitente,
                    respuesta.mensaje(),
                    respuesta.contenidoInteractivo().contentSid(),
                    resultado
            );
            if (resultado == null) {
                return false;
            }

            boolean enviadoSinError = !tieneTexto(resultado.codigoErrorProveedor()) && !tieneTexto(resultado.detalleErrorProveedor());
            if (!enviadoSinError) {
                LOGGER.warn(
                        "No se pudo enviar contenido interactivo de WhatsApp a {}. Codigo={} detalle={}",
                        telefonoRemitente,
                        resultado.codigoErrorProveedor(),
                        resultado.detalleErrorProveedor()
                );
            }
            return enviadoSinError;
        } catch (Exception ex) {
            LOGGER.warn("Fallo el envio del contenido interactivo de WhatsApp para {}", telefonoRemitente, ex);
            registrarMensajeSalienteFallido(
                    telefonoRemitente,
                    respuesta.mensaje(),
                    respuesta.contenidoInteractivo().contentSid(),
                    ex.getMessage()
            );
            return false;
        }
    }

    public void registrarMensajeEntrante(Long empresaId, String telefonoRemitente, String mensaje) {
        contextoEmpresaWhatsapp.ejecutar(empresaId, () -> registrarMensajeEntranteInterno(telefonoRemitente, mensaje));
    }

    private void registrarMensajeEntranteInterno(String telefonoRemitente, String mensaje) {
        registrarMensajeWhatsapp(
                telefonoRemitente,
                "ENTRANTE",
                mensaje,
                null,
                null,
                "RECIBIDO",
                null,
                null
        );
    }

    public void registrarMensajeSalienteTexto(Long empresaId, String telefonoDestino, String mensaje) {
        contextoEmpresaWhatsapp.ejecutar(empresaId, () -> registrarMensajeSalienteTextoInterno(telefonoDestino, mensaje));
    }

    private void registrarMensajeSalienteTextoInterno(String telefonoDestino, String mensaje) {
        registrarMensajeWhatsapp(
                telefonoDestino,
                "SALIENTE",
                mensaje,
                null,
                null,
                "ENVIADO",
                null,
                null
        );
    }

    private void registrarMensajeSaliente(String telefonoDestino, String mensaje, String contentSid, ResultadoEnvioWhatsapp resultado) {
        registrarMensajeWhatsapp(
                telefonoDestino,
                "SALIENTE",
                mensaje,
                contentSid,
                resultado != null ? resultado.proveedorMensajeId() : null,
                resultado != null ? resultado.estadoProveedor() : null,
                resultado != null ? resultado.codigoErrorProveedor() : null,
                resultado != null ? resultado.detalleErrorProveedor() : null
        );
    }

    private void registrarMensajeSalienteFallido(String telefonoDestino, String mensaje, String contentSid, String detalleError) {
        registrarMensajeWhatsapp(
                telefonoDestino,
                "SALIENTE",
                mensaje,
                contentSid,
                null,
                "ERROR",
                null,
                detalleError
        );
    }

    private void registrarMensajeWhatsapp(
            String telefono,
            String direccion,
            String cuerpo,
            String contentSid,
            String proveedorMensajeId,
            String estado,
            String codigoErrorProveedor,
            String detalleErrorProveedor
    ) {
        try {
            String telefonoNormalizado = NormalizadorTelefonoWhatsapp.normalizarComparable(telefono);
            if (!tieneTexto(telefonoNormalizado)) {
                return;
            }
            MensajeWhatsappEntidad mensaje = new MensajeWhatsappEntidad();
            mensaje.setEmpresaId(obtenerEmpresaId());
            mensaje.setTelefonoNormalizado(telefonoNormalizado);
            mensaje.setDireccion(direccion);
            mensaje.setCuerpo(limitar(cuerpo, 1600));
            mensaje.setContentSid(limitar(contentSid, 80));
            mensaje.setProveedorMensajeId(limitar(proveedorMensajeId, 80));
            mensaje.setEstado(limitar(estado, 30));
            mensaje.setCodigoErrorProveedor(limitar(codigoErrorProveedor, 32));
            mensaje.setDetalleErrorProveedor(limitar(detalleErrorProveedor, 500));
            mensaje.setCreadoEn(LocalDateTime.now());
            mensajeWhatsappRepositorio.save(mensaje);
        } catch (Exception ex) {
            LOGGER.warn("No se pudo registrar mensaje WhatsApp {} para {}", direccion, telefono, ex);
        }
    }

    private String procesarMensaje(String telefonoRemitente, String mensajeOriginal) {
        if (!servicioConfiguracionWhatsappEmpresa.resolver(obtenerEmpresaId()).habilitado()) {
            return "El canal de WhatsApp no esta habilitado en este momento.";
        }

        String mensaje = mensajeOriginal == null ? "" : mensajeOriginal.trim();
        if (mensaje.isBlank()) {
            return bienvenidaTexto();
        }

        String mensajeMayus = mensaje.toUpperCase(Locale.ROOT);
        String mensajeNormalizado = normalizarTextoLibre(mensaje);
        ConversacionWhatsappEntidad conversacion = obtenerConversacion(telefonoRemitente);
        try {
            if (esMensajeMenu(mensajeNormalizado)) {
                limpiarConversacion(telefonoRemitente);
                return bienvenidaTexto();
            }
            if ("SUCURSALES".equals(mensajeMayus)) {
                return listarSucursales();
            }
            if ("MIS CITAS".equals(mensajeMayus)) {
                return listarMisCitas(telefonoRemitente);
            }
            if ("VER DETALLE".equals(mensajeMayus) || "VER DETALLES".equals(mensajeMayus)) {
                return responderDetalleCita(telefonoRemitente);
            }
            if ("CONFIRMAR CITA".equals(mensajeMayus)) {
                return confirmarCitaWhatsapp(telefonoRemitente, "CONFIRMAR");
            }
            if (esIntencionUbicacion(mensajeNormalizado)) {
                limpiarConversacion(telefonoRemitente);
                return responderUbicacion();
            }
            if (esIntencionServicios(mensajeNormalizado)) {
                limpiarConversacion(telefonoRemitente);
                return responderServiciosNaturales();
            }
            if (esIntencionPromociones(mensajeNormalizado)) {
                limpiarConversacion(telefonoRemitente);
                return responderPromociones();
            }
            if (esIntencionPausarRecordatorios(mensajeNormalizado)) {
                limpiarConversacion(telefonoRemitente);
                return "Perfecto. Dejamos pendiente la opción de pausar recordatorios de regreso para dejarlo formal en tu perfil.";
            }
            if (esIntencionNoPorAhora(mensajeNormalizado)) {
                limpiarConversacion(telefonoRemitente);
                return "Entendido. Cuando quieras retomar tu próxima cita, aquí mismo te ayudo.";
            }
            if (esIntencionHorarios(mensajeNormalizado)) {
                return iniciarFlujoHorarios(telefonoRemitente);
            }
            if (esIntencionAgendar(mensajeNormalizado)) {
                return iniciarFlujoAgendar(telefonoRemitente);
            }
            if (esIntencionReagendar(mensajeNormalizado)) {
                return iniciarFlujoReagendar(telefonoRemitente, mensaje);
            }
            if (conversacion != null) {
                if (esCancelarFlujoConversacional(conversacion, mensajeNormalizado)) {
                    limpiarConversacion(telefonoRemitente);
                    return "Listo, cancelé este proceso. Cuando quieras retomarlo, solo dime: Quiero agendar.";
                }
                if (debePriorizarConversacion(conversacion, mensajeMayus, mensajeNormalizado)) {
                    return continuarConversacion(conversacion, telefonoRemitente, mensaje);
                }
            }
            if (mensajeMayus.startsWith("SERVICIOS")) {
                return listarServicios(mensaje);
            }
            if (mensajeMayus.startsWith("HORARIOS")) {
                return listarHorarios(mensaje);
            }
            if (mensajeMayus.startsWith("CONFIRMAR")) {
                return confirmarCita(telefonoRemitente, mensaje);
            }
            if (mensajeMayus.startsWith("CANCELAR")) {
                return cancelarCita(telefonoRemitente, mensaje);
            }
            if (mensajeMayus.startsWith("REAGENDAR|")) {
                return reprogramarCita(telefonoRemitente, mensaje);
            }
            if (mensajeMayus.startsWith("REAGENDAR")) {
                return iniciarFlujoReagendar(telefonoRemitente, mensaje);
            }
            if (mensajeMayus.startsWith("AGENDAR")) {
                return agendarCita(telefonoRemitente, mensaje);
            }
            if (conversacion != null) {
                return continuarConversacion(conversacion, telefonoRemitente, mensaje);
            }
            return bienvenidaTexto();
        } catch (ResponseStatusException ex) {
            return ex.getReason() != null ? ex.getReason() : "No pudimos procesar tu solicitud.";
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        }
    }

    private String bienvenidaTexto() {
        return """
                Hola, 👋 Soy el asistente de %s.

                ¿Que te gustaría hacer hoy?:

                1️⃣ Agendar una cita
                2️⃣ Ver servicios
                3️⃣ Ver horarios disponibles
                4️⃣ Ver ubicación
                5️⃣ Revisar mis citas
                6️⃣ Ver promociones

                Solo dime qué necesitas y te guío paso a paso.
                """.formatted(obtenerNombreNegocio()).trim();
    }

    private RespuestaWhatsapp construirRespuestaBienvenida(String fallbackTexto) {
        Long empresaId = obtenerEmpresaId();
        String plantillaMenuBienvenidaSid = servicioPlantillasWhatsappEmpresa.resolverContentSid(empresaId, "MENU_BIENVENIDA");
        if (!tieneTexto(plantillaMenuBienvenidaSid)) {
            plantillaMenuBienvenidaSid = servicioConfiguracionWhatsappEmpresa
                    .resolver(empresaId)
                    .plantillaMenuBienvenidaSid();
        }
        if (!tieneTexto(plantillaMenuBienvenidaSid)) {
            return RespuestaWhatsapp.texto(fallbackTexto);
        }

        return RespuestaWhatsapp.conContenido(
                fallbackTexto,
                plantillaMenuBienvenidaSid,
                Map.of("1", obtenerNombreNegocio())
        );
    }

    private String obtenerNombreNegocio() {
        return empresaRepositorio.findById(obtenerEmpresaId())
                .map(EmpresaEntidad::getNombre)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .orElse("nuestro estudio");
    }

    private String responderUbicacion() {
        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdAndActivaTrue(obtenerEmpresaId()).stream()
                .sorted(Comparator.comparing(SucursalEntidad::getNombre))
                .toList();

        if (sucursales.isEmpty()) {
            return "En este momento no tenemos una ubicación disponible para compartirte.";
        }

        StringBuilder respuesta = new StringBuilder("Estas son nuestras ubicaciones:\n");
        for (SucursalEntidad sucursal : sucursales) {
            respuesta.append("- ")
                    .append(sucursal.getNombre());
            if (tieneTexto(sucursal.getDireccion())) {
                respuesta.append(": ").append(sucursal.getDireccion());
            }
            if (tieneTexto(sucursal.getTelefono())) {
                respuesta.append(" · Tel. ").append(sucursal.getTelefono());
            }
            respuesta.append("\n");
        }
        respuesta.append("\nSi quieres agendar, responde: Quiero agendar");
        return respuesta.toString().trim();
    }

    private String responderServiciosNaturales() {
        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdAndActivaTrue(obtenerEmpresaId()).stream()
                .sorted(Comparator.comparing(SucursalEntidad::getNombre))
                .toList();

        if (sucursales.isEmpty()) {
            return "No tenemos sucursales activas en este momento.";
        }

        StringBuilder respuesta = new StringBuilder("Estos son nuestros servicios disponibles:\n");
        for (SucursalEntidad sucursal : sucursales) {
            List<ServicioEntidad> servicios = servicioRepositorio.findBySucursalIdAndActivoTrue(sucursal.getId()).stream()
                    .sorted(Comparator.comparing(ServicioEntidad::getNombre))
                    .toList();
            if (servicios.isEmpty()) {
                continue;
            }
            respuesta.append("\n").append(sucursal.getNombre()).append(":\n");
            for (ServicioEntidad servicio : servicios) {
                respuesta.append("- ")
                        .append(servicio.getNombre())
                        .append(" (")
                        .append(servicio.getDuracionMinutos())
                        .append(" min)\n");
            }
        }
        respuesta.append("\nSi quieres agendar, responde: Quiero agendar");
        return respuesta.toString().trim();
    }

    private String responderPromociones() {
        return """
                Muy pronto te compartiremos promociones por este medio.

                Si quieres, por ahora puedo ayudarte a:

                - Agendar una cita
                - Ver servicios
                - Ver horarios disponibles
                - Revisar mis citas
                """.trim();
    }

    private String iniciarFlujoAgendar(String telefonoRemitente) {
        ConversacionWhatsappEntidad conversacion = obtenerOCrearConversacion(telefonoRemitente);
        conversacion.setFlujo("AGENDAR");

        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdAndActivaTrue(obtenerEmpresaId()).stream()
                .sorted(Comparator.comparing(SucursalEntidad::getNombre))
                .toList();
        if (sucursales.isEmpty()) {
            limpiarConversacion(telefonoRemitente);
            return "En este momento no tenemos sucursales activas para agendar.";
        }

        if (sucursales.size() == 1) {
            conversacion.setCitaId(null);
            conversacion.setSucursalId(sucursales.getFirst().getId());
            conversacion.setGrupoId(null);
            conversacion.setSubgrupoId(null);
            conversacion.setServicioId(null);
            conversacion.setPaso("AGENDAR_GRUPO");
            guardarConversacion(conversacion);
            return construirSiguientePreguntaCatalogo(conversacion, "Claro, te ayudo a agendar.");
        }

        conversacion.setPaso("AGENDAR_SUCURSAL");
        conversacion.setCitaId(null);
        conversacion.setSucursalId(null);
        conversacion.setGrupoId(null);
        conversacion.setSubgrupoId(null);
        conversacion.setServicioId(null);
        guardarConversacion(conversacion);
        return construirPreguntaSucursales("Claro, te ayudo a agendar. Primero elige la sucursal.");
    }

    private String iniciarFlujoHorarios(String telefonoRemitente) {
        ConversacionWhatsappEntidad conversacion = obtenerOCrearConversacion(telefonoRemitente);
        conversacion.setFlujo("HORARIOS");

        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdAndActivaTrue(obtenerEmpresaId()).stream()
                .sorted(Comparator.comparing(SucursalEntidad::getNombre))
                .toList();
        if (sucursales.isEmpty()) {
            limpiarConversacion(telefonoRemitente);
            return "En este momento no tenemos sucursales activas para mostrar horarios.";
        }

        if (sucursales.size() == 1) {
            conversacion.setCitaId(null);
            conversacion.setSucursalId(sucursales.getFirst().getId());
            conversacion.setGrupoId(null);
            conversacion.setSubgrupoId(null);
            conversacion.setServicioId(null);
            conversacion.setPaso("HORARIOS_GRUPO");
            guardarConversacion(conversacion);
            return construirSiguientePreguntaCatalogo(conversacion, "Perfecto, revisemos horarios.");
        }

        conversacion.setPaso("HORARIOS_SUCURSAL");
        conversacion.setCitaId(null);
        conversacion.setSucursalId(null);
        conversacion.setGrupoId(null);
        conversacion.setSubgrupoId(null);
        conversacion.setServicioId(null);
        guardarConversacion(conversacion);
        return construirPreguntaSucursales("Perfecto, revisemos horarios. Primero dime la sucursal.");
    }

    private String iniciarFlujoReagendar(String telefonoRemitente, String mensaje) {
        ClienteEntidad cliente = obtenerClientePorTelefono(telefonoRemitente);
        List<CitaClienteResponse> citas = obtenerCitasCancelablesCliente(cliente);

        if (citas.isEmpty()) {
            limpiarConversacion(telefonoRemitente);
            return "No encontré una cita activa para reagendar. Responde MIS CITAS para revisar tus opciones o dime: Quiero agendar.";
        }

        String opcion = extraerOpcionSimple(mensaje);
        if (opcion != null) {
            Long citaId = resolverCitaPorOpcion(cliente, opcion, false);
            return prepararFlujoReagendar(obtenerOCrearConversacion(telefonoRemitente), telefonoRemitente, citaId, citas);
        }

        if (citas.size() == 1) {
            return prepararFlujoReagendar(obtenerOCrearConversacion(telefonoRemitente), telefonoRemitente, citas.getFirst().id(), citas);
        }

        ConversacionWhatsappEntidad conversacion = obtenerOCrearConversacion(telefonoRemitente);
        conversacion.setFlujo("REAGENDAR");
        conversacion.setPaso("REAGENDAR_CITA");
        conversacion.setCitaId(null);
        conversacion.setSucursalId(null);
        conversacion.setGrupoId(null);
        conversacion.setSubgrupoId(null);
        conversacion.setServicioId(null);
        conversacion.setFechaSeleccionada(null);
        conversacion.setHoraSeleccionada(null);
        guardarConversacion(conversacion);
        return construirPreguntaReagendarCita(citas);
    }

    private String continuarConversacion(ConversacionWhatsappEntidad conversacion, String telefonoRemitente, String mensaje) {
        return switch (conversacion.getPaso()) {
            case "AGENDAR_SUCURSAL", "HORARIOS_SUCURSAL" -> procesarSucursalConversacion(conversacion, mensaje);
            case "AGENDAR_GRUPO", "HORARIOS_GRUPO" -> procesarGrupoConversacion(conversacion, mensaje);
            case "AGENDAR_SUBGRUPO", "HORARIOS_SUBGRUPO" -> procesarSubgrupoConversacion(conversacion, mensaje);
            case "AGENDAR_SERVICIO", "HORARIOS_SERVICIO" -> procesarServicioConversacion(conversacion, mensaje);
            case "AGENDAR_FECHA", "HORARIOS_FECHA" -> procesarFechaConversacion(conversacion, mensaje);
            case "AGENDAR_HORA" -> procesarHoraAgendar(conversacion, telefonoRemitente, mensaje);
            case "AGENDAR_CONFIRMAR_DATOS" -> procesarConfirmacionDatosAgendar(conversacion, telefonoRemitente, mensaje);
            case "AGENDAR_NOMBRE" -> procesarNombreAgendar(conversacion, mensaje);
            case "AGENDAR_CORREO" -> procesarCorreoAgendar(conversacion, telefonoRemitente, mensaje);
            case "REAGENDAR_CITA" -> procesarSeleccionReagendar(conversacion, telefonoRemitente, mensaje);
            case "REAGENDAR_FECHA" -> procesarFechaReagendar(conversacion, telefonoRemitente, mensaje);
            case "REAGENDAR_HORA" -> procesarHoraReagendar(conversacion, telefonoRemitente, mensaje);
            case "DETALLE_CONFIRMACION" -> procesarConfirmacionDetalle(conversacion, telefonoRemitente, mensaje);
            default -> {
                limpiarConversacion(telefonoRemitente);
                yield bienvenidaTexto();
            }
        };
    }

    private String procesarSucursalConversacion(ConversacionWhatsappEntidad conversacion, String mensaje) {
        SucursalEntidad sucursal = resolverSucursalPorTexto(mensaje);
        if (sucursal == null) {
            return construirPreguntaSucursales("No identifiqué la sucursal. Respóndeme con el número o con parte del nombre.");
        }

        conversacion.setSucursalId(sucursal.getId());
        conversacion.setGrupoId(null);
        conversacion.setSubgrupoId(null);
        conversacion.setServicioId(null);
        conversacion.setFechaSeleccionada(null);
        conversacion.setHoraSeleccionada(null);
        conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_GRUPO" : "AGENDAR_GRUPO");
        guardarConversacion(conversacion);
        return construirSiguientePreguntaCatalogo(conversacion, "Perfecto.");
    }

    private String procesarGrupoConversacion(ConversacionWhatsappEntidad conversacion, String mensaje) {
        if (conversacion.getSucursalId() == null) {
            conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_SUCURSAL" : "AGENDAR_SUCURSAL");
            guardarConversacion(conversacion);
            return construirPreguntaSucursales("Necesito primero la sucursal.");
        }

        if (seleccionoGrupoSinCategoria(conversacion.getSucursalId(), mensaje)) {
            conversacion.setGrupoId(GRUPO_SIN_CATEGORIA_ID);
            conversacion.setSubgrupoId(null);
            conversacion.setServicioId(null);
            conversacion.setFechaSeleccionada(null);
            conversacion.setHoraSeleccionada(null);
            conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_SERVICIO" : "AGENDAR_SERVICIO");
            guardarConversacion(conversacion);
            return construirPreguntaServicios(conversacion.getSucursalId(), GRUPO_SIN_CATEGORIA_ID, null, "Perfecto.");
        }

        GrupoServicioEntidad grupo = resolverGrupoPorTexto(conversacion.getSucursalId(), mensaje);
        if (grupo == null) {
            return construirPreguntaGrupos(conversacion.getSucursalId(), "No identifiqué la categoría. Elige una opción.");
        }

        conversacion.setGrupoId(grupo.getId());
        conversacion.setSubgrupoId(null);
        conversacion.setServicioId(null);
        conversacion.setFechaSeleccionada(null);
        conversacion.setHoraSeleccionada(null);
        guardarConversacion(conversacion);
        return construirSiguientePreguntaDespuesDeGrupo(conversacion, "Perfecto.");
    }

    private String procesarSubgrupoConversacion(ConversacionWhatsappEntidad conversacion, String mensaje) {
        if (conversacion.getSucursalId() == null || conversacion.getGrupoId() == null) {
            conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_GRUPO" : "AGENDAR_GRUPO");
            guardarConversacion(conversacion);
            return construirPreguntaGrupos(conversacion.getSucursalId(), "Vamos a retomar desde la categoría.");
        }

        if (seleccionoSubgrupoSinSubcategoria(conversacion.getSucursalId(), conversacion.getGrupoId(), mensaje)) {
            conversacion.setSubgrupoId(null);
            conversacion.setServicioId(null);
            conversacion.setFechaSeleccionada(null);
            conversacion.setHoraSeleccionada(null);
            conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_SERVICIO" : "AGENDAR_SERVICIO");
            guardarConversacion(conversacion);
            return construirPreguntaServicios(conversacion.getSucursalId(), conversacion.getGrupoId(), null, "Perfecto.");
        }

        SubgrupoServicioEntidad subgrupo = resolverSubgrupoPorTexto(conversacion.getSucursalId(), conversacion.getGrupoId(), mensaje);
        if (subgrupo == null) {
            return construirPreguntaSubgrupos(conversacion.getSucursalId(), conversacion.getGrupoId(), "No identifiqué la subcategoría. Elige una opción.");
        }

        conversacion.setSubgrupoId(subgrupo.getId());
        conversacion.setServicioId(null);
        conversacion.setFechaSeleccionada(null);
        conversacion.setHoraSeleccionada(null);
        conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_SERVICIO" : "AGENDAR_SERVICIO");
        guardarConversacion(conversacion);
        return construirPreguntaServicios(conversacion.getSucursalId(), conversacion.getGrupoId(), subgrupo.getId(), "Perfecto.");
    }

    private String procesarServicioConversacion(ConversacionWhatsappEntidad conversacion, String mensaje) {
        if (conversacion.getSucursalId() == null) {
            conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_SUCURSAL" : "AGENDAR_SUCURSAL");
            guardarConversacion(conversacion);
            return construirPreguntaSucursales("Necesito primero la sucursal.");
        }

        ServicioEntidad servicio = resolverServicioPorTexto(conversacion.getSucursalId(), conversacion.getGrupoId(), conversacion.getSubgrupoId(), mensaje);
        if (servicio == null) {
            return construirPreguntaServicios(conversacion.getSucursalId(), conversacion.getGrupoId(), conversacion.getSubgrupoId(), "No identifiqué el servicio. Elige una opción.");
        }

        conversacion.setServicioId(servicio.getId());
        conversacion.setFechaSeleccionada(null);
        conversacion.setHoraSeleccionada(null);
        conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_FECHA" : "AGENDAR_FECHA");
        guardarConversacion(conversacion);
        return """
                Excelente. Ahora dime la fecha que prefieres.

                Puedes escribir:
                - hoy
                - mañana
                - pasado mañana
                - 2026-04-05
                """.trim();
    }

    private String procesarFechaConversacion(ConversacionWhatsappEntidad conversacion, String mensaje) {
        if (conversacion.getSucursalId() == null || conversacion.getServicioId() == null) {
            conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_SUCURSAL" : "AGENDAR_SUCURSAL");
            guardarConversacion(conversacion);
            return construirPreguntaSucursales("Vamos a retomar desde la sucursal para ayudarte mejor.");
        }

        LocalDate fecha = parsearFechaFlexible(mensaje);
        List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                obtenerEmpresaId(),
                conversacion.getSucursalId(),
                conversacion.getServicioId(),
                null,
                fecha
        );

        if (franjas.isEmpty()) {
            return "No encontré horarios disponibles para esa fecha. Prueba con otra fecha, por ejemplo mañana o 2026-04-05.";
        }

        conversacion.setFechaSeleccionada(fecha);
        if ("HORARIOS".equals(conversacion.getFlujo())) {
            guardarConversacion(conversacion);
            limpiarConversacion(conversacion);
            return construirRespuestaHorariosDisponibles(conversacion.getSucursalId(), conversacion.getServicioId(), fecha, franjas, false);
        }

        conversacion.setPaso("AGENDAR_HORA");
        guardarConversacion(conversacion);
        return construirRespuestaHorariosDisponibles(conversacion.getSucursalId(), conversacion.getServicioId(), fecha, franjas, true);
    }

    private String procesarHoraAgendar(ConversacionWhatsappEntidad conversacion, String telefonoRemitente, String mensaje) {
        if (conversacion.getSucursalId() == null || conversacion.getServicioId() == null || conversacion.getFechaSeleccionada() == null) {
            conversacion.setPaso("AGENDAR_SUCURSAL");
            guardarConversacion(conversacion);
            return construirPreguntaSucursales("Vamos a empezar de nuevo con la sucursal.");
        }

        String hora = extraerHora(mensaje);
        List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                obtenerEmpresaId(),
                conversacion.getSucursalId(),
                conversacion.getServicioId(),
                null,
                conversacion.getFechaSeleccionada()
        );

        boolean horaValida = franjas.stream()
                .map(franja -> java.time.OffsetDateTime.parse(franja.inicio()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .anyMatch(hora::equals);

        if (!horaValida) {
            return "No reconocí ese horario dentro de las opciones disponibles. Respóndeme solo con la hora, por ejemplo 10:30.";
        }

        conversacion.setHoraSeleccionada(hora);
        ClienteEntidad clienteExistente = obtenerClientePorTelefonoOpcional(telefonoRemitente);
        if (clienteExistente != null) {
            String correo = usuarioRepositorio.findByIdAndEmpresaId(clienteExistente.getUsuarioId(), obtenerEmpresaId())
                    .map(usuario -> usuario.getCorreo())
                    .orElse(null);
            if (tieneTexto(clienteExistente.getNombreCompleto()) && tieneTexto(correo)) {
                conversacion.setNombreCliente(clienteExistente.getNombreCompleto());
                conversacion.setCorreoCliente(correo);
                conversacion.setPaso("AGENDAR_CONFIRMAR_DATOS");
                guardarConversacion(conversacion);
                return """
                        Perfecto. Ya tengo estos datos:
                        - Nombre: %s
                        - Correo: %s

                        Si están correctos, responde CONFIRMAR DATOS.
                        Si deseas cambiarlos, envíame tu nombre completo.
                        """.formatted(clienteExistente.getNombreCompleto(), correo).trim();
            }
        }

        conversacion.setPaso("AGENDAR_NOMBRE");
        guardarConversacion(conversacion);
        return "Perfecto. Ahora compárteme tu nombre completo.";
    }

    private String procesarConfirmacionDatosAgendar(ConversacionWhatsappEntidad conversacion, String telefonoRemitente, String mensaje) {
        String mensajeNormalizado = normalizarTextoLibre(mensaje);
        if (mensajeNormalizado.equals("confirmar datos")
                || mensajeNormalizado.equals("confirmar")
                || mensajeNormalizado.equals("si")
                || mensajeNormalizado.equals("si estan correctos")
                || mensajeNormalizado.equals("correcto")) {
            return registrarCitaConversacion(conversacion, telefonoRemitente, conversacion.getCorreoCliente());
        }
        if (mensajeNormalizado.equals("no")
                || mensajeNormalizado.equals("cambiar")
                || mensajeNormalizado.equals("cambiar datos")
                || mensajeNormalizado.equals("editar")
                || mensajeNormalizado.equals("modificar")) {
            conversacion.setNombreCliente(null);
            conversacion.setCorreoCliente(null);
            conversacion.setPaso("AGENDAR_NOMBRE");
            guardarConversacion(conversacion);
            return "Claro. Compárteme tu nombre completo y actualizamos tus datos.";
        }

        String nombre = mensaje == null ? "" : mensaje.trim();
        if (nombre.length() < 3) {
            return "Si los datos son correctos, responde CONFIRMAR DATOS. Si quieres cambiarlos, envíame tu nombre completo.";
        }

        conversacion.setNombreCliente(nombre);
        conversacion.setCorreoCliente(null);
        conversacion.setPaso("AGENDAR_CORREO");
        guardarConversacion(conversacion);
        return "Gracias. Ahora compárteme tu correo electrónico.";
    }

    private String procesarNombreAgendar(ConversacionWhatsappEntidad conversacion, String mensaje) {
        String nombre = mensaje == null ? "" : mensaje.trim();
        if (nombre.length() < 3) {
            return "Necesito tu nombre completo para apartar la cita.";
        }

        conversacion.setNombreCliente(nombre);
        conversacion.setPaso("AGENDAR_CORREO");
        guardarConversacion(conversacion);
        return "Gracias. Ahora compárteme tu correo electrónico.";
    }

    private String procesarCorreoAgendar(ConversacionWhatsappEntidad conversacion, String telefonoRemitente, String mensaje) {
        String correo = mensaje == null ? "" : mensaje.trim().toLowerCase(Locale.ROOT);
        if (!correo.contains("@") || correo.startsWith("@") || correo.endsWith("@")) {
            return "Ese correo no parece válido. Compártemelo nuevamente, por ejemplo nombre@dominio.com.";
        }

        conversacion.setCorreoCliente(correo);
        guardarConversacion(conversacion);
        return registrarCitaConversacion(conversacion, telefonoRemitente, correo);
    }

    private String procesarSeleccionReagendar(ConversacionWhatsappEntidad conversacion, String telefonoRemitente, String mensaje) {
        ClienteEntidad cliente = obtenerClientePorTelefono(telefonoRemitente);
        Long citaId = resolverCitaPorOpcion(cliente, mensaje, false);
        return prepararFlujoReagendar(conversacion, telefonoRemitente, citaId, obtenerCitasCancelablesCliente(cliente));
    }

    private String procesarFechaReagendar(ConversacionWhatsappEntidad conversacion, String telefonoRemitente, String mensaje) {
        if (conversacion.getCitaId() == null || conversacion.getSucursalId() == null || conversacion.getServicioId() == null) {
            limpiarConversacion(telefonoRemitente);
            return "Perdí el contexto de la cita a reagendar. Responde MIS CITAS o dime REAGENDAR y lo retomamos.";
        }

        LocalDate fecha = parsearFechaFlexible(mensaje);
        List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                obtenerEmpresaId(),
                conversacion.getSucursalId(),
                conversacion.getServicioId(),
                null,
                fecha
        );

        if (franjas.isEmpty()) {
            return "No encontré horarios disponibles para esa fecha. Prueba con otra fecha, por ejemplo mañana o 2026-04-05.";
        }

        conversacion.setFechaSeleccionada(fecha);
        conversacion.setPaso("REAGENDAR_HORA");
        guardarConversacion(conversacion);
        return construirRespuestaHorariosDisponibles(conversacion.getSucursalId(), conversacion.getServicioId(), fecha, franjas, true);
    }

    private String procesarHoraReagendar(ConversacionWhatsappEntidad conversacion, String telefonoRemitente, String mensaje) {
        if (conversacion.getCitaId() == null
                || conversacion.getSucursalId() == null
                || conversacion.getServicioId() == null
                || conversacion.getFechaSeleccionada() == null) {
            limpiarConversacion(telefonoRemitente);
            return "Perdí el contexto de la cita a reagendar. Responde MIS CITAS o dime REAGENDAR y lo retomamos.";
        }

        String hora = extraerHora(mensaje);
        List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                obtenerEmpresaId(),
                conversacion.getSucursalId(),
                conversacion.getServicioId(),
                null,
                conversacion.getFechaSeleccionada()
        );

        boolean horaValida = franjas.stream()
                .map(franja -> java.time.OffsetDateTime.parse(franja.inicio()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .anyMatch(hora::equals);

        if (!horaValida) {
            return "No reconocí ese horario dentro de las opciones disponibles. Respóndeme solo con la hora, por ejemplo 10:30.";
        }

        conversacion.setHoraSeleccionada(hora);
        ClienteEntidad cliente = obtenerClientePorTelefono(telefonoRemitente);
        CitaClienteResponse citaActual = obtenerCitaActivaPorId(cliente, conversacion.getCitaId());
        ZoneId zona = ZoneId.of(resolverZonaHoraria(conversacion.getSucursalId()));
        LocalDateTime nuevaFecha = LocalDateTime.parse(
                conversacion.getFechaSeleccionada() + " " + conversacion.getHoraSeleccionada(),
                FORMATO_FECHA_HORA
        );
        CitaClienteResponse citaReprogramada = servicioCitasCliente.reprogramar(
                obtenerEmpresaId(),
                cliente.getUsuarioId(),
                conversacion.getCitaId(),
                nuevaFecha.atZone(zona).toOffsetDateTime()
        );

        limpiarConversacion(telefonoRemitente);
        return "Tu cita de %s, que estaba para el %s, fue reprogramada para el %s y quedó pendiente de confirmación.".formatted(
                citaActual.servicioNombre(),
                formatearFechaHoraCliente(citaActual.inicio()),
                formatearFechaHoraCliente(citaReprogramada.inicio())
        );
    }

    private String registrarCitaConversacion(ConversacionWhatsappEntidad conversacion, String telefonoRemitente, String correo) {
        if (conversacion.getSucursalId() == null
                || conversacion.getServicioId() == null
                || conversacion.getFechaSeleccionada() == null
                || conversacion.getHoraSeleccionada() == null
                || !tieneTexto(conversacion.getNombreCliente())
                || !tieneTexto(correo)) {
            limpiarConversacion(telefonoRemitente);
            return "Perdí parte del contexto de tu cita. Escríbeme \"Quiero agendar\" y lo retomamos enseguida.";
        }

        ZoneId zona = ZoneId.of(resolverZonaHoraria(conversacion.getSucursalId()));
        LocalDateTime fechaHora = LocalDateTime.parse(
                conversacion.getFechaSeleccionada() + " " + conversacion.getHoraSeleccionada(),
                FORMATO_FECHA_HORA
        );

        CitaCreadaResponse cita = servicioCitas.crearCita(new CrearCitaRequest(
                obtenerEmpresaId(),
                conversacion.getSucursalId(),
                conversacion.getServicioId(),
                null,
                conversacion.getNombreCliente(),
                correo,
                NormalizadorTelefonoWhatsapp.normalizarComparable(telefonoRemitente),
                fechaHora.atZone(zona).toOffsetDateTime(),
                null
        ));

        limpiarConversacion(telefonoRemitente);
        return "Listo, registré tu cita de %s para el %s. En un momento te enviaré la solicitud de confirmación por este mismo chat.".formatted(
                obtenerNombreServicio(conversacion.getServicioId()),
                cita.inicio().format(FORMATO_RESPUESTA)
        );
    }

    private String prepararFlujoReagendar(
            ConversacionWhatsappEntidad conversacion,
            String telefonoRemitente,
            Long citaId,
            List<CitaClienteResponse> citasDisponibles
    ) {
        CitaClienteResponse cita = citasDisponibles.stream()
                .filter(item -> item.id().equals(citaId))
                .findFirst()
                .orElseGet(() -> {
                    ClienteEntidad cliente = obtenerClientePorTelefono(telefonoRemitente);
                    return obtenerCitaActivaPorId(cliente, citaId);
                });

        conversacion.setFlujo("REAGENDAR");
        conversacion.setPaso("REAGENDAR_FECHA");
        conversacion.setCitaId(cita.id());
        conversacion.setSucursalId(cita.sucursalId());
        conversacion.setServicioId(cita.servicioId());
        conversacion.setFechaSeleccionada(null);
        conversacion.setHoraSeleccionada(null);
        guardarConversacion(conversacion);

        return """
                Claro. Vamos a cambiar esta cita:
                %s

                Ahora dime la nueva fecha que prefieres.

                Puedes escribir:
                - hoy
                - mañana
                - pasado mañana
                - 2026-04-05
                """.formatted(formatearCitaVisible(cita)).trim();
    }

    private String construirPreguntaSucursales(String encabezado) {
        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdAndActivaTrue(obtenerEmpresaId()).stream()
                .sorted(Comparator.comparing(SucursalEntidad::getNombre))
                .toList();
        List<OpcionListPicker> opciones = sucursales.stream()
                .map(sucursal -> new OpcionListPicker(
                        "SUCURSAL|" + sucursal.getId(),
                        nombreVisibleSucursal(sucursal.getNombre()),
                        tieneTexto(sucursal.getDireccion()) ? sucursal.getDireccion() : "Seleccionar sucursal"
                ))
                .toList();
        StringBuilder respuesta = new StringBuilder(encabezado).append("\n\nSucursales disponibles:\n");
        for (int i = 0; i < sucursales.size(); i++) {
            respuesta.append(i + 1)
                    .append(". ")
                    .append(nombreVisibleSucursal(sucursales.get(i).getNombre()))
                    .append("\n");
        }
        respuesta.append("\nRespóndeme con el número o con parte del nombre de la sucursal.");
        String fallback = respuesta.toString().trim();
        return construirRespuestaListPicker(fallback, encabezado, "Elegir sucursal", opciones, USO_SELECCIONAR_SUCURSAL);
    }

    private String construirSiguientePreguntaCatalogo(ConversacionWhatsappEntidad conversacion, String encabezado) {
        List<GrupoServicioEntidad> grupos = gruposDisponiblesParaSucursal(conversacion.getSucursalId());
        boolean tieneServiciosSinGrupo = serviciosDisponibles(conversacion.getSucursalId(), GRUPO_SIN_CATEGORIA_ID, null).size() > 0;
        if (!grupos.isEmpty() || tieneServiciosSinGrupo) {
            conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_GRUPO" : "AGENDAR_GRUPO");
            guardarConversacion(conversacion);
            return construirPreguntaGrupos(conversacion.getSucursalId(), encabezado);
        }

        conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_SERVICIO" : "AGENDAR_SERVICIO");
        guardarConversacion(conversacion);
        return construirPreguntaServicios(conversacion.getSucursalId(), null, null, encabezado);
    }

    private String construirSiguientePreguntaDespuesDeGrupo(ConversacionWhatsappEntidad conversacion, String encabezado) {
        List<SubgrupoServicioEntidad> subgrupos = subgruposDisponiblesParaGrupo(conversacion.getSucursalId(), conversacion.getGrupoId());
        boolean tieneServiciosSinSubgrupo = !serviciosSinSubgrupo(conversacion.getSucursalId(), conversacion.getGrupoId()).isEmpty();
        if (!subgrupos.isEmpty()) {
            conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_SUBGRUPO" : "AGENDAR_SUBGRUPO");
            guardarConversacion(conversacion);
            return construirPreguntaSubgrupos(conversacion.getSucursalId(), conversacion.getGrupoId(), encabezado);
        }

        conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_SERVICIO" : "AGENDAR_SERVICIO");
        guardarConversacion(conversacion);
        return construirPreguntaServicios(conversacion.getSucursalId(), conversacion.getGrupoId(), tieneServiciosSinSubgrupo ? null : conversacion.getSubgrupoId(), encabezado);
    }

    private String construirPreguntaGrupos(Long sucursalId, String encabezado) {
        List<GrupoServicioEntidad> grupos = gruposDisponiblesParaSucursal(sucursalId);
        boolean tieneServiciosSinGrupo = !serviciosDisponibles(sucursalId, GRUPO_SIN_CATEGORIA_ID, null).isEmpty();
        List<OpcionListPicker> opciones = new ArrayList<>();
        for (GrupoServicioEntidad grupo : grupos) {
            opciones.add(new OpcionListPicker(
                    "GRUPO|" + grupo.getId(),
                    grupo.getNombre(),
                    tieneTexto(grupo.getDescripcion()) ? grupo.getDescripcion() : "Categoría de servicios"
            ));
        }
        if (tieneServiciosSinGrupo) {
            opciones.add(new OpcionListPicker(
                    "GRUPO|SIN_CATEGORIA",
                    "Otros servicios",
                    "Servicios sin categoría"
            ));
        }

        if (opciones.isEmpty()) {
            return construirPreguntaServicios(sucursalId, null, null, encabezado);
        }

        StringBuilder respuesta = new StringBuilder(encabezado).append("\n\nCategorías disponibles:\n");
        for (int i = 0; i < opciones.size(); i++) {
            respuesta.append(i + 1)
                    .append(". ")
                    .append(opciones.get(i).titulo())
                    .append("\n");
        }
        respuesta.append("\nRespóndeme con el número o el nombre de la categoría.");
        return construirRespuestaListPicker(respuesta.toString().trim(), encabezado, "Elegir categoría", opciones, USO_CATEGORIA_SERVICIO);
    }

    private String construirPreguntaSubgrupos(Long sucursalId, Long grupoId, String encabezado) {
        List<SubgrupoServicioEntidad> subgrupos = subgruposDisponiblesParaGrupo(sucursalId, grupoId);
        List<OpcionListPicker> opciones = new ArrayList<>(subgrupos.stream()
                .map(subgrupo -> new OpcionListPicker(
                        "SUBGRUPO|" + subgrupo.getId(),
                        subgrupo.getNombre(),
                        tieneTexto(subgrupo.getDescripcion()) ? subgrupo.getDescripcion() : "Subcategoría de servicios"
                ))
                .toList());
        if (!serviciosSinSubgrupo(sucursalId, grupoId).isEmpty()) {
            opciones.add(new OpcionListPicker(
                    "SUBGRUPO|SIN_SUBGRUPO",
                    "Otros servicios",
                    "Servicios sin subcategoría"
            ));
        }

        if (opciones.isEmpty()) {
            return construirPreguntaServicios(sucursalId, grupoId, null, encabezado);
        }

        StringBuilder respuesta = new StringBuilder(encabezado).append("\n\nSubcategorías disponibles:\n");
        for (int i = 0; i < opciones.size(); i++) {
            respuesta.append(i + 1)
                    .append(". ")
                    .append(opciones.get(i).titulo())
                    .append("\n");
        }
        respuesta.append("\nRespóndeme con el número o el nombre de la subcategoría.");
        return construirRespuestaListPicker(respuesta.toString().trim(), encabezado, "Elegir subcategoría", opciones, USO_SUBCATEGORIA_GENERICO);
    }

    private String construirPreguntaServicios(Long sucursalId, Long grupoId, Long subgrupoId, String encabezado) {
        SucursalEntidad sucursal = sucursalRepositorio.findById(sucursalId).orElse(null);
        List<ServicioEntidad> servicios = serviciosDisponibles(sucursalId, grupoId, subgrupoId);
        if (servicios.isEmpty()) {
            return "No encontré servicios activos para esa sucursal. Si deseas, responde Ubicación o Mis citas.";
        }

        List<OpcionListPicker> opciones = servicios.stream()
                .map(servicio -> new OpcionListPicker(
                        "SERVICIO|" + servicio.getId(),
                        servicio.getNombre(),
                        "%d min".formatted(servicio.getDuracionMinutos())
                ))
                .toList();
        StringBuilder respuesta = new StringBuilder(encabezado)
                .append("\n\n")
                .append("Sucursal: ")
                .append(sucursal != null ? nombreVisibleSucursal(sucursal.getNombre()) : "Seleccionada")
                .append("\n")
                .append("Servicios disponibles:\n");
        for (int i = 0; i < servicios.size(); i++) {
            ServicioEntidad servicio = servicios.get(i);
            respuesta.append(i + 1)
                    .append(". ")
                    .append(servicio.getNombre())
                    .append(" · ")
                    .append(servicio.getDuracionMinutos())
                    .append(" min\n");
        }
        respuesta.append("\nRespóndeme con el número o con el nombre del servicio que te interesa.");
        String fallback = respuesta.toString().trim();
        String body = "%s\nSucursal: %s".formatted(
                encabezado,
                sucursal != null ? nombreVisibleSucursal(sucursal.getNombre()) : "Seleccionada"
        );
        return construirRespuestaListPicker(fallback, body, "Elegir servicio", opciones, USO_SERVICIO_GENERICO);
    }

    private String construirPreguntaReagendarCita(List<CitaClienteResponse> citas) {
        StringBuilder respuesta = new StringBuilder("Claro. ¿Cuál de tus citas deseas cambiar?\n");
        for (int i = 0; i < citas.size(); i++) {
            respuesta.append(i + 1)
                    .append(". ")
                    .append(formatearCitaVisible(citas.get(i)))
                    .append("\n");
        }
        respuesta.append("\nRespóndeme con el número de la cita que quieres reagendar.");
        return respuesta.toString().trim();
    }

    private String construirRespuestaHorariosDisponibles(Long sucursalId, Long servicioId, LocalDate fecha, List<FranjaDisponibleResponse> franjas, boolean paraAgendar) {
        ServicioEntidad servicio = servicioRepositorio.findById(servicioId).orElse(null);
        SucursalEntidad sucursal = sucursalRepositorio.findById(sucursalId).orElse(null);
        ZoneId zona = ZoneId.of(sucursal != null ? sucursal.getZonaHoraria() : "America/Mexico_City");
        List<OpcionListPicker> opciones = new ArrayList<>();

        StringBuilder respuesta = new StringBuilder("Encontré estos horarios disponibles");
        if (servicio != null) {
            respuesta.append(" para ").append(servicio.getNombre());
        }
        respuesta.append(" el ")
                .append(fecha.format(FORMATO_FECHA_AMIGABLE))
                .append(":\n");

        franjas.stream().limit(8).forEach(franja -> {
            String hora = java.time.OffsetDateTime.parse(franja.inicio()).atZoneSameInstant(zona).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            respuesta.append("- ").append(hora).append("\n");
            opciones.add(new OpcionListPicker(
                    "HORA|" + hora,
                    hora,
                    "Disponible"
            ));
        });

        if (paraAgendar) {
            respuesta.append("\nRespóndeme solo con la hora que prefieras, por ejemplo 10:30.");
        } else {
            respuesta.append("\nSi quieres apartar uno de estos horarios, responde: Quiero agendar.");
        }
        String fallback = respuesta.toString().trim();
        if (!paraAgendar) {
            return fallback;
        }
        String body = "Encontré estos horarios%s el %s.".formatted(
                servicio != null ? " para " + servicio.getNombre() : "",
                fecha.format(FORMATO_FECHA_AMIGABLE)
        );
        return construirRespuestaListPicker(fallback, body, "Elegir horario", opciones);
    }

    private ConversacionWhatsappEntidad obtenerConversacion(String telefonoRemitente) {
        ConversacionWhatsappEntidad conversacion = conversacionWhatsappRepositorio.findByEmpresaIdAndTelefonoNormalizado(
                obtenerEmpresaId(),
                NormalizadorTelefonoWhatsapp.normalizarComparable(telefonoRemitente)
        ).orElse(null);
        if (conversacion == null) {
            return null;
        }
        if (conversacion.getActualizadaEn() != null
                && conversacion.getActualizadaEn().isBefore(LocalDateTime.now().minusMinutes(MINUTOS_EXPIRACION_CONVERSACION))) {
            conversacionWhatsappRepositorio.delete(conversacion);
            return null;
        }
        return conversacion;
    }

    private boolean esCancelarFlujoConversacional(ConversacionWhatsappEntidad conversacion, String mensajeNormalizado) {
        return tienePasoConversacionalActivo(conversacion)
                && (mensajeNormalizado.equals("cancelar")
                || mensajeNormalizado.equals("cancelar proceso")
                || mensajeNormalizado.equals("salir"));
    }

    private boolean debePriorizarConversacion(ConversacionWhatsappEntidad conversacion, String mensajeMayus, String mensajeNormalizado) {
        if (!tienePasoConversacionalActivo(conversacion)) {
            return false;
        }
        if ("MIS CITAS".equals(mensajeMayus) || "SUCURSALES".equals(mensajeMayus) || esMensajeMenu(mensajeNormalizado)) {
            return false;
        }
        if ("AGENDAR_CONFIRMAR_DATOS".equals(conversacion.getPaso())) {
            return true;
        }
        if (mensajeMayus.startsWith("CONFIRMAR ")
                || mensajeMayus.startsWith("CANCELAR ")
                || mensajeMayus.startsWith("REAGENDAR")
                || mensajeMayus.startsWith("AGENDAR|")
                || mensajeMayus.startsWith("HORARIOS ")) {
            return false;
        }
        return true;
    }

    private boolean tienePasoConversacionalActivo(ConversacionWhatsappEntidad conversacion) {
        return conversacion != null
                && tieneTexto(conversacion.getPaso())
                && !"MENU".equalsIgnoreCase(conversacion.getPaso());
    }

    private ConversacionWhatsappEntidad obtenerOCrearConversacion(String telefonoRemitente) {
        return conversacionWhatsappRepositorio.findByEmpresaIdAndTelefonoNormalizado(
                obtenerEmpresaId(),
                NormalizadorTelefonoWhatsapp.normalizarComparable(telefonoRemitente)
        ).orElseGet(() -> {
            ConversacionWhatsappEntidad conversacion = new ConversacionWhatsappEntidad();
            conversacion.setEmpresaId(obtenerEmpresaId());
            conversacion.setTelefonoNormalizado(NormalizadorTelefonoWhatsapp.normalizarComparable(telefonoRemitente));
            conversacion.setFlujo("NINGUNO");
            conversacion.setPaso("MENU");
            conversacion.setActualizadaEn(LocalDateTime.now());
            return conversacion;
        });
    }

    private void guardarConversacion(ConversacionWhatsappEntidad conversacion) {
        conversacion.setActualizadaEn(LocalDateTime.now());
        conversacionWhatsappRepositorio.save(conversacion);
    }

    private void limpiarConversacion(String telefonoRemitente) {
        ConversacionWhatsappEntidad conversacion = obtenerConversacion(telefonoRemitente);
        if (conversacion != null) {
            conversacionWhatsappRepositorio.delete(conversacion);
        }
    }

    private void limpiarConversacion(ConversacionWhatsappEntidad conversacion) {
        if (conversacion.getId() != null) {
            conversacionWhatsappRepositorio.delete(conversacion);
        }
    }

    private SucursalEntidad resolverSucursalPorTexto(String texto) {
        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdAndActivaTrue(obtenerEmpresaId()).stream()
                .sorted(Comparator.comparing(SucursalEntidad::getNombre))
                .toList();
        Long sucursalIdPayload = extraerIdPayload(texto, "SUCURSAL");
        if (sucursalIdPayload != null) {
            return sucursales.stream()
                    .filter(sucursal -> sucursal.getId().equals(sucursalIdPayload))
                    .findFirst()
                    .orElse(null);
        }

        Integer opcion = parsearOpcion(texto);
        if (opcion != null && opcion >= 1 && opcion <= sucursales.size()) {
            return sucursales.get(opcion - 1);
        }

        String buscado = normalizarTextoLibre(texto);
        return sucursales.stream()
                .filter(sucursal -> coincideTexto(buscado, sucursal.getNombre()))
                .findFirst()
                .orElse(null);
    }

    private GrupoServicioEntidad resolverGrupoPorTexto(Long sucursalId, String texto) {
        List<GrupoServicioEntidad> grupos = gruposDisponiblesParaSucursal(sucursalId);
        Long grupoIdPayload = extraerIdPayload(texto, "GRUPO");
        if (grupoIdPayload != null) {
            return grupos.stream()
                    .filter(grupo -> grupo.getId().equals(grupoIdPayload))
                    .findFirst()
                    .orElse(null);
        }

        Integer opcion = parsearOpcion(texto);
        boolean tieneServiciosSinGrupo = !serviciosDisponibles(sucursalId, GRUPO_SIN_CATEGORIA_ID, null).isEmpty();
        int totalOpciones = grupos.size() + (tieneServiciosSinGrupo ? 1 : 0);
        if (opcion != null && opcion >= 1 && opcion <= totalOpciones) {
            if (opcion <= grupos.size()) {
                return grupos.get(opcion - 1);
            }
            return null;
        }

        String buscado = normalizarTextoLibre(texto);
        return grupos.stream()
                .filter(grupo -> coincideTexto(buscado, grupo.getNombre()))
                .findFirst()
                .orElse(null);
    }

    private boolean seleccionoGrupoSinCategoria(Long sucursalId, String texto) {
        if (serviciosDisponibles(sucursalId, GRUPO_SIN_CATEGORIA_ID, null).isEmpty()) {
            return false;
        }
        String payload = extraerPayload(texto, "GRUPO");
        if ("SIN_CATEGORIA".equalsIgnoreCase(payload)) {
            return true;
        }
        Integer opcion = parsearOpcion(texto);
        if (opcion != null) {
            return opcion == gruposDisponiblesParaSucursal(sucursalId).size() + 1;
        }
        String normalizado = normalizarTextoLibre(texto);
        return normalizado.equals("otros")
                || normalizado.equals("otros servicios")
                || normalizado.equals("sin categoria");
    }

    private SubgrupoServicioEntidad resolverSubgrupoPorTexto(Long sucursalId, Long grupoId, String texto) {
        List<SubgrupoServicioEntidad> subgrupos = subgruposDisponiblesParaGrupo(sucursalId, grupoId);
        Long subgrupoIdPayload = extraerIdPayload(texto, "SUBGRUPO");
        if (subgrupoIdPayload != null) {
            return subgrupos.stream()
                    .filter(subgrupo -> subgrupo.getId().equals(subgrupoIdPayload))
                    .findFirst()
                    .orElse(null);
        }

        Integer opcion = parsearOpcion(texto);
        boolean tieneServiciosSinSubgrupo = !serviciosSinSubgrupo(sucursalId, grupoId).isEmpty();
        int totalOpciones = subgrupos.size() + (tieneServiciosSinSubgrupo ? 1 : 0);
        if (opcion != null && opcion >= 1 && opcion <= totalOpciones) {
            if (opcion <= subgrupos.size()) {
                return subgrupos.get(opcion - 1);
            }
            return null;
        }

        String buscado = normalizarTextoLibre(texto);
        return subgrupos.stream()
                .filter(subgrupo -> coincideTexto(buscado, subgrupo.getNombre()))
                .findFirst()
                .orElse(null);
    }

    private boolean seleccionoSubgrupoSinSubcategoria(Long sucursalId, Long grupoId, String texto) {
        if (serviciosSinSubgrupo(sucursalId, grupoId).isEmpty()) {
            return false;
        }
        String payload = extraerPayload(texto, "SUBGRUPO");
        if ("SIN_SUBGRUPO".equalsIgnoreCase(payload)) {
            return true;
        }
        Integer opcion = parsearOpcion(texto);
        if (opcion != null) {
            return opcion == subgruposDisponiblesParaGrupo(sucursalId, grupoId).size() + 1;
        }
        String normalizado = normalizarTextoLibre(texto);
        return normalizado.equals("otros")
                || normalizado.equals("otros servicios")
                || normalizado.equals("sin subcategoria")
                || normalizado.equals("sin subgrupo");
    }

    private ServicioEntidad resolverServicioPorTexto(Long sucursalId, Long grupoId, Long subgrupoId, String texto) {
        List<ServicioEntidad> servicios = serviciosDisponibles(sucursalId, grupoId, subgrupoId);
        Long servicioIdPayload = extraerIdPayload(texto, "SERVICIO");
        if (servicioIdPayload != null) {
            return servicios.stream()
                    .filter(servicio -> servicio.getId().equals(servicioIdPayload))
                    .findFirst()
                    .orElse(null);
        }

        Integer opcion = parsearOpcion(texto);
        if (opcion != null && opcion >= 1 && opcion <= servicios.size()) {
            return servicios.get(opcion - 1);
        }

        String buscado = normalizarTextoLibre(texto);
        return servicios.stream()
                .filter(servicio -> coincideTexto(buscado, servicio.getNombre()))
                .findFirst()
                .orElse(null);
    }

    private List<GrupoServicioEntidad> gruposDisponiblesParaSucursal(Long sucursalId) {
        if (sucursalId == null) {
            return List.of();
        }
        List<ServicioEntidad> servicios = servicioRepositorio.findBySucursalIdAndActivoTrue(sucursalId);
        List<Long> grupoIds = servicios.stream()
                .map(ServicioEntidad::getGrupoId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (grupoIds.isEmpty()) {
            return List.of();
        }
        return grupoServicioRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(obtenerEmpresaId()).stream()
                .filter(GrupoServicioEntidad::isActivo)
                .filter(grupo -> grupoIds.contains(grupo.getId()))
                .toList();
    }

    private List<SubgrupoServicioEntidad> subgruposDisponiblesParaGrupo(Long sucursalId, Long grupoId) {
        if (sucursalId == null || grupoId == null || GRUPO_SIN_CATEGORIA_ID.equals(grupoId)) {
            return List.of();
        }
        List<ServicioEntidad> servicios = servicioRepositorio.findBySucursalIdAndActivoTrue(sucursalId);
        List<Long> subgrupoIds = servicios.stream()
                .filter(servicio -> grupoId.equals(servicio.getGrupoId()))
                .map(ServicioEntidad::getSubgrupoId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (subgrupoIds.isEmpty()) {
            return List.of();
        }
        return subgrupoServicioRepositorio.findByEmpresaIdAndGrupoIdOrderByOrdenPublicoAscNombreAsc(obtenerEmpresaId(), grupoId).stream()
                .filter(SubgrupoServicioEntidad::isActivo)
                .filter(subgrupo -> subgrupoIds.contains(subgrupo.getId()))
                .toList();
    }

    private List<ServicioEntidad> serviciosSinSubgrupo(Long sucursalId, Long grupoId) {
        if (sucursalId == null || grupoId == null || GRUPO_SIN_CATEGORIA_ID.equals(grupoId)) {
            return List.of();
        }
        return servicioRepositorio.findBySucursalIdAndActivoTrue(sucursalId).stream()
                .filter(servicio -> grupoId.equals(servicio.getGrupoId()))
                .filter(servicio -> servicio.getSubgrupoId() == null)
                .sorted(Comparator.comparingInt(ServicioEntidad::getOrdenPublico).thenComparing(ServicioEntidad::getNombre))
                .toList();
    }

    private List<ServicioEntidad> serviciosDisponibles(Long sucursalId, Long grupoId, Long subgrupoId) {
        if (sucursalId == null) {
            return List.of();
        }
        List<ServicioEntidad> servicios = servicioRepositorio.findBySucursalIdAndActivoTrue(sucursalId).stream()
                .sorted(Comparator.comparingInt(ServicioEntidad::getOrdenPublico).thenComparing(ServicioEntidad::getNombre))
                .toList();
        if (grupoId == null) {
            return servicios;
        }
        if (GRUPO_SIN_CATEGORIA_ID.equals(grupoId)) {
            return servicios.stream()
                    .filter(servicio -> servicio.getGrupoId() == null)
                    .toList();
        }
        List<SubgrupoServicioEntidad> subgrupos = subgruposDisponiblesParaGrupo(sucursalId, grupoId);
        return servicios.stream()
                .filter(servicio -> grupoId.equals(servicio.getGrupoId()))
                .filter(servicio -> {
                    if (subgrupoId != null) {
                        return subgrupoId.equals(servicio.getSubgrupoId());
                    }
                    if (!subgrupos.isEmpty()) {
                        return servicio.getSubgrupoId() == null;
                    }
                    return true;
                })
                .toList();
    }

    private boolean coincideTexto(String buscado, String candidato) {
        if (!tieneTexto(buscado) || !tieneTexto(candidato)) {
            return false;
        }
        String normalizado = normalizarTextoLibre(candidato);
        return normalizado.contains(buscado) || buscado.contains(normalizado);
    }

    private LocalDate parsearFechaFlexible(String valor) {
        String normalizado = normalizarTextoLibre(valor);
        LocalDate hoy = LocalDate.now();
        if (Objects.equals(normalizado, "hoy")) {
            return hoy;
        }
        if (Objects.equals(normalizado, "manana")) {
            return hoy.plusDays(1);
        }
        if (Objects.equals(normalizado, "pasado manana")) {
            return hoy.plusDays(2);
        }
        return parsearFecha(valor);
    }

    private String extraerHora(String valor) {
        String candidato = extraerPayload(valor, "HORA");
        if (!tieneTexto(candidato)) {
            candidato = valor == null ? "" : valor.trim();
        }
        if (!SOLO_HORA.matcher(candidato).matches()) {
            throw new IllegalArgumentException("Respóndeme solo con la hora en formato HH:mm, por ejemplo 10:30.");
        }
        return candidato;
    }

    private boolean esIntencionAgendar(String mensajeNormalizado) {
        return mensajeNormalizado.contains("agendar")
                || mensajeNormalizado.contains("agenda")
                || mensajeNormalizado.contains("quiero una cita")
                || mensajeNormalizado.contains("quiero cita")
                || mensajeNormalizado.contains("reservar");
    }

    private boolean esIntencionReagendar(String mensajeNormalizado) {
        return mensajeNormalizado.contains("reagendar")
                || mensajeNormalizado.contains("reprogramar")
                || mensajeNormalizado.contains("cambiar mi cita")
                || mensajeNormalizado.contains("cambiar la cita")
                || mensajeNormalizado.contains("mover mi cita");
    }

    private boolean esIntencionServicios(String mensajeNormalizado) {
        return mensajeNormalizado.contains("servicios") || mensajeNormalizado.equals("servicio");
    }

    private boolean esIntencionHorarios(String mensajeNormalizado) {
        return mensajeNormalizado.contains("horarios")
                || mensajeNormalizado.contains("horario")
                || mensajeNormalizado.contains("disponibilidad")
                || mensajeNormalizado.contains("disponibles");
    }

    private boolean esIntencionUbicacion(String mensajeNormalizado) {
        return mensajeNormalizado.contains("ubicacion")
                || mensajeNormalizado.contains("direccion")
                || mensajeNormalizado.contains("donde estan")
                || mensajeNormalizado.contains("donde se encuentran");
    }

    private boolean esIntencionPromociones(String mensajeNormalizado) {
        return mensajeNormalizado.contains("promociones")
                || mensajeNormalizado.contains("promocion");
    }

    private boolean esIntencionPausarRecordatorios(String mensajeNormalizado) {
        return mensajeNormalizado.contains("pausar recordatorios")
                || mensajeNormalizado.contains("detener recordatorios")
                || mensajeNormalizado.contains("detener promociones");
    }

    private boolean esIntencionNoPorAhora(String mensajeNormalizado) {
        return mensajeNormalizado.contains("no por ahora")
                || mensajeNormalizado.contains("ahora no")
                || mensajeNormalizado.contains("ahora no gracias");
    }

    private boolean esIntencionConfirmarDetalle(String mensajeNormalizado) {
        return mensajeNormalizado.equals("si")
                || mensajeNormalizado.equals("confirmar")
                || mensajeNormalizado.equals("confirmar cita")
                || mensajeNormalizado.equals("ok")
                || mensajeNormalizado.equals("vale")
                || mensajeNormalizado.equals("listo");
    }

    private boolean esIntencionRechazarDetalle(String mensajeNormalizado) {
        return mensajeNormalizado.equals("no")
                || mensajeNormalizado.equals("aun no")
                || mensajeNormalizado.equals("todavia no")
                || mensajeNormalizado.equals("despues");
    }

    private boolean esMensajeMenu(String mensajeNormalizado) {
        return mensajeNormalizado.equals("ayuda")
                || mensajeNormalizado.equals("menu")
                || mensajeNormalizado.equals("inicio")
                || mensajeNormalizado.equals("hola")
                || mensajeNormalizado.equals("buenas")
                || mensajeNormalizado.equals("buenos dias")
                || mensajeNormalizado.equals("buenas tardes")
                || mensajeNormalizado.equals("buenas noches");
    }

    private String construirRespuestaListPicker(
            String fallback,
            String body,
            String button,
            List<OpcionListPicker> opciones
    ) {
        return construirRespuestaListPicker(fallback, body, button, opciones, null);
    }

    private String construirRespuestaListPicker(
            String fallback,
            String body,
            String button,
            List<OpcionListPicker> opciones,
            String uso
    ) {
        if (opciones == null || opciones.isEmpty()) {
            return fallback;
        }
        if (opciones.size() > 10) {
            return fallback;
        }

        String contentSid = resolverListPickerSid(uso, opciones.size());
        if (!tieneTexto(contentSid)) {
            return fallback;
        }

        Map<String, String> variables = construirVariablesListPicker(body, button, opciones, usaVariablesNumericasListPicker(uso));

        return serializarRespuestaContenido(fallback, contentSid, variables);
    }

    private Map<String, String> construirVariablesListPicker(
            String body,
            String button,
            List<OpcionListPicker> opciones,
            boolean variablesNumericas
    ) {
        Map<String, String> variables = new LinkedHashMap<>();
        if (variablesNumericas) {
            for (int i = 0; i < opciones.size(); i++) {
                OpcionListPicker opcion = opciones.get(i);
                int base = i * 3 + 1;
                variables.put(String.valueOf(base), limitar(opcion.titulo(), 24));
                variables.put(String.valueOf(base + 1), limitar(opcion.payload(), 200));
                variables.put(String.valueOf(base + 2), limitar(opcion.descripcion(), 72));
            }
            return variables;
        }

        variables.put("body", limitar(body, 1024));
        variables.put("button", limitar(button, 20));
        for (int i = 0; i < opciones.size(); i++) {
            OpcionListPicker opcion = opciones.get(i);
            int numero = i + 1;
            variables.put("item" + numero, limitar(opcion.titulo(), 24));
            variables.put("id" + numero, limitar(opcion.payload(), 200));
            variables.put("desc" + numero, limitar(opcion.descripcion(), 72));
        }
        return variables;
    }

    private boolean usaVariablesNumericasListPicker(String uso) {
        if (!tieneTexto(uso)) {
            return false;
        }
        String normalizado = uso.trim().toLowerCase(Locale.ROOT);
        return USO_SELECCIONAR_SUCURSAL.equals(normalizado)
                || USO_CATEGORIA_SERVICIO.equals(normalizado)
                || USO_SUBCATEGORIA_GENERICO.equals(normalizado)
                || USO_SUBCATEGORIA_GENERICA.equals(normalizado)
                || USO_SERVICIO_GENERICO.equals(normalizado);
    }

    private String resolverListPickerSid(String uso, int cantidadOpciones) {
        if (cantidadOpciones < 1 || cantidadOpciones > 10) {
            return null;
        }
        Long empresaId = obtenerEmpresaId();
        if (tieneTexto(uso)) {
            for (String usoEquivalente : usosEquivalentesListPicker(uso)) {
                String contentSidPorUso = servicioPlantillasWhatsappEmpresa.resolverContentSid(empresaId, usoEquivalente);
                if (tieneTexto(contentSidPorUso)) {
                    return contentSidPorUso;
                }
                contentSidPorUso = LIST_PICKER_SIDS_POR_USO.get(usoEquivalente.trim().toLowerCase(Locale.ROOT));
                if (tieneTexto(contentSidPorUso)) {
                    return contentSidPorUso;
                }
            }
        }

        String contentSidCatalogo = servicioPlantillasWhatsappEmpresa.resolverContentSid(empresaId, "LIST_PICKER_" + cantidadOpciones);
        if (tieneTexto(contentSidCatalogo)) {
            return contentSidCatalogo;
        }

        String configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId).plantillasListPickerSids();
        if (!tieneTexto(configuracion)) {
            return null;
        }

        for (String entrada : configuracion.split("[,;\\n]")) {
            String limpio = entrada.trim();
            if (limpio.isEmpty()) {
                continue;
            }
            String[] partes = limpio.split("[:=]", 2);
            if (partes.length != 2) {
                continue;
            }
            try {
                int cantidad = Integer.parseInt(partes[0].trim());
                if (cantidad == cantidadOpciones) {
                    return partes[1].trim();
                }
            } catch (NumberFormatException ignored) {
                // Ignoramos entradas mal formateadas para conservar fallback a texto.
            }
        }
        return null;
    }

    private List<String> usosEquivalentesListPicker(String uso) {
        String normalizado = uso.trim().toLowerCase(Locale.ROOT);
        if (USO_SUBCATEGORIA_GENERICO.equals(normalizado) || USO_SUBCATEGORIA_GENERICA.equals(normalizado)) {
            return List.of(USO_SUBCATEGORIA_GENERICO, USO_SUBCATEGORIA_GENERICA);
        }
        return List.of(normalizado);
    }

    private String serializarRespuestaContenido(String fallback, String contentSid, Map<String, String> variables) {
        try {
            RespuestaContenidoSerializada contenido = new RespuestaContenidoSerializada(fallback, contentSid, variables);
            String json = objectMapper.writeValueAsString(contenido);
            return PREFIJO_RESPUESTA_CONTENIDO + Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException ex) {
            LOGGER.warn("No se pudo serializar respuesta interactiva de WhatsApp, se usara texto plano", ex);
            return fallback;
        }
    }

    private RespuestaWhatsapp deserializarRespuestaContenido(String respuesta) {
        if (respuesta == null || !respuesta.startsWith(PREFIJO_RESPUESTA_CONTENIDO)) {
            return null;
        }
        try {
            String encoded = respuesta.substring(PREFIJO_RESPUESTA_CONTENIDO.length());
            String json = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            RespuestaContenidoSerializada contenido = objectMapper.readValue(json, RespuestaContenidoSerializada.class);
            return RespuestaWhatsapp.conContenido(contenido.fallback(), contenido.contentSid(), contenido.variables());
        } catch (Exception ex) {
            LOGGER.warn("No se pudo leer respuesta interactiva de WhatsApp, se usara texto plano", ex);
            return RespuestaWhatsapp.texto("No pude preparar las opciones interactivas. Escribe MENU para intentarlo de nuevo.");
        }
    }

    private Long extraerIdPayload(String texto, String tipo) {
        String payload = extraerPayload(texto, tipo);
        if (!tieneTexto(payload)) {
            return null;
        }
        try {
            return Long.parseLong(payload);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String extraerPayload(String texto, String tipo) {
        if (!tieneTexto(texto) || !tieneTexto(tipo)) {
            return null;
        }
        String prefijo = tipo + "|";
        String limpio = texto.trim();
        if (!limpio.toUpperCase(Locale.ROOT).startsWith(prefijo)) {
            return null;
        }
        return limpio.substring(prefijo.length()).trim();
    }

    private String limitar(String valor, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.length() <= maximo) {
            return limpio;
        }
        return limpio.substring(0, Math.max(0, maximo)).trim();
    }

    private String normalizarTextoLibre(String valor) {
        String base = valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
        String sinAcentos = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinAcentos.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    public record RespuestaWhatsapp(
            String mensaje,
            ContenidoInteractivo contenidoInteractivo
    ) {
        public static RespuestaWhatsapp texto(String mensaje) {
            return new RespuestaWhatsapp(mensaje, null);
        }

        public static RespuestaWhatsapp conContenido(String mensaje, String contentSid, Map<String, String> variables) {
            return new RespuestaWhatsapp(mensaje, new ContenidoInteractivo(contentSid, variables == null ? Map.of() : Map.copyOf(variables)));
        }

        public boolean tieneContenidoInteractivo() {
            return contenidoInteractivo != null
                    && contenidoInteractivo.contentSid() != null
                    && !contenidoInteractivo.contentSid().isBlank();
        }
    }

    public record ContenidoInteractivo(
            String contentSid,
            Map<String, String> variables
    ) {
    }

    private record OpcionListPicker(
            String payload,
            String titulo,
            String descripcion
    ) {
    }

    public record RespuestaContenidoSerializada(
            String fallback,
            String contentSid,
            Map<String, String> variables
    ) {
    }

    private String listarMisCitas(String telefonoRemitente) {
        ClienteEntidad cliente = obtenerClientePorTelefono(telefonoRemitente);
        List<CitaClienteResponse> citas = obtenerCitasActivasCliente(cliente);

        if (citas.isEmpty()) {
            return "No encontramos citas próximas para este número. Si quieres, puedo ayudarte a agendar una nueva.";
        }

        StringBuilder respuesta = new StringBuilder(citas.size() == 1 ? "Tu próxima cita:\n" : "Tus próximas citas:\n");
        for (int i = 0; i < citas.size(); i++) {
            respuesta.append(i + 1)
                    .append(". ")
                    .append(formatearCitaVisible(citas.get(i)))
                    .append("\n");
        }
        if (citas.size() == 1) {
            respuesta.append("\nPuedes responder:");
            if ("PENDIENTE".equalsIgnoreCase(citas.getFirst().estado())) {
                respuesta.append("\n- CONFIRMAR");
            }
            respuesta.append("\n- CANCELAR");
            respuesta.append("\n- REAGENDAR");
        } else {
            respuesta.append("\nPuedes responder, por ejemplo:");
            respuesta.append("\n- CONFIRMAR 1");
            respuesta.append("\n- CANCELAR 1");
            respuesta.append("\n- REAGENDAR 1");
        }
        return respuesta.toString().trim();
    }

    private String responderDetalleCita(String telefonoRemitente) {
        ClienteEntidad cliente = obtenerClientePorTelefono(telefonoRemitente);
        List<CitaClienteResponse> citas = obtenerCitasActivasCliente(cliente);

        if (citas.isEmpty()) {
            return "No encontramos una cita próxima para mostrarte. Si quieres, puedo ayudarte a agendar una nueva.";
        }

        ReservaDetalleWhatsapp detalle = construirDetalleReserva(citas);
        ConversacionWhatsappEntidad conversacion = obtenerOCrearConversacion(telefonoRemitente);
        conversacion.setFlujo("DETALLE_CITA");
        conversacion.setPaso("DETALLE_CONFIRMACION");
        conversacion.setCitaId(detalle.citas().getFirst().id());
        conversacion.setSucursalId(detalle.citas().getFirst().sucursalId());
        conversacion.setServicioId(null);
        conversacion.setFechaSeleccionada(detalle.citas().getFirst().inicio().toLocalDate());
        conversacion.setHoraSeleccionada(formatearHoraDetalle(detalle.citas().getFirst().inicio()));
        guardarConversacion(conversacion);
        StringBuilder respuesta = new StringBuilder("""
                📋 Detalle de tu cita:

                """);

        for (CitaClienteResponse cita : detalle.citas()) {
            respuesta.append("• ")
                    .append(cita.servicioNombre())
                    .append(" — ")
                    .append(formatearHoraDetalle(cita.inicio()))
                    .append("\n");
        }

        respuesta.append("\nSucursal: ")
                .append(nombreVisibleSucursal(detalle.sucursalNombre()))
                .append("\nDuración estimada total: ")
                .append(formatearDuracion(detalle.duracionTotal()))
                .append("\n\n¿Deseas confirmar tu cita?");

        respuesta.append("\nResponde CONFIRMAR CITA para confirmarla o MIS CITAS para revisar tus opciones.");
        return respuesta.toString().trim();
    }

    private String listarSucursales() {
        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdAndActivaTrue(obtenerEmpresaId()).stream()
                .sorted(Comparator.comparing(SucursalEntidad::getNombre))
                .toList();

        if (sucursales.isEmpty()) {
            return "No hay sucursales activas disponibles.";
        }

        StringBuilder respuesta = new StringBuilder("Sucursales disponibles:\n");
        for (int i = 0; i < sucursales.size(); i++) {
            SucursalEntidad sucursal = sucursales.get(i);
            respuesta.append(i + 1)
                    .append(". ")
                    .append(nombreVisibleSucursal(sucursal.getNombre()));
            if (sucursal.getDireccion() != null && !sucursal.getDireccion().isBlank()) {
                respuesta.append(" · ").append(sucursal.getDireccion());
            }
            respuesta.append("\n");
        }
        respuesta.append("\nSi quieres agendar, responde: Quiero agendar");
        return respuesta.toString().trim();
    }

    private String listarServicios(String mensaje) {
        String[] partes = mensaje.trim().split("\\s+");
        if (partes.length == 1) {
            List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdAndActivaTrue(obtenerEmpresaId());
            if (sucursales.size() == 1) {
                return construirListaServicios(sucursales.getFirst().getId());
            }
            return "Indica la sucursal. Ejemplo: SERVICIOS 1\nPrimero puedes consultar SUCURSALES.";
        }

        Long sucursalId = parsearLong(partes[1], "El sucursalId no es valido");
        return construirListaServicios(sucursalId);
    }

    private String construirListaServicios(Long sucursalId) {
        List<ServicioEntidad> servicios = servicioRepositorio.findBySucursalIdAndActivoTrue(sucursalId).stream()
                .sorted(Comparator.comparing(ServicioEntidad::getNombre))
                .toList();

        if (servicios.isEmpty()) {
            return "No encontramos servicios activos para esa sucursal.";
        }

        StringBuilder respuesta = new StringBuilder("Servicios disponibles en ")
                .append(nombreVisibleSucursal(sucursalRepositorio.findById(sucursalId).map(SucursalEntidad::getNombre).orElse("la sucursal")))
                .append(":\n");
        for (int i = 0; i < servicios.size(); i++) {
            ServicioEntidad servicio = servicios.get(i);
            respuesta.append(i + 1)
                    .append(". ")
                    .append(servicio.getNombre())
                    .append(" · ")
                    .append(servicio.getPrecio())
                    .append(" ")
                    .append(servicio.getMoneda())
                    .append(" · ")
                    .append(servicio.getDuracionMinutos())
                    .append(" min\n");
        }
        respuesta.append("\nSi quieres agendar, responde: Quiero agendar");
        return respuesta.toString().trim();
    }

    private String listarHorarios(String mensaje) {
        String[] partes = mensaje.trim().split("\\s+");
        if (partes.length < 4) {
            throw new IllegalArgumentException("Usa: HORARIOS <sucursalId> <servicioId> <AAAA-MM-DD>");
        }

        Long sucursalId = parsearLong(partes[1], "El sucursalId no es valido");
        Long servicioId = parsearLong(partes[2], "El servicioId no es valido");
        LocalDate fecha = parsearFecha(partes[3]);

        List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                obtenerEmpresaId(),
                sucursalId,
                servicioId,
                null,
                fecha
        );

        if (franjas.isEmpty()) {
            return "No hay horarios disponibles para esa fecha.";
        }

        SucursalEntidad sucursal = sucursalRepositorio.findById(sucursalId)
                .orElseThrow(() -> new IllegalArgumentException("La sucursal indicada no existe"));
        ZoneId zona = ZoneId.of(sucursal.getZonaHoraria());

        StringBuilder respuesta = new StringBuilder("Horarios disponibles:\n");
        franjas.stream().limit(8).forEach(franja -> {
            LocalDateTime fechaHora = franja.inicio().contains("T")
                    ? java.time.OffsetDateTime.parse(franja.inicio()).atZoneSameInstant(zona).toLocalDateTime()
                    : LocalDateTime.parse(franja.inicio(), FORMATO_FECHA_HORA);
            respuesta.append("- ").append(fechaHora.format(FORMATO_FECHA_HORA)).append("\n");
        });
        respuesta.append("\nPara agendar usa:\nAGENDAR|")
                .append(sucursalId)
                .append("|")
                .append(servicioId)
                .append("|AAAA-MM-DD HH:mm|Nombre completo|correo@dominio.com|Notas opcionales");
        return respuesta.toString().trim();
    }

    private String confirmarCita(String telefonoRemitente, String mensaje) {
        ClienteEntidad cliente = obtenerClientePorTelefono(telefonoRemitente);
        Long empresaId = obtenerEmpresaId();
        String opcion = extraerOpcionSimple(mensaje);

        if (opcion != null) {
            Long citaId = resolverCitaPorOpcion(cliente, opcion, true);
            CitaClienteResponse cita = servicioCitasCliente.confirmar(empresaId, cliente.getUsuarioId(), citaId, false);
            return construirRespuestaConfirmacion(cliente, List.of(cita));
        }

        List<CitaClienteResponse> pendientes = servicioCitasCliente.listarMisCitas(empresaId, cliente.getUsuarioId()).stream()
                .filter(cita -> "PENDIENTE".equalsIgnoreCase(cita.estado()))
                .filter(cita -> cita.inicio().toLocalDateTime().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(CitaClienteResponse::inicio))
                .toList();

        if (pendientes.isEmpty()) {
            throw new IllegalArgumentException("No encontramos una cita pendiente para confirmar. Responde MIS CITAS para revisar tus opciones.");
        }

        ReservaDetalleWhatsapp detalle = construirDetalleReserva(pendientes);
        List<CitaClienteResponse> confirmadas = detalle.citas().stream()
                .map(cita -> servicioCitasCliente.confirmar(empresaId, cliente.getUsuarioId(), cita.id(), false))
                .toList();
        return construirRespuestaConfirmacion(cliente, confirmadas);
    }

    private String cancelarCita(String telefonoRemitente, String mensaje) {
        ClienteEntidad cliente = obtenerClientePorTelefono(telefonoRemitente);
        Long citaId = extraerIdCancelacion(cliente, mensaje);
        CitaClienteResponse cita = obtenerCitaActivaPorId(cliente, citaId);
        servicioCitasCliente.cancelar(obtenerEmpresaId(), cliente.getUsuarioId(), citaId);
        return "Tu cita de %s para el %s fue cancelada correctamente.".formatted(
                cita.servicioNombre(),
                formatearFechaHoraCliente(cita.inicio())
        );
    }

    private String reprogramarCita(String telefonoRemitente, String mensaje) {
        ClienteEntidad cliente = obtenerClientePorTelefono(telefonoRemitente);
        String[] partes = mensaje.split("\\|", -1);
        if (partes.length < 3) {
            throw new IllegalArgumentException("Usa: REAGENDAR|1|AAAA-MM-DD HH:mm");
        }

        Long citaId = resolverCitaPorOpcion(cliente, partes[1], false);
        CitaClienteResponse citaActual = obtenerCitaActivaPorId(cliente, citaId);

        ZoneId zona = ZoneId.of(resolverZonaHoraria(citaActual.sucursalId()));
        LocalDateTime nuevaFecha = parsearFechaHora(partes[2]);
        CitaClienteResponse citaReprogramada = servicioCitasCliente.reprogramar(
                obtenerEmpresaId(),
                cliente.getUsuarioId(),
                citaId,
                nuevaFecha.atZone(zona).toOffsetDateTime()
        );

        return "Tu cita de %s fue reprogramada para el %s y quedó pendiente de confirmación.".formatted(
                citaReprogramada.servicioNombre(),
                formatearFechaHoraCliente(citaReprogramada.inicio())
        );
    }

    private String agendarCita(String telefonoRemitente, String mensaje) {
        String[] partes = mensaje.split("\\|", -1);
        if (partes.length < 6) {
            throw new IllegalArgumentException("""
                    Usa:
                    AGENDAR|<sucursalId>|<servicioId>|AAAA-MM-DD HH:mm|Nombre completo|correo@dominio.com|Notas opcionales
                    """.trim());
        }

        Long sucursalId = parsearLong(partes[1], "El sucursalId no es valido");
        Long servicioId = parsearLong(partes[2], "El servicioId no es valido");
        LocalDateTime fechaHora = parsearFechaHora(partes[3]);
        String nombre = partes[4].trim();
        String correo = partes[5].trim();
        String notas = partes.length >= 7 ? partes[6].trim() : null;

        ZoneId zona = ZoneId.of(resolverZonaHoraria(sucursalId));
        CitaCreadaResponse cita = servicioCitas.crearCita(new CrearCitaRequest(
                obtenerEmpresaId(),
                sucursalId,
                servicioId,
                null,
                nombre,
                correo,
                NormalizadorTelefonoWhatsapp.normalizarComparable(telefonoRemitente),
                fechaHora.atZone(zona).toOffsetDateTime(),
                notas == null || notas.isBlank() ? null : notas
        ));

        return "Tu cita fue creada con folio %d para el %s. Si deseas ver mas detalles responde MIS CITAS.".formatted(
                cita.id(),
                cita.inicio().format(FORMATO_RESPUESTA)
        );
    }

    private ClienteEntidad obtenerClientePorTelefono(String telefonoRemitente) {
        Long empresaId = obtenerEmpresaId();
        return clienteRepositorio.findByAceptaWhatsappTrue().stream()
                .filter(cliente -> NormalizadorTelefonoWhatsapp.coincide(cliente.getTelefono(), telefonoRemitente))
                .filter(cliente -> usuarioRepositorio.findByIdAndEmpresaId(cliente.getUsuarioId(), empresaId).isPresent())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No encontramos un cliente asociado a este número. Si gustas, responde: Quiero agendar, y te ayudo paso a paso."
                ));
    }

    private ClienteEntidad obtenerClientePorTelefonoOpcional(String telefonoRemitente) {
        Long empresaId = obtenerEmpresaId();
        return clienteRepositorio.findByAceptaWhatsappTrue().stream()
                .filter(cliente -> NormalizadorTelefonoWhatsapp.coincide(cliente.getTelefono(), telefonoRemitente))
                .filter(cliente -> usuarioRepositorio.findByIdAndEmpresaId(cliente.getUsuarioId(), empresaId).isPresent())
                .findFirst()
                .orElse(null);
    }

    private String ayuda() {
        return """
                Comandos disponibles:
                SUCURSALES
                SERVICIOS <sucursalId>
                HORARIOS <sucursalId> <servicioId> <AAAA-MM-DD>
                AGENDAR|<sucursalId>|<servicioId>|AAAA-MM-DD HH:mm|Nombre completo|correo@dominio.com|Notas opcionales
                MIS CITAS
                CONFIRMAR <folio>
                CANCELAR <folio>
                REAGENDAR|<folio>|AAAA-MM-DD HH:mm
                Si solo tienes una cita pendiente, tambien puedes responder solo CONFIRMAR
                Si quieres cambiar una cita de forma guiada, responde REAGENDAR
                """.trim();
    }

    private Long obtenerEmpresaId() {
        return contextoEmpresaWhatsapp.requerirEmpresaId();
    }

    private Long extraerIdSimple(String mensaje, String ayuda) {
        String[] partes = mensaje.trim().split("\\s+");
        if (partes.length < 2) {
            throw new IllegalArgumentException(ayuda);
        }
        return parsearLong(partes[1], ayuda);
    }

    private String extraerOpcionSimple(String mensaje) {
        String[] partes = mensaje.trim().split("\\s+", 2);
        if (partes.length < 2) {
            return null;
        }
        String valor = partes[1].trim();
        return valor.isBlank() ? null : valor;
    }

    private Long extraerIdConfirmacion(ClienteEntidad cliente, String mensaje) {
        String[] partes = mensaje.trim().split("\\s+");
        if (partes.length >= 2) {
            return resolverCitaPorOpcion(cliente, partes[1], true);
        }

        List<CitaClienteResponse> pendientes = servicioCitasCliente.listarMisCitas(obtenerEmpresaId(), cliente.getUsuarioId()).stream()
                .filter(cita -> "PENDIENTE".equalsIgnoreCase(cita.estado()))
                .filter(cita -> cita.inicio().toLocalDateTime().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(CitaClienteResponse::inicio))
                .limit(2)
                .toList();

        if (pendientes.size() == 1) {
            return pendientes.getFirst().id();
        }

        if (pendientes.isEmpty()) {
            throw new IllegalArgumentException("No encontramos una cita pendiente para confirmar. Responde MIS CITAS para revisar tus opciones.");
        }

        throw new IllegalArgumentException("Tienes varias citas pendientes. Usa por ejemplo CONFIRMAR 1 o responde MIS CITAS para ver tus opciones.");
    }

    private Long extraerIdCancelacion(ClienteEntidad cliente, String mensaje) {
        String[] partes = mensaje.trim().split("\\s+");
        if (partes.length >= 2) {
            return resolverCitaPorOpcion(cliente, partes[1], false);
        }

        List<CitaClienteResponse> activas = obtenerCitasCancelablesCliente(cliente).stream()
                .limit(2)
                .toList();
        if (activas.size() == 1) {
            return activas.getFirst().id();
        }
        if (activas.isEmpty()) {
            throw new IllegalArgumentException("No encontré una cita activa para cancelar. Responde MIS CITAS para revisar tus opciones.");
        }
        throw new IllegalArgumentException("Tienes varias citas activas. Usa por ejemplo CANCELAR 1 o responde MIS CITAS para ver tus opciones.");
    }

    private Long parsearLong(String valor, String mensajeError) {
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(mensajeError);
        }
    }

    private LocalDate parsearFecha(String valor) {
        try {
            return LocalDate.parse(valor.trim(), FORMATO_FECHA);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha debe ir en formato AAAA-MM-DD");
        }
    }

    private LocalDateTime parsearFechaHora(String valor) {
        try {
            return LocalDateTime.parse(valor.trim(), FORMATO_FECHA_HORA);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha y hora deben ir en formato AAAA-MM-DD HH:mm");
        }
    }

    private String resolverZonaHoraria(Long sucursalId) {
        return sucursalRepositorio.findById(sucursalId)
                .map(SucursalEntidad::getZonaHoraria)
                .orElseThrow(() -> new IllegalArgumentException("La sucursal indicada no existe"));
    }

    private List<CitaClienteResponse> obtenerCitasActivasCliente(ClienteEntidad cliente) {
        return servicioCitasCliente.listarMisCitas(obtenerEmpresaId(), cliente.getUsuarioId()).stream()
                .filter(cita -> cita.inicio().toLocalDateTime().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(CitaClienteResponse::inicio))
                .limit(5)
                .toList();
    }

    private List<CitaClienteResponse> obtenerCitasCancelablesCliente(ClienteEntidad cliente) {
        return obtenerCitasActivasCliente(cliente).stream()
                .filter(cita -> List.of("PENDIENTE", "CONFIRMADA").contains(cita.estado()))
                .toList();
    }

    private Long resolverCitaPorOpcion(ClienteEntidad cliente, String valor, boolean soloPendientes) {
        List<CitaClienteResponse> citas = (soloPendientes ? obtenerCitasActivasCliente(cliente).stream()
                .filter(cita -> "PENDIENTE".equalsIgnoreCase(cita.estado()))
                .toList() : obtenerCitasCancelablesCliente(cliente));

        Integer opcion = parsearOpcion(valor);
        if (opcion != null && opcion >= 1 && opcion <= citas.size()) {
            return citas.get(opcion - 1).id();
        }

        Long citaId = parsearLong(valor, "La opción indicada no es válida");
        return citas.stream()
                .filter(cita -> cita.id().equals(citaId))
                .findFirst()
                .map(CitaClienteResponse::id)
                .orElseThrow(() -> new IllegalArgumentException("No encontré esa opción para este número. Responde MIS CITAS para revisar tus opciones."));
    }

    private CitaClienteResponse obtenerCitaActivaPorId(ClienteEntidad cliente, Long citaId) {
        return obtenerCitasActivasCliente(cliente).stream()
                .filter(cita -> cita.id().equals(citaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No encontré la cita indicada para este número."));
    }

    private ReservaDetalleWhatsapp construirDetalleReserva(List<CitaClienteResponse> citas) {
        CitaClienteResponse primera = citas.getFirst();
        LocalDate fechaBase = primera.inicio().toLocalDate();
        Long sucursalBase = primera.sucursalId();

        List<CitaClienteResponse> agrupadas = citas.stream()
                .filter(cita -> Objects.equals(cita.sucursalId(), sucursalBase))
                .filter(cita -> cita.inicio().toLocalDate().equals(fechaBase))
                .toList();

        Duration duracionTotal = agrupadas.stream()
                .map(cita -> Duration.between(cita.inicio().toLocalDateTime(), cita.fin().toLocalDateTime()))
                .reduce(Duration.ZERO, Duration::plus);

        boolean todasPendientes = agrupadas.stream()
                .allMatch(cita -> "PENDIENTE".equalsIgnoreCase(cita.estado()));

        return new ReservaDetalleWhatsapp(
                agrupadas,
                primera.sucursalNombre(),
                duracionTotal,
                todasPendientes
        );
    }

    private String construirRespuestaConfirmacion(ClienteEntidad cliente, List<CitaClienteResponse> citasConfirmadas) {
        ReservaDetalleWhatsapp detalle = construirDetalleReserva(citasConfirmadas);
        CitaClienteResponse primera = detalle.citas().getFirst();
        SucursalEntidad sucursal = sucursalRepositorio.findById(primera.sucursalId()).orElse(null);
        EmpresaEntidad empresa = empresaRepositorio.findById(obtenerEmpresaId()).orElse(null);
        String nombreNegocio = empresa != null ? empresa.getNombre() : "tu negocio";
        String fecha = primera.inicio().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String hora = formatearHoraDetalle(primera.inicio());
        String direccion = sucursal != null && tieneTexto(sucursal.getDireccion())
                ? sucursal.getDireccion()
                : nombreVisibleSucursal(detalle.sucursalNombre());

        return """
                ¡Todo listo, %s!

                Tu cita en %s quedó confirmada para el %s a las %s.

                Te recomendamos llegar 10 minutos antes para brindarte un mejor servicio.

                Te esperamos en %s.
                Si más adelante necesitas reprogramarla o cancelarla, responde MIS CITAS.
                """.formatted(
                cliente.getNombreCompleto(),
                nombreNegocio,
                fecha,
                hora,
                direccion
        ).trim();
    }

    private String procesarConfirmacionDetalle(ConversacionWhatsappEntidad conversacion, String telefonoRemitente, String mensaje) {
        String mensajeNormalizado = normalizarTextoLibre(mensaje);
        if (esIntencionConfirmarDetalle(mensajeNormalizado)) {
            limpiarConversacion(conversacion);
            return confirmarCitaWhatsapp(telefonoRemitente, "CONFIRMAR");
        }
        if (esIntencionRechazarDetalle(mensajeNormalizado)) {
            limpiarConversacion(conversacion);
            return "Entendido. Cuando quieras retomarlo, responde CONFIRMAR CITA o MIS CITAS.";
        }
        if ("ver detalle".equals(mensajeNormalizado) || "ver detalles".equals(mensajeNormalizado)) {
            return responderDetalleCita(telefonoRemitente);
        }
        return "Si deseas confirmarla, responde CONFIRMAR CITA. Si prefieres revisar tus opciones, responde MIS CITAS.";
    }

    private String confirmarCitaWhatsapp(String telefonoRemitente, String mensaje) {
        ClienteEntidad cliente = obtenerClientePorTelefono(telefonoRemitente);
        Long empresaId = obtenerEmpresaId();
        String opcion = extraerOpcionSimple(mensaje);

        if (opcion != null) {
            Long citaId = resolverCitaPorOpcion(cliente, opcion, true);
            servicioCitasCliente.confirmar(empresaId, cliente.getUsuarioId(), citaId, true);
            return "Listo. Estoy enviando la confirmación de tu cita.";
        }

        List<CitaClienteResponse> pendientes = servicioCitasCliente.listarMisCitas(empresaId, cliente.getUsuarioId()).stream()
                .filter(cita -> "PENDIENTE".equalsIgnoreCase(cita.estado()))
                .filter(cita -> cita.inicio().toLocalDateTime().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(CitaClienteResponse::inicio))
                .toList();

        if (pendientes.isEmpty()) {
            throw new IllegalArgumentException("No encontramos una cita pendiente para confirmar. Responde MIS CITAS para revisar tus opciones.");
        }

        ReservaDetalleWhatsapp detalle = construirDetalleReserva(pendientes);
        detalle.citas().forEach(cita -> servicioCitasCliente.confirmar(empresaId, cliente.getUsuarioId(), cita.id(), true));
        return "Listo. Estoy enviando la confirmación de tu cita.";
    }

    private Integer parsearOpcion(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String formatearCitaVisible(CitaClienteResponse cita) {
        return "%s · %s · %s · %s".formatted(
                cita.servicioNombre(),
                formatearFechaHoraCliente(cita.inicio()),
                nombreVisibleSucursal(cita.sucursalNombre()),
                estadoVisible(cita.estado())
        );
    }

    private String formatearFechaHoraCliente(java.time.OffsetDateTime fechaHora) {
        LocalDateTime local = fechaHora.toLocalDateTime();
        String dia = local.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "MX"));
        return "%s %02d/%02d a las %s".formatted(
                capitalizar(dia),
                local.getDayOfMonth(),
                local.getMonthValue(),
                local.toLocalTime().format(FORMATO_HORA)
        );
    }

    private String formatearHoraDetalle(java.time.OffsetDateTime fechaHora) {
        return fechaHora.toLocalDateTime().toLocalTime().format(FORMATO_HORA_DETALLE);
    }

    private String formatearDuracion(Duration duracion) {
        long minutos = Math.max(duracion.toMinutes(), 0);
        long horas = minutos / 60;
        long restantes = minutos % 60;
        if (horas == 0) {
            return "%d min".formatted(restantes);
        }
        if (restantes == 0) {
            return "%d h".formatted(horas);
        }
        return "%d h %02d min".formatted(horas, restantes);
    }

    private String estadoVisible(String estado) {
        return switch (estado == null ? "" : estado.toUpperCase(Locale.ROOT)) {
            case "PENDIENTE" -> "Pendiente de confirmación";
            case "CONFIRMADA" -> "Confirmada";
            case "CANCELADA" -> "Cancelada";
            default -> capitalizar((estado == null ? "" : estado).toLowerCase(Locale.ROOT));
        };
    }

    private String obtenerNombreServicio(Long servicioId) {
        return servicioRepositorio.findById(servicioId)
                .map(ServicioEntidad::getNombre)
                .orElse("tu servicio");
    }

    private String nombreVisibleSucursal(String nombre) {
        if (!tieneTexto(nombre)) {
            return "Sucursal";
        }
        String visible = nombre.trim();
        return visible.length() > 28 ? visible.substring(0, 28).trim() + "..." : visible;
    }

    private String capitalizar(String valor) {
        if (!tieneTexto(valor)) {
            return "";
        }
        return valor.substring(0, 1).toUpperCase(Locale.ROOT) + valor.substring(1);
    }

    private record ReservaDetalleWhatsapp(
            List<CitaClienteResponse> citas,
            String sucursalNombre,
            Duration duracionTotal,
            boolean todasPendientes
    ) {
    }
}
