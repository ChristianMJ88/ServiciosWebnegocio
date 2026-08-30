package com.techprotech.agenda.compartido.correo;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioCorreoBienvenida {

    private static final String SOPORTE = "contacto@refluora.com";
    private static final String ACCESO = "https://app.refluora.com/acceso";
    private static final String ORIGEN = "https://refluora.com";

    private final ClienteCorreoSendgrid correoSendgrid;

    public ServicioCorreoBienvenida(ClienteCorreoSendgrid correoSendgrid) {
        this.correoSendgrid = correoSendgrid;
    }

    public void enviar(BienvenidaEmpresaCorreo bienvenida) {
        ConfiguracionCorreoResolvida canonica = correoSendgrid.configuracionCanonica();
        if (!canonica.habilitado()) {
            throw new IllegalStateException("El correo canónico de Fluora no está configurado");
        }
        String sitio = ORIGEN + "/e/" + bienvenida.slug();
        correoSendgrid.enviar(canonica, new MensajeCorreoSaliente(
                bienvenida.correoAdministrador(),
                "Te damos la bienvenida a Fluora | " + bienvenida.nombreEmpresa(),
                texto(bienvenida, sitio),
                html(bienvenida, sitio),
                SOPORTE,
                List.of()
        ));
    }

    private String texto(BienvenidaEmpresaCorreo bienvenida, String sitio) {
        return "Hola " + bienvenida.nombreAdministrador() + ",\n\n"
                + "Tu negocio " + bienvenida.nombreEmpresa() + " ya fue creado en Fluora.\n\n"
                + "Primeros pasos:\n"
                + "1. Inicia sesión en " + ACCESO + "\n"
                + "2. Completa los datos, horarios y branding de tu negocio.\n"
                + "3. Crea tus servicios, precios y duración.\n"
                + "4. Agrega a tu equipo y asigna roles.\n"
                + "5. Configura correo y WhatsApp.\n"
                + "6. Revisa y comparte tu sitio público: " + sitio + "\n\n"
                + "Si tienes dudas, escríbenos a " + SOPORTE + ".\n\nEquipo Fluora";
    }

    private String html(BienvenidaEmpresaCorreo bienvenida, String sitio) {
        return """
                <!doctype html><html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;background:#f3f2fb;font-family:Arial,sans-serif;color:#17213b"><table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="padding:28px 12px"><tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:650px;background:#fff;border-radius:24px;overflow:hidden"><tr><td style="padding:36px;background:linear-gradient(135deg,#17194b,#6d4cff);color:#fff">
                <img src="https://refluora.com/fluora-apple-touch-icon.png" alt="Fluora" style="width:64px;height:64px;border-radius:15px;margin-bottom:18px"><div style="font-size:13px;letter-spacing:.12em;text-transform:uppercase;opacity:.8">Bienvenido a Fluora</div><h1 style="font-size:30px;margin:10px 0 0">%s ya está listo</h1></td></tr>
                <tr><td style="padding:34px 38px"><p style="font-size:17px;line-height:1.6;margin-top:0">Hola <strong>%s</strong>. Ya creamos tu negocio y tu acceso administrativo.</p><h2 style="font-size:20px">Comienza con estos primeros pasos</h2>
                <ol style="padding-left:22px;line-height:1.85"><li>Completa los datos, horarios y la imagen de tu negocio.</li><li>Crea servicios con precio y duración.</li><li>Agrega a tu equipo y asigna sus roles.</li><li>Configura el correo y conecta WhatsApp.</li><li>Revisa y comparte tu sitio público.</li></ol>
                <p style="margin:26px 0"><a href="%s" style="display:inline-block;background:#6d4cff;color:#fff;text-decoration:none;padding:14px 22px;border-radius:12px;font-weight:700">Entrar a Fluora</a>&nbsp; <a href="%s" style="display:inline-block;color:#5035cf;padding:14px 10px;font-weight:700">Ver mi sitio</a></p>
                <p style="font-size:14px;color:#647087;line-height:1.6">¿Tienes dudas? Escríbenos a <a href="mailto:%s">%s</a>.</p></td></tr></table></td></tr></table></body></html>
                """.formatted(esc(bienvenida.nombreEmpresa()), esc(bienvenida.nombreAdministrador()), ACCESO, sitio, SOPORTE, SOPORTE);
    }

    private String esc(String valor) {
        return valor == null ? "" : valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
