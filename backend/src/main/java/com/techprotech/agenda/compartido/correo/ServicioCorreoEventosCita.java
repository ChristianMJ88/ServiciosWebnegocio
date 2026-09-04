package com.techprotech.agenda.compartido.correo;

import com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

@Service
public class ServicioCorreoEventosCita {

    private static final Locale LOCALE_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", LOCALE_MX);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", LOCALE_MX);

    private final CitaRepositorio citaRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final EmpresaSitioConfigRepositorio sitioRepositorio;
    private final ServicioConfiguracionCorreoEmpresa configuracionCorreo;
    private final ClienteCorreoSaliente clienteCorreo;
    private final String origenPublico;

    public ServicioCorreoEventosCita(
            CitaRepositorio citaRepositorio,
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            EmpresaRepositorio empresaRepositorio,
            ServicioRepositorio servicioRepositorio,
            SucursalRepositorio sucursalRepositorio,
            EmpresaSitioConfigRepositorio sitioRepositorio,
            ServicioConfiguracionCorreoEmpresa configuracionCorreo,
            ClienteCorreoSaliente clienteCorreo,
            @Value("${aplicacion.correo-bienvenida.origen-publico}") String origenPublico
    ) {
        this.citaRepositorio = citaRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.empresaRepositorio = empresaRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.sitioRepositorio = sitioRepositorio;
        this.configuracionCorreo = configuracionCorreo;
        this.clienteCorreo = clienteCorreo;
        this.origenPublico = normalizarOrigen(origenPublico);
    }

    @Transactional(readOnly = true)
    public void enviar(Long empresaId, String evento, EventoCitaCorreoPayload payload) {
        ConfiguracionCorreoResolvida configuracion = configuracionCorreo.resolver(empresaId);
        if (!configuracion.habilitado()) {
            throw new IllegalStateException("El correo no está habilitado para la empresa " + empresaId);
        }

        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaId(payload.citaId(), empresaId)
                .orElseThrow(() -> new IllegalStateException("La cita ya no existe"));
        if (payload.inicioEsperado() != null && eventoDependeDelHorario(evento)
                && !payload.inicioEsperado().equals(cita.getInicio())) {
            return;
        }

        ClienteEntidad cliente = clienteRepositorio.findById(cita.getClienteId()).orElse(null);
        UsuarioEntidad usuario = usuarioRepositorio.findByIdAndEmpresaId(cita.getClienteId(), empresaId).orElse(null);
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId).orElse(null);
        ServicioEntidad servicio = servicioRepositorio.findById(cita.getServicioId()).orElse(null);
        SucursalEntidad sucursal = sucursalRepositorio.findById(cita.getSucursalId()).orElse(null);
        if (cliente == null || usuario == null || empresa == null || servicio == null || sucursal == null
                || !empresaId.equals(servicio.getEmpresaId()) || !empresaId.equals(sucursal.getEmpresaId())) {
            throw new IllegalStateException("No se pudo resolver la información multitenant de la cita");
        }
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            return;
        }

        EmpresaSitioConfigEntidad sitio = sitioRepositorio.findById(empresaId).orElse(null);
        ZoneId zona = ZoneId.of(sucursal.getZonaHoraria());
        String nombreEmpresa = sitio != null && tieneTexto(sitio.getNombreComercial())
                ? sitio.getNombreComercial() : empresa.getNombre();
        String color = sitio != null && colorValido(sitio.getColorPrimario()) ? sitio.getColorPrimario() : "#173b57";
        String urlSitio = sitio != null && tieneTexto(sitio.getSlug()) ? origenPublico + "/e/" + sitio.getSlug() : null;

        if (esNotificacionNegocio(evento)) {
            enviarAlNegocio(configuracion, cita, servicio, sucursal, sitio, nombreEmpresa,
                    cliente.getNombreCompleto(), usuario.getCorreo(), zona, color, urlSitio);
            return;
        }

        DatosEvento datos = datosEvento(evento, empresa.getNombre(), cliente.getNombreCompleto());
        List<AdjuntoCorreoSaliente> adjuntos = incluyeCalendario(evento)
                ? List.of(crearCalendario(cita, servicio, sucursal, nombreEmpresa, zona)) : List.of();
        clienteCorreo.enviar(configuracion, new MensajeCorreoSaliente(
                usuario.getCorreo(),
                datos.asunto().formatted(nombreEmpresa),
                textoPlano(datos, cita, servicio, sucursal, nombreEmpresa, cliente.getNombreCompleto(), zona, urlSitio),
                html(datos, cita, servicio, sucursal, sitio, nombreEmpresa, cliente.getNombreCompleto(), zona, color, urlSitio),
                configuracion.responderA(),
                adjuntos
        ));
    }

    private DatosEvento datosEvento(String evento, String empresa, String cliente) {
        return switch (evento) {
            case "CITA_REGISTRADA_EMAIL" -> new DatosEvento("Recibimos tu solicitud de cita | %s", "Solicitud recibida", "Tu cita fue registrada y está pendiente de confirmación.");
            case "CITA_CONFIRMADA_EMAIL" -> new DatosEvento("Tu cita está confirmada | %s", "Cita confirmada", "Tu lugar quedó confirmado. Te esperamos.");
            case "CITA_REPROGRAMADA_EMAIL" -> new DatosEvento("Tu cita cambió de horario | %s", "Cita reprogramada", "Actualizamos la fecha y el horario de tu cita. Revisa los nuevos datos.");
            case "CITA_RECORDATORIO_CONFIRMACION_EMAIL" -> new DatosEvento("Confirma tu próxima cita | %s", "Confirmación pendiente", "Tu cita se aproxima y todavía está pendiente de confirmación.");
            case "CITA_RECORDATORIO_EMAIL" -> new DatosEvento("Recordatorio de tu cita | %s", "Tu cita se aproxima", "Te recordamos los datos de tu próxima cita.");
            case "CITA_CANCELADA_CLIENTE_EMAIL" -> new DatosEvento("Cancelaste tu cita | %s", "Cita cancelada", "Registramos correctamente la cancelación que solicitaste.");
            case "CITA_CANCELADA_NEGOCIO_EMAIL" -> new DatosEvento("Actualización importante de tu cita | %s", "Cita cancelada por el negocio", "El negocio tuvo que cancelar esta cita. Comunícate con la sucursal para reagendar.");
            case "CITA_LIBERADA_SIN_CONFIRMACION_EMAIL" -> new DatosEvento("Tu horario fue liberado | %s", "Cita liberada", "Como no recibimos tu confirmación a tiempo, el horario fue liberado.");
            case "CITA_GRACIAS_VISITA_EMAIL" -> new DatosEvento("Gracias por tu visita | %s", "Gracias por visitarnos", "Agradecemos tu preferencia. Será un gusto atenderte nuevamente.");
            case "CITA_NO_ASISTIO_EMAIL" -> new DatosEvento("No pudimos atenderte en tu cita | %s", "No registramos tu asistencia", "Si todavía deseas el servicio, agenda una nueva fecha o comunícate con la sucursal.");
            default -> throw new IllegalArgumentException("Evento de correo no soportado: " + evento);
        };
    }

    private void enviarAlNegocio(ConfiguracionCorreoResolvida configuracion, CitaEntidad cita,
                                 ServicioEntidad servicio, SucursalEntidad sucursal,
                                 EmpresaSitioConfigEntidad sitio, String nombreEmpresa,
                                 String nombreCliente, String correoCliente, ZoneId zona,
                                 String color, String urlSitio) {
        String destinatario = resolverCorreoNegocio(sitio, configuracion);
        if (!tieneTexto(destinatario)) {
            throw new IllegalStateException("No hay correo del negocio configurado para la empresa " + cita.getEmpresaId());
        }
        DatosEvento datos = new DatosEvento("Nueva cita registrada | %s", "Nueva cita", "Se registró una nueva solicitud de cita.");
        String texto = textoPlano(datos, cita, servicio, sucursal, nombreEmpresa, nombreCliente, zona, urlSitio)
                + "\nCorreo del cliente: " + correoCliente;
        String contenidoHtml = html(datos, cita, servicio, sucursal, sitio, nombreEmpresa,
                nombreCliente, zona, color, urlSitio)
                .replace("</table></td></tr></table></body></html>",
                        "<p style=\"margin:0 38px 30px;color:#607486;font-size:14px\"><strong>Correo del cliente:</strong> "
                                + esc(correoCliente) + "</p></table></td></tr></table></body></html>");
        clienteCorreo.enviar(configuracion, new MensajeCorreoSaliente(
                destinatario,
                datos.asunto().formatted(nombreEmpresa),
                texto,
                contenidoHtml,
                correoCliente,
                List.of()
        ));
    }

    private String resolverCorreoNegocio(EmpresaSitioConfigEntidad sitio, ConfiguracionCorreoResolvida configuracion) {
        if (sitio != null && tieneTexto(sitio.getCorreo())) return sitio.getCorreo();
        if (tieneTexto(configuracion.responderA())) return configuracion.responderA();
        return configuracion.remitente();
    }

    private boolean esNotificacionNegocio(String evento) {
        return "CITA_REGISTRADA_NEGOCIO_EMAIL".equals(evento);
    }

    private String textoPlano(DatosEvento evento, CitaEntidad cita, ServicioEntidad servicio, SucursalEntidad sucursal,
                              String empresa, String cliente, ZoneId zona, String urlSitio) {
        StringBuilder texto = new StringBuilder("Hola ").append(cliente).append(",\n\n")
                .append(evento.mensaje()).append("\n\n")
                .append("Folio: #").append(cita.getId()).append('\n')
                .append("Servicio: ").append(servicio.getNombre()).append('\n')
                .append("Sucursal: ").append(sucursal.getNombre()).append('\n')
                .append("Fecha: ").append(capitalizar(cita.getInicio().atZone(zona).format(FECHA))).append('\n')
                .append("Horario: ").append(cita.getInicio().atZone(zona).format(HORA)).append(" - ")
                .append(cita.getFin().atZone(zona).format(HORA)).append('\n');
        if (cita.getPrecio() != null) texto.append("Precio: ").append(moneda(cita.getPrecio(), cita.getMoneda())).append('\n');
        if (tieneTexto(sucursal.getDireccion())) texto.append("Dirección: ").append(sucursal.getDireccion()).append('\n');
        if (tieneTexto(sucursal.getTelefono())) texto.append("Teléfono: ").append(sucursal.getTelefono()).append('\n');
        if (urlSitio != null) texto.append("\nAgenda o consulta servicios: ").append(urlSitio).append('\n');
        return texto.append("\n").append(empresa).toString();
    }

    private String html(DatosEvento evento, CitaEntidad cita, ServicioEntidad servicio, SucursalEntidad sucursal,
                        EmpresaSitioConfigEntidad sitio, String empresa, String cliente, ZoneId zona, String color, String urlSitio) {
        String logoUrl = sitio != null && tieneTexto(sitio.getLogoUrl())
                ? urlAbsoluta(sitio.getLogoUrl()) : origenPublico + "/fluora-apple-touch-icon.png";
        String logo = "<img src=\"" + attr(logoUrl) + "\" alt=\"" + attr(empresa)
                + "\" style=\"max-height:64px;max-width:180px;margin-bottom:18px\">";
        String boton = urlSitio != null
                ? "<p style=\"margin:26px 0 0\"><a href=\"" + attr(urlSitio) + "\" style=\"display:inline-block;background:" + attr(color) + ";color:#fff;text-decoration:none;padding:13px 20px;border-radius:12px;font-weight:700\">Ver servicios del negocio</a></p>" : "";
        return """
                <!doctype html><html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;background:#f3f6f9;font-family:Arial,sans-serif;color:#183247"><table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="padding:24px 12px"><tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:640px;background:#fff;border-radius:22px;overflow:hidden">
                <tr><td style="background:%s;padding:34px 38px;color:#fff">%s<div style="font-size:13px;opacity:.82;text-transform:uppercase;letter-spacing:.12em">%s</div><h1 style="margin:10px 0 0;font-size:29px">Hola, %s</h1></td></tr>
                <tr><td style="padding:30px 38px"><p style="font-size:16px;line-height:1.65;margin-top:0">%s</p>
                <table role="presentation" width="100%%" cellpadding="9" cellspacing="0" style="background:#f7f9fb;border-radius:14px"><tr><td><strong>Folio</strong></td><td>#%d</td></tr><tr><td><strong>Servicio</strong></td><td>%s</td></tr><tr><td><strong>Sucursal</strong></td><td>%s</td></tr><tr><td><strong>Fecha</strong></td><td>%s</td></tr><tr><td><strong>Horario</strong></td><td>%s - %s</td></tr>%s%s%s</table>%s
                <p style="margin:28px 0 0;color:#607486;font-size:14px">Este mensaje corresponde a una cita con <strong>%s</strong>.</p></td></tr></table></td></tr></table></body></html>
                """.formatted(
                attr(color), logo, esc(evento.titulo()), esc(cliente), esc(evento.mensaje()), cita.getId(),
                esc(servicio.getNombre()), esc(sucursal.getNombre()), esc(capitalizar(cita.getInicio().atZone(zona).format(FECHA))),
                cita.getInicio().atZone(zona).format(HORA), cita.getFin().atZone(zona).format(HORA),
                filaOpcional("Precio", cita.getPrecio() != null ? moneda(cita.getPrecio(), cita.getMoneda()) : null),
                filaOpcional("Dirección", sucursal.getDireccion()), filaOpcional("Teléfono", sucursal.getTelefono()),
                boton, esc(empresa));
    }

    private AdjuntoCorreoSaliente crearCalendario(CitaEntidad cita, ServicioEntidad servicio, SucursalEntidad sucursal, String empresa, ZoneId zona) {
        DateTimeFormatter utc = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        String ics = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Fluora//Citas//ES\r\nCALSCALE:GREGORIAN\r\nMETHOD:PUBLISH\r\nBEGIN:VEVENT\r\n"
                + "UID:cita-" + cita.getEmpresaId() + "-" + cita.getId() + "@refluora.com\r\n"
                + "DTSTAMP:" + LocalDateTime.now().atOffset(ZoneOffset.UTC).format(utc) + "\r\n"
                + "DTSTART:" + cita.getInicio().atZone(zona).withZoneSameInstant(ZoneOffset.UTC).format(utc) + "\r\n"
                + "DTEND:" + cita.getFin().atZone(zona).withZoneSameInstant(ZoneOffset.UTC).format(utc) + "\r\n"
                + "SUMMARY:" + ics(servicio.getNombre() + " - " + empresa) + "\r\n"
                + "LOCATION:" + ics(sucursal.getNombre() + (tieneTexto(sucursal.getDireccion()) ? ", " + sucursal.getDireccion() : "")) + "\r\n"
                + "END:VEVENT\r\nEND:VCALENDAR\r\n";
        return new AdjuntoCorreoSaliente("cita-" + cita.getId() + ".ics", "text/calendar; charset=UTF-8", ics.getBytes(StandardCharsets.UTF_8));
    }

    private boolean incluyeCalendario(String evento) {
        return List.of("CITA_REGISTRADA_EMAIL", "CITA_CONFIRMADA_EMAIL", "CITA_REPROGRAMADA_EMAIL",
                "CITA_RECORDATORIO_CONFIRMACION_EMAIL", "CITA_RECORDATORIO_EMAIL").contains(evento);
    }

    private boolean eventoDependeDelHorario(String evento) {
        return !List.of("CITA_CANCELADA_CLIENTE_EMAIL", "CITA_CANCELADA_NEGOCIO_EMAIL", "CITA_GRACIAS_VISITA_EMAIL", "CITA_NO_ASISTIO_EMAIL").contains(evento);
    }

    private String filaOpcional(String etiqueta, String valor) {
        return tieneTexto(valor) ? "<tr><td><strong>" + esc(etiqueta) + "</strong></td><td>" + esc(valor) + "</td></tr>" : "";
    }
    private String moneda(BigDecimal valor, String codigo) { NumberFormat f = NumberFormat.getCurrencyInstance(LOCALE_MX); if (tieneTexto(codigo)) f.setCurrency(Currency.getInstance(codigo)); return f.format(valor); }
    private boolean colorValido(String valor) { return valor != null && valor.matches("#[0-9a-fA-F]{6}"); }
    private String capitalizar(String v) { return v == null || v.isBlank() ? "" : v.substring(0, 1).toUpperCase(LOCALE_MX) + v.substring(1); }
    private boolean tieneTexto(String v) { return v != null && !v.isBlank(); }
    private String esc(String v) { return v == null ? "" : v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    private String attr(String v) { return esc(v); }
    private String urlAbsoluta(String v) { return v.startsWith("http://") || v.startsWith("https://") ? v : origenPublico + (v.startsWith("/") ? v : "/" + v); }
    private String normalizarOrigen(String v) { return v != null && v.endsWith("/") ? v.substring(0, v.length() - 1) : v; }
    private String ics(String v) { return v == null ? "" : v.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n"); }
    private record DatosEvento(String asunto, String titulo, String mensaje) {}
}
