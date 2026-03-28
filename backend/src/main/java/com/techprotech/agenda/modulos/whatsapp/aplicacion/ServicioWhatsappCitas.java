package com.techprotech.agenda.modulos.whatsapp.aplicacion;

import com.techprotech.agenda.compartido.whatsapp.NormalizadorTelefonoWhatsapp;
import com.techprotech.agenda.compartido.whatsapp.PropiedadesWhatsapp;
import com.techprotech.agenda.compartido.whatsapp.ServicioConfiguracionWhatsappEmpresa;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaRequest;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitas;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitasCliente;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.FranjaDisponibleResponse;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConsultaDisponibilidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.whatsapp.infraestructura.entidad.ConversacionWhatsappEntidad;
import com.techprotech.agenda.modulos.whatsapp.infraestructura.repositorio.ConversacionWhatsappRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class ServicioWhatsappCitas {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter FORMATO_RESPUESTA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "MX"));
    private static final DateTimeFormatter FORMATO_FECHA_AMIGABLE = DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM", new Locale("es", "MX"));
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern SOLO_HORA = Pattern.compile("^\\d{2}:\\d{2}$");
    private static final long MINUTOS_EXPIRACION_CONVERSACION = 60L;

    private final PropiedadesWhatsapp propiedadesWhatsapp;
    private final ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa;
    private final ServicioCitas servicioCitas;
    private final ServicioCitasCliente servicioCitasCliente;
    private final ServicioConsultaDisponibilidad servicioConsultaDisponibilidad;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final ConversacionWhatsappRepositorio conversacionWhatsappRepositorio;

    public ServicioWhatsappCitas(
            PropiedadesWhatsapp propiedadesWhatsapp,
            ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa,
            ServicioCitas servicioCitas,
            ServicioCitasCliente servicioCitasCliente,
            ServicioConsultaDisponibilidad servicioConsultaDisponibilidad,
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            ConversacionWhatsappRepositorio conversacionWhatsappRepositorio
    ) {
        this.propiedadesWhatsapp = propiedadesWhatsapp;
        this.servicioConfiguracionWhatsappEmpresa = servicioConfiguracionWhatsappEmpresa;
        this.servicioCitas = servicioCitas;
        this.servicioCitasCliente = servicioCitasCliente;
        this.servicioConsultaDisponibilidad = servicioConsultaDisponibilidad;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.conversacionWhatsappRepositorio = conversacionWhatsappRepositorio;
    }

    public String procesarMensaje(String telefonoRemitente, String mensajeOriginal) {
        if (!servicioConfiguracionWhatsappEmpresa.resolver(obtenerEmpresaId()).habilitado()) {
            return "El canal de WhatsApp no esta habilitado en este momento.";
        }

        String mensaje = mensajeOriginal == null ? "" : mensajeOriginal.trim();
        if (mensaje.isBlank()) {
            return bienvenida();
        }

        String mensajeMayus = mensaje.toUpperCase(Locale.ROOT);
        String mensajeNormalizado = normalizarTextoLibre(mensaje);
        ConversacionWhatsappEntidad conversacion = obtenerConversacion(telefonoRemitente);
        try {
            if (esMensajeMenu(mensajeNormalizado)) {
                limpiarConversacion(telefonoRemitente);
                return bienvenida();
            }
            if ("SUCURSALES".equals(mensajeMayus)) {
                return listarSucursales();
            }
            if ("MIS CITAS".equals(mensajeMayus)) {
                return listarMisCitas(telefonoRemitente);
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
            return bienvenida();
        } catch (ResponseStatusException ex) {
            return ex.getReason() != null ? ex.getReason() : "No pudimos procesar tu solicitud.";
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        }
    }

    private String bienvenida() {
        return """
                Hola, gracias por escribir a NailArt Studio.

                Puedo ayudarte con:
                - Agendar una cita
                - Ver servicios
                - Ver horarios disponibles
                - Ver ubicación
                - Revisar mis citas
                - Ver promociones

                Solo dime qué necesitas y te guío paso a paso.
                """.trim();
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
            conversacion.setPaso("AGENDAR_SERVICIO");
            guardarConversacion(conversacion);
            return construirPreguntaServicios(sucursales.getFirst().getId(), "Claro, te ayudo a agendar.");
        }

        conversacion.setPaso("AGENDAR_SUCURSAL");
        conversacion.setCitaId(null);
        conversacion.setSucursalId(null);
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
            conversacion.setPaso("HORARIOS_SERVICIO");
            guardarConversacion(conversacion);
            return construirPreguntaServicios(sucursales.getFirst().getId(), "Perfecto, revisemos horarios.");
        }

        conversacion.setPaso("HORARIOS_SUCURSAL");
        conversacion.setCitaId(null);
        conversacion.setSucursalId(null);
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
        conversacion.setServicioId(null);
        conversacion.setFechaSeleccionada(null);
        conversacion.setHoraSeleccionada(null);
        guardarConversacion(conversacion);
        return construirPreguntaReagendarCita(citas);
    }

    private String continuarConversacion(ConversacionWhatsappEntidad conversacion, String telefonoRemitente, String mensaje) {
        return switch (conversacion.getPaso()) {
            case "AGENDAR_SUCURSAL", "HORARIOS_SUCURSAL" -> procesarSucursalConversacion(conversacion, mensaje);
            case "AGENDAR_SERVICIO", "HORARIOS_SERVICIO" -> procesarServicioConversacion(conversacion, mensaje);
            case "AGENDAR_FECHA", "HORARIOS_FECHA" -> procesarFechaConversacion(conversacion, mensaje);
            case "AGENDAR_HORA" -> procesarHoraAgendar(conversacion, telefonoRemitente, mensaje);
            case "AGENDAR_CONFIRMAR_DATOS" -> procesarConfirmacionDatosAgendar(conversacion, telefonoRemitente, mensaje);
            case "AGENDAR_NOMBRE" -> procesarNombreAgendar(conversacion, mensaje);
            case "AGENDAR_CORREO" -> procesarCorreoAgendar(conversacion, telefonoRemitente, mensaje);
            case "REAGENDAR_CITA" -> procesarSeleccionReagendar(conversacion, telefonoRemitente, mensaje);
            case "REAGENDAR_FECHA" -> procesarFechaReagendar(conversacion, telefonoRemitente, mensaje);
            case "REAGENDAR_HORA" -> procesarHoraReagendar(conversacion, telefonoRemitente, mensaje);
            default -> {
                limpiarConversacion(telefonoRemitente);
                yield bienvenida();
            }
        };
    }

    private String procesarSucursalConversacion(ConversacionWhatsappEntidad conversacion, String mensaje) {
        SucursalEntidad sucursal = resolverSucursalPorTexto(mensaje);
        if (sucursal == null) {
            return construirPreguntaSucursales("No identifiqué la sucursal. Respóndeme con el número o con parte del nombre.");
        }

        conversacion.setSucursalId(sucursal.getId());
        conversacion.setServicioId(null);
        conversacion.setFechaSeleccionada(null);
        conversacion.setHoraSeleccionada(null);
        conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_SERVICIO" : "AGENDAR_SERVICIO");
        guardarConversacion(conversacion);
        return construirPreguntaServicios(sucursal.getId(), "Perfecto.");
    }

    private String procesarServicioConversacion(ConversacionWhatsappEntidad conversacion, String mensaje) {
        if (conversacion.getSucursalId() == null) {
            conversacion.setPaso(conversacion.getFlujo().equals("HORARIOS") ? "HORARIOS_SUCURSAL" : "AGENDAR_SUCURSAL");
            guardarConversacion(conversacion);
            return construirPreguntaSucursales("Necesito primero la sucursal.");
        }

        ServicioEntidad servicio = resolverServicioPorTexto(conversacion.getSucursalId(), mensaje);
        if (servicio == null) {
            return construirPreguntaServicios(conversacion.getSucursalId(), "No identifiqué el servicio. Respóndeme con el número o con el nombre del servicio que te interesa.");
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
        StringBuilder respuesta = new StringBuilder(encabezado).append("\n\nSucursales disponibles:\n");
        for (int i = 0; i < sucursales.size(); i++) {
            respuesta.append(i + 1)
                    .append(". ")
                    .append(nombreVisibleSucursal(sucursales.get(i).getNombre()))
                    .append("\n");
        }
        respuesta.append("\nRespóndeme con el número o con parte del nombre de la sucursal.");
        return respuesta.toString().trim();
    }

    private String construirPreguntaServicios(Long sucursalId, String encabezado) {
        SucursalEntidad sucursal = sucursalRepositorio.findById(sucursalId).orElse(null);
        List<ServicioEntidad> servicios = servicioRepositorio.findBySucursalIdAndActivoTrue(sucursalId).stream()
                .sorted(Comparator.comparing(ServicioEntidad::getNombre))
                .toList();
        if (servicios.isEmpty()) {
            return "No encontré servicios activos para esa sucursal. Si deseas, responde Ubicación o Mis citas.";
        }

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
        return respuesta.toString().trim();
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
        });

        if (paraAgendar) {
            respuesta.append("\nRespóndeme solo con la hora que prefieras, por ejemplo 10:30.");
        } else {
            respuesta.append("\nSi quieres apartar uno de estos horarios, responde: Quiero agendar.");
        }
        return respuesta.toString().trim();
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

    private ServicioEntidad resolverServicioPorTexto(Long sucursalId, String texto) {
        List<ServicioEntidad> servicios = servicioRepositorio.findBySucursalIdAndActivoTrue(sucursalId).stream()
                .sorted(Comparator.comparing(ServicioEntidad::getNombre))
                .toList();
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
        String candidato = valor == null ? "" : valor.trim();
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

    private String normalizarTextoLibre(String valor) {
        String base = valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
        String sinAcentos = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinAcentos.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
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
        Long citaId = extraerIdConfirmacion(cliente, mensaje);
        CitaClienteResponse cita = servicioCitasCliente.confirmar(obtenerEmpresaId(), cliente.getUsuarioId(), citaId);
        return "Tu cita de %s para el %s quedó confirmada.".formatted(
                cita.servicioNombre(),
                formatearFechaHoraCliente(cita.inicio())
        );
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
        return propiedadesWhatsapp.empresaIdPorDefecto() != null ? propiedadesWhatsapp.empresaIdPorDefecto() : 1L;
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
        String visible = nombre.replace("NailArt Studio", "").trim();
        if (visible.startsWith("-")) {
            visible = visible.substring(1).trim();
        }
        if (visible.isBlank()) {
            visible = nombre.trim();
        }
        return visible.length() > 28 ? visible.substring(0, 28).trim() + "..." : visible;
    }

    private String capitalizar(String valor) {
        if (!tieneTexto(valor)) {
            return "";
        }
        return valor.substring(0, 1).toUpperCase(Locale.ROOT) + valor.substring(1);
    }
}
