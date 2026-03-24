package com.techprotech.agenda.compartido.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;

@Service
public class ServicioCorreoContactos {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioCorreoContactos.class);
    private static final Locale LOCALE_MX = Locale.forLanguageTag("es-MX");

    private final ServicioConfiguracionCorreoEmpresa servicioConfiguracionCorreoEmpresa;
    private final ClienteCorreoSaliente clienteCorreoSaliente;

    public ServicioCorreoContactos(
            ServicioConfiguracionCorreoEmpresa servicioConfiguracionCorreoEmpresa,
            ClienteCorreoSaliente clienteCorreoSaliente
    ) {
        this.servicioConfiguracionCorreoEmpresa = servicioConfiguracionCorreoEmpresa;
        this.clienteCorreoSaliente = clienteCorreoSaliente;
    }

    public boolean estaHabilitado(Long empresaId) {
        return servicioConfiguracionCorreoEmpresa.resolver(empresaId).habilitado();
    }

    public boolean enviarNotificacion(Long empresaId, NotificacionSolicitudContactoCorreo notificacion) {
        ConfiguracionCorreoResolvida configuracion = servicioConfiguracionCorreoEmpresa.resolver(empresaId);
        if (!configuracion.habilitado()) {
            throw new IllegalStateException("La configuracion de correo no esta habilitada para la empresa " + empresaId);
        }

        String destinatario = resolverDestinatario(configuracion);
        if (destinatario == null) {
            throw new IllegalStateException("No hay destinatario configurado para notificaciones de contacto");
        }

        clienteCorreoSaliente.enviar(
                configuracion,
                new MensajeCorreoSaliente(
                        destinatario,
                        construirAsunto(notificacion),
                        construirTextoPlano(notificacion),
                        construirContenidoHtml(notificacion),
                        notificacion.correoContacto(),
                        List.of()
                )
        );
        LOGGER.info("Correo de contacto enviado para la solicitud {} de la empresa {}", notificacion.solicitudContactoId(), empresaId);
        return true;
    }

    private String resolverDestinatario(ConfiguracionCorreoResolvida configuracion) {
        if (configuracion.responderA() != null && !configuracion.responderA().isBlank()) {
            return configuracion.responderA();
        }
        if (configuracion.remitente() != null && !configuracion.remitente().isBlank()) {
            return configuracion.remitente();
        }
        return null;
    }

    private String construirAsunto(NotificacionSolicitudContactoCorreo notificacion) {
        return "Nuevo mensaje de contacto #" + notificacion.solicitudContactoId() + " | " + notificacion.nombreEmpresa();
    }

    private String construirTextoPlano(NotificacionSolicitudContactoCorreo notificacion) {
        StringBuilder contenido = new StringBuilder();
        contenido.append("Se registro un nuevo mensaje desde el formulario de contacto.\n\n");
        contenido.append("Folio: ").append(notificacion.solicitudContactoId()).append('\n');
        contenido.append("Empresa: ").append(notificacion.nombreEmpresa()).append('\n');
        contenido.append("Nombre: ").append(notificacion.nombreContacto()).append('\n');
        contenido.append("Correo: ").append(notificacion.correoContacto()).append('\n');

        if (notificacion.telefonoContacto() != null && !notificacion.telefonoContacto().isBlank()) {
            contenido.append("Telefono: ").append(notificacion.telefonoContacto()).append('\n');
        }

        contenido.append("Canal: ").append(notificacion.canal()).append('\n');
        contenido.append("Fecha: ").append(formatearFecha(notificacion)).append('\n');
        contenido.append("Asunto: ").append(notificacion.asunto()).append("\n\n");
        contenido.append("Mensaje:\n").append(notificacion.mensaje()).append('\n');
        return contenido.toString();
    }

    private String construirContenidoHtml(NotificacionSolicitudContactoCorreo notificacion) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"es\"><head><meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>Nuevo contacto</title></head>");
        html.append("<body style=\"margin:0;padding:0;background:#f4f7fb;font-family:Arial,'Helvetica Neue',sans-serif;color:#153049;\">");
        html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:#f4f7fb;padding:24px 12px;\">");
        html.append("<tr><td align=\"center\">");
        html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:680px;background:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 18px 45px rgba(21,48,73,0.10);\">");
        html.append("<tr><td style=\"background:linear-gradient(135deg,#0f3d62,#1e6ea1);padding:32px 36px;color:#ffffff;\">");
        html.append("<div style=\"font-size:12px;letter-spacing:0.16em;text-transform:uppercase;opacity:0.8;\">Nuevo contacto web</div>");
        html.append("<h1 style=\"margin:12px 0 8px;font-size:28px;line-height:1.2;font-weight:700;\">")
                .append(escapeHtml(notificacion.nombreContacto()))
                .append("</h1>");
        html.append("<p style=\"margin:0;font-size:15px;line-height:1.6;max-width:480px;\">Se registró una nueva solicitud de contacto desde la web. Aquí tienes el detalle para dar seguimiento.</p>");
        html.append("</td></tr>");

        html.append("<tr><td style=\"padding:28px 36px 16px;\">");
        html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse:separate;border-spacing:0 12px;\">");
        agregarFilaResumen(html, "Folio", "#" + notificacion.solicitudContactoId());
        agregarFilaResumen(html, "Empresa", notificacion.nombreEmpresa());
        agregarFilaResumen(html, "Correo", notificacion.correoContacto());
        if (notificacion.telefonoContacto() != null && !notificacion.telefonoContacto().isBlank()) {
            agregarFilaResumen(html, "Telefono", notificacion.telefonoContacto());
        }
        agregarFilaResumen(html, "Canal", notificacion.canal());
        agregarFilaResumen(html, "Fecha", formatearFecha(notificacion));
        agregarFilaResumen(html, "Asunto", notificacion.asunto());
        html.append("</table>");
        html.append("</td></tr>");

        html.append("<tr><td style=\"padding:0 36px 36px;\">");
        html.append("<div style=\"padding:20px 22px;border-radius:18px;background:#f8fbfe;border:1px solid #d7e7f5;\">");
        html.append("<div style=\"font-size:13px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#1e6ea1;margin-bottom:10px;\">Mensaje</div>");
        html.append("<p style=\"margin:0;font-size:15px;line-height:1.75;color:#274863;white-space:pre-line;\">")
                .append(escapeHtml(notificacion.mensaje()))
                .append("</p>");
        html.append("</div>");
        html.append("</td></tr>");

        html.append("</table>");
        html.append("</td></tr></table></body></html>");
        return html.toString();
    }

    private void agregarFilaResumen(StringBuilder html, String etiqueta, String valor) {
        html.append("<tr><td style=\"width:34%;padding:14px 18px;background:#f8fbfe;border-radius:14px 0 0 14px;font-size:13px;font-weight:700;letter-spacing:0.04em;text-transform:uppercase;color:#5b7388;\">")
                .append(escapeHtml(etiqueta))
                .append("</td>");
        html.append("<td style=\"padding:14px 18px;background:#f8fbfe;border-radius:0 14px 14px 0;font-size:15px;line-height:1.6;color:#16324a;\">")
                .append(escapeHtml(valor))
                .append("</td></tr>");
    }

    private String formatearFecha(NotificacionSolicitudContactoCorreo notificacion) {
        ZoneId zona = notificacion.creadaEn().getOffset() != null
                ? ZoneId.ofOffset("UTC", notificacion.creadaEn().getOffset())
                : ZoneId.of("America/Mexico_City");
        return notificacion.creadaEn()
                .atZoneSameInstant(zona)
                .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy, HH:mm", LOCALE_MX));
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
}
