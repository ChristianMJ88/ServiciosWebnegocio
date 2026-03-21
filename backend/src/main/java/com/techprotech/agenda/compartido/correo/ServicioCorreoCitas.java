package com.techprotech.agenda.compartido.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Currency;
import java.util.Locale;

@Service
public class ServicioCorreoCitas {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioCorreoCitas.class);
    private static final Locale LOCALE_MX = Locale.forLanguageTag("es-MX");

    private final ServicioConfiguracionCorreoEmpresa servicioConfiguracionCorreoEmpresa;
    private final ClienteCorreoSaliente clienteCorreoSaliente;

    public ServicioCorreoCitas(
            ServicioConfiguracionCorreoEmpresa servicioConfiguracionCorreoEmpresa,
            ClienteCorreoSaliente clienteCorreoSaliente
    ) {
        this.servicioConfiguracionCorreoEmpresa = servicioConfiguracionCorreoEmpresa;
        this.clienteCorreoSaliente = clienteCorreoSaliente;
    }

    public boolean estaHabilitado(Long empresaId) {
        return servicioConfiguracionCorreoEmpresa.resolver(empresaId).habilitado();
    }

    public boolean enviarConfirmacion(Long empresaId, ConfirmacionCitaCorreo confirmacion) {
        ConfiguracionCorreoResolvida configuracion = servicioConfiguracionCorreoEmpresa.resolver(empresaId);
        if (!configuracion.habilitado()) {
            throw new IllegalStateException("La configuracion de correo no esta habilitada para la empresa " + empresaId);
        }

        if (confirmacion.destinatario() == null || confirmacion.destinatario().isBlank()) {
            throw new IllegalArgumentException("El destinatario del correo es invalido");
        }

        if (configuracion.remitente() == null || configuracion.remitente().isBlank()) {
            throw new IllegalStateException("No hay remitente configurado para la empresa " + empresaId);
        }

        clienteCorreoSaliente.enviar(
                configuracion,
                new MensajeCorreoSaliente(
                        confirmacion.destinatario(),
                        construirAsunto(confirmacion),
                        construirTextoPlano(confirmacion),
                        construirContenidoHtml(confirmacion),
                        configuracion.responderA(),
                        List.of(construirAdjuntoCalendario(confirmacion))
                )
        );
        LOGGER.info("Correo de confirmacion enviado para la cita {} de la empresa {}", confirmacion.citaId(), empresaId);
        return true;
    }

    private String construirAsunto(ConfirmacionCitaCorreo confirmacion) {
        return "Confirmacion de tu cita #" + confirmacion.citaId() + " | " + confirmacion.nombreEmpresa();
    }

    private String construirTextoPlano(ConfirmacionCitaCorreo confirmacion) {
        StringBuilder contenido = new StringBuilder();
        contenido.append("Hola ").append(confirmacion.nombreCliente()).append(",\n\n");
        contenido.append("Tu cita fue registrada correctamente.\n\n");
        contenido.append("Folio: ").append(confirmacion.citaId()).append('\n');
        contenido.append("Servicio: ").append(confirmacion.servicioNombre()).append('\n');
        contenido.append("Sucursal: ").append(confirmacion.sucursalNombre()).append('\n');
        contenido.append("Fecha: ").append(formatearFecha(confirmacion.inicio(), confirmacion.zonaHoraria())).append('\n');
        contenido.append("Horario: ")
                .append(formatearHora(confirmacion.inicio(), confirmacion.zonaHoraria()))
                .append(" - ")
                .append(formatearHora(confirmacion.fin(), confirmacion.zonaHoraria()))
                .append(" (")
                .append(confirmacion.zonaHoraria().getId())
                .append(")\n");

        if (confirmacion.precio() != null && confirmacion.moneda() != null && !confirmacion.moneda().isBlank()) {
            contenido.append("Precio estimado: ").append(formatearMoneda(confirmacion.precio(), confirmacion.moneda())).append('\n');
        }

        if (confirmacion.sucursalDireccion() != null && !confirmacion.sucursalDireccion().isBlank()) {
            contenido.append("Direccion: ").append(confirmacion.sucursalDireccion()).append('\n');
        }

        if (confirmacion.sucursalTelefono() != null && !confirmacion.sucursalTelefono().isBlank()) {
            contenido.append("Telefono de sucursal: ").append(confirmacion.sucursalTelefono()).append('\n');
        }

        contenido.append("\nSi necesitas reprogramar o cancelar, ponte en contacto con la sucursal.\n\n");
        contenido.append("Gracias por tu preferencia.\n");
        contenido.append(confirmacion.nombreEmpresa());
        return contenido.toString();
    }

    private String construirContenidoHtml(ConfirmacionCitaCorreo confirmacion) {
        String precio = null;
        if (confirmacion.precio() != null && confirmacion.moneda() != null && !confirmacion.moneda().isBlank()) {
            precio = formatearMoneda(confirmacion.precio(), confirmacion.moneda());
        }

        String direccion = textoSeguro(confirmacion.sucursalDireccion());
        String telefono = textoSeguro(confirmacion.sucursalTelefono());
        String googleCalendarUrl = construirUrlGoogleCalendar(confirmacion);
        String outlookCalendarUrl = construirUrlOutlookCalendar(confirmacion);
        String responderTexto = telefono != null
                ? "Si necesitas reprogramar o cancelar, ponte en contacto con la sucursal al " + telefono + "."
                : "Si necesitas reprogramar o cancelar, responde a este correo o ponte en contacto con la sucursal.";

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"es\"><head><meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>Confirmacion de cita</title></head>");
        html.append("<body style=\"margin:0;padding:0;background:#f4f7fb;font-family:Arial,'Helvetica Neue',sans-serif;color:#153049;\">");
        html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:#f4f7fb;padding:24px 12px;\">");
        html.append("<tr><td align=\"center\">");
        html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:640px;background:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 18px 45px rgba(21,48,73,0.10);\">");
        html.append("<tr><td style=\"background:linear-gradient(135deg,#0f3d62,#1e6ea1);padding:36px 40px;color:#ffffff;\">");
        html.append("<div style=\"font-size:12px;letter-spacing:0.16em;text-transform:uppercase;opacity:0.8;\">Confirmacion de cita</div>");
        html.append("<h1 style=\"margin:12px 0 10px;font-size:30px;line-height:1.2;font-weight:700;\">Hola, ")
                .append(escapeHtml(confirmacion.nombreCliente()))
                .append("</h1>");
        html.append("<p style=\"margin:0;font-size:16px;line-height:1.6;max-width:460px;\">Tu reserva quedo registrada exitosamente. Aqui tienes un resumen claro de tu cita para que la tengas a la mano.</p>");
        html.append("</td></tr>");

        html.append("<tr><td style=\"padding:32px 40px 18px;\">");
        html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse:separate;border-spacing:0 14px;\">");
        agregarFilaResumen(html, "Folio", "#" + confirmacion.citaId());
        agregarFilaResumen(html, "Servicio", confirmacion.servicioNombre());
        agregarFilaResumen(html, "Sucursal", confirmacion.sucursalNombre());
        agregarFilaResumen(html, "Fecha", capitalizar(formatearFecha(confirmacion.inicio(), confirmacion.zonaHoraria())));
        agregarFilaResumen(
                html,
                "Horario",
                formatearHora(confirmacion.inicio(), confirmacion.zonaHoraria())
                        + " - "
                        + formatearHora(confirmacion.fin(), confirmacion.zonaHoraria())
                        + " (" + confirmacion.zonaHoraria().getId() + ")"
        );
        if (precio != null) {
            agregarFilaResumen(html, "Precio estimado", precio);
        }
        if (direccion != null) {
            agregarFilaResumen(html, "Direccion", direccion);
        }
        if (telefono != null) {
            agregarFilaResumen(html, "Telefono", telefono);
        }
        html.append("</table>");
        html.append("</td></tr>");

        html.append("<tr><td style=\"padding:0 40px 24px;\">");
        html.append("<div style=\"background:#eef5fb;border:1px solid #d7e7f5;border-radius:18px;padding:20px 22px;\">");
        html.append("<div style=\"font-size:13px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#1e6ea1;margin-bottom:8px;\">Siguiente paso</div>");
        html.append("<p style=\"margin:0;font-size:15px;line-height:1.7;color:#274863;\">")
                .append(escapeHtml(responderTexto))
                .append("</p>");
        html.append("</div>");
        html.append("</td></tr>");

        html.append("<tr><td style=\"padding:0 40px 28px;\">");
        html.append("<div style=\"font-size:13px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#5b7388;margin-bottom:14px;\">Agregar a tu calendario</div>");
        html.append("<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\"><tr>");
        html.append("<td style=\"padding-right:12px;padding-bottom:12px;\">");
        html.append("<a href=\"").append(escapeHtmlAtributo(googleCalendarUrl)).append("\" style=\"display:inline-block;padding:14px 20px;border-radius:14px;background:#1f8f4e;color:#ffffff;text-decoration:none;font-size:14px;font-weight:700;\">Agregar a Google Calendar</a>");
        html.append("</td>");
        html.append("<td style=\"padding-bottom:12px;\">");
        html.append("<a href=\"").append(escapeHtmlAtributo(outlookCalendarUrl)).append("\" style=\"display:inline-block;padding:14px 20px;border-radius:14px;background:#0f6cbd;color:#ffffff;text-decoration:none;font-size:14px;font-weight:700;\">Abrir en Outlook</a>");
        html.append("</td>");
        html.append("</tr></table>");
        html.append("<p style=\"margin:6px 0 0;font-size:13px;line-height:1.6;color:#6b7f91;\">Tambien adjuntamos un archivo .ics por si prefieres abrirlo desde Apple Calendar o cualquier otra app compatible.</p>");
        html.append("</td></tr>");

        html.append("<tr><td style=\"padding:0 40px 36px;\">");
        html.append("<p style=\"margin:0;font-size:15px;line-height:1.7;color:#4d6376;\">Gracias por confiar en <strong>")
                .append(escapeHtml(confirmacion.nombreEmpresa()))
                .append("</strong>. Estaremos listos para recibirte.</p>");
        html.append("</td></tr>");
        html.append("</table>");
        html.append("</td></tr></table></body></html>");
        return html.toString();
    }

    private void agregarFilaResumen(StringBuilder html, String etiqueta, String valor) {
        html.append("<tr><td style=\"width:36%;padding:14px 18px;background:#f8fbfe;border-radius:14px 0 0 14px;font-size:13px;font-weight:700;letter-spacing:0.04em;text-transform:uppercase;color:#5b7388;\">")
                .append(escapeHtml(etiqueta))
                .append("</td>");
        html.append("<td style=\"padding:14px 18px;background:#f8fbfe;border-radius:0 14px 14px 0;font-size:15px;line-height:1.6;color:#16324a;\">")
                .append(escapeHtml(valor))
                .append("</td></tr>");
    }

    private String textoSeguro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private String escapeHtml(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String capitalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        return valor.substring(0, 1).toUpperCase(LOCALE_MX) + valor.substring(1);
    }

    private String escapeHtmlAtributo(String valor) {
        return escapeHtml(valor);
    }

    private AdjuntoCorreoSaliente construirAdjuntoCalendario(ConfirmacionCitaCorreo confirmacion) {
        String contenido = construirContenidoIcs(confirmacion);
        return new AdjuntoCorreoSaliente(
                "cita-" + confirmacion.citaId() + ".ics",
                "text/calendar; charset=UTF-8; method=PUBLISH",
                contenido.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String construirContenidoIcs(ConfirmacionCitaCorreo confirmacion) {
        String descripcion = construirDescripcionCalendario(confirmacion);
        String ubicacion = construirUbicacionCalendario(confirmacion);
        DateTimeFormatter formatterUtc = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        return new StringBuilder()
                .append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//Tecprotech//Agenda//ES\r\n")
                .append("CALSCALE:GREGORIAN\r\n")
                .append("METHOD:PUBLISH\r\n")
                .append("BEGIN:VEVENT\r\n")
                .append("UID:cita-").append(confirmacion.citaId()).append("@tecprotech.com.mx\r\n")
                .append("DTSTAMP:").append(OffsetDateTime.now(ZoneOffset.UTC).format(formatterUtc)).append("\r\n")
                .append("DTSTART:").append(confirmacion.inicio().withOffsetSameInstant(ZoneOffset.UTC).format(formatterUtc)).append("\r\n")
                .append("DTEND:").append(confirmacion.fin().withOffsetSameInstant(ZoneOffset.UTC).format(formatterUtc)).append("\r\n")
                .append("SUMMARY:").append(escapeIcs(confirmacion.servicioNombre() + " - " + confirmacion.nombreEmpresa())).append("\r\n")
                .append("DESCRIPTION:").append(escapeIcs(descripcion)).append("\r\n")
                .append("LOCATION:").append(escapeIcs(ubicacion)).append("\r\n")
                .append("STATUS:CONFIRMED\r\n")
                .append("END:VEVENT\r\n")
                .append("END:VCALENDAR\r\n")
                .toString();
    }

    private String construirDescripcionCalendario(ConfirmacionCitaCorreo confirmacion) {
        StringBuilder descripcion = new StringBuilder();
        descripcion.append("Cita confirmada para ").append(confirmacion.nombreCliente()).append(". ");
        descripcion.append("Folio ").append(confirmacion.citaId()).append(". ");
        descripcion.append("Servicio: ").append(confirmacion.servicioNombre()).append(". ");
        descripcion.append("Sucursal: ").append(confirmacion.sucursalNombre()).append(". ");
        if (confirmacion.sucursalTelefono() != null && !confirmacion.sucursalTelefono().isBlank()) {
            descripcion.append("Telefono: ").append(confirmacion.sucursalTelefono()).append(". ");
        }
        descripcion.append("Generado por ").append(confirmacion.nombreEmpresa()).append(".");
        return descripcion.toString();
    }

    private String construirUbicacionCalendario(ConfirmacionCitaCorreo confirmacion) {
        if (confirmacion.sucursalDireccion() != null && !confirmacion.sucursalDireccion().isBlank()) {
            return confirmacion.sucursalNombre() + ", " + confirmacion.sucursalDireccion();
        }
        return confirmacion.sucursalNombre();
    }

    private String construirUrlGoogleCalendar(ConfirmacionCitaCorreo confirmacion) {
        return "https://calendar.google.com/calendar/render?action=TEMPLATE"
                + "&text=" + codificarUrl(confirmacion.servicioNombre() + " - " + confirmacion.nombreEmpresa())
                + "&dates=" + formatearFechaCalendarioUrl(confirmacion.inicio()) + "/" + formatearFechaCalendarioUrl(confirmacion.fin())
                + "&details=" + codificarUrl(construirDescripcionCalendario(confirmacion))
                + "&location=" + codificarUrl(construirUbicacionCalendario(confirmacion))
                + "&ctz=" + codificarUrl(confirmacion.zonaHoraria().getId());
    }

    private String construirUrlOutlookCalendar(ConfirmacionCitaCorreo confirmacion) {
        return "https://outlook.office.com/calendar/0/deeplink/compose?path=/calendar/action/compose&rru=addevent"
                + "&subject=" + codificarUrl(confirmacion.servicioNombre() + " - " + confirmacion.nombreEmpresa())
                + "&startdt=" + codificarUrl(confirmacion.inicio().toString())
                + "&enddt=" + codificarUrl(confirmacion.fin().toString())
                + "&body=" + codificarUrl(construirDescripcionCalendario(confirmacion))
                + "&location=" + codificarUrl(construirUbicacionCalendario(confirmacion));
    }

    private String formatearFechaCalendarioUrl(OffsetDateTime fecha) {
        return fecha.withOffsetSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    }

    private String codificarUrl(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private String escapeIcs(String valor) {
        return valor
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    private String formatearFecha(OffsetDateTime fecha, ZoneId zonaHoraria) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", LOCALE_MX);
        return fecha.atZoneSameInstant(zonaHoraria).format(formatter);
    }

    private String formatearHora(OffsetDateTime fecha, ZoneId zonaHoraria) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE_MX);
        return fecha.atZoneSameInstant(zonaHoraria).format(formatter);
    }

    private String formatearMoneda(BigDecimal cantidad, String moneda) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(LOCALE_MX);
        formatter.setCurrency(Currency.getInstance(moneda));
        return formatter.format(cantidad);
    }

}
