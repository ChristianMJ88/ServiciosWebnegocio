package com.techprotech.agenda.compartido.correo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioCorreoCitasTest {

    @Test
    void noEnviaCorreoCuandoLaEmpresaNoLoTieneHabilitado() {
        ClienteCorreoFalso clienteCorreoFalso = new ClienteCorreoFalso();
        ServicioCorreoCitas servicio = new ServicioCorreoCitas(
                crearServicioConfiguracion(new ConfiguracionCorreoResolvida(false, ProveedorCorreo.SMTP, null, null, null, null, 0, null, null, false, false, null, null, null, null, null, null)),
                clienteCorreoFalso
        );

        boolean lanzo = false;
        try {
            servicio.enviarConfirmacion(3L, crearConfirmacion());
        } catch (IllegalStateException ex) {
            lanzo = true;
        }

        assertTrue(lanzo);
        assertFalse(clienteCorreoFalso.fueInvocado);
    }

    @Test
    void enviaCorreoConConfiguracionEspecificaDelTenant() {
        ClienteCorreoFalso clienteCorreoFalso = new ClienteCorreoFalso();
        ServicioCorreoCitas servicio = new ServicioCorreoCitas(
                crearServicioConfiguracion(new ConfiguracionCorreoResolvida(
                        true,
                        ProveedorCorreo.SMTP,
                        "hola@tenant.com",
                        "Tenant Uno",
                        "reply@tenant.com",
                        "smtp.tenant.com",
                        587,
                        "tenant-user",
                        "tenant-pass",
                        true,
                        true,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )),
                clienteCorreoFalso
        );

        boolean enviado = servicio.enviarConfirmacion(25L, crearConfirmacion());

        assertTrue(enviado);
        assertTrue(clienteCorreoFalso.fueInvocado);
        assertEquals("cliente@correo.com", clienteCorreoFalso.mensaje.destinatario());
        assertEquals("Confirmacion de tu cita #25 | Empresa Demo", clienteCorreoFalso.mensaje.asunto());
        assertEquals("reply@tenant.com", clienteCorreoFalso.mensaje.responderA());
        assertTrue(clienteCorreoFalso.mensaje.textoPlano().contains("Empresa Demo"));
        assertTrue(clienteCorreoFalso.mensaje.contenidoHtml().contains("<html"));
        assertTrue(clienteCorreoFalso.mensaje.contenidoHtml().contains("Ana Garcia"));
        assertTrue(clienteCorreoFalso.mensaje.contenidoHtml().contains("Agregar a Google Calendar"));
        assertEquals(1, clienteCorreoFalso.mensaje.adjuntos().size());
        assertEquals("cita-25.ics", clienteCorreoFalso.mensaje.adjuntos().get(0).nombreArchivo());
        assertTrue(new String(clienteCorreoFalso.mensaje.adjuntos().get(0).contenido()).contains("BEGIN:VCALENDAR"));
        assertEquals("hola@tenant.com", clienteCorreoFalso.configuracion.remitente());
    }

    @Test
    void reportaDeshabilitadoSiLaEmpresaNoTieneConfiguracionActiva() {
        ServicioCorreoCitas servicio = new ServicioCorreoCitas(
                crearServicioConfiguracion(new ConfiguracionCorreoResolvida(
                        false,
                        ProveedorCorreo.SMTP,
                        null,
                        null,
                        null,
                        null,
                        0,
                        null,
                        null,
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )),
                new ClienteCorreoFalso()
        );

        assertFalse(servicio.estaHabilitado(12L));
    }

    private ConfirmacionCitaCorreo crearConfirmacion() {
        return new ConfirmacionCitaCorreo(
                25L,
                "Empresa Demo",
                "Ana Garcia",
                "cliente@correo.com",
                "Manicura",
                "Sucursal Centro",
                "Av. Reforma 123",
                "5550000000",
                OffsetDateTime.parse("2026-03-25T10:00:00-06:00"),
                OffsetDateTime.parse("2026-03-25T11:00:00-06:00"),
                ZoneId.of("America/Mexico_City"),
                new BigDecimal("250.00"),
                "MXN"
        );
    }

    private ServicioConfiguracionCorreoEmpresa crearServicioConfiguracion(ConfiguracionCorreoResolvida configuracion) {
        ConfiguracionCorreoEmpresaEntidad entidad = new ConfiguracionCorreoEmpresaEntidad();
        entidad.setEmpresaId(1L);
        entidad.setHabilitado(configuracion.habilitado());
        entidad.setProveedor(configuracion.proveedor().name());
        entidad.setRemitente(configuracion.remitente());
        entidad.setNombreRemitente(configuracion.nombreRemitente());
        entidad.setResponderA(configuracion.responderA());
        entidad.setSmtpHost(configuracion.smtpHost());
        entidad.setSmtpPort(configuracion.smtpPort());
        entidad.setSmtpUsername(configuracion.smtpUsername());
        entidad.setSmtpPassword(configuracion.smtpPassword());
        entidad.setSmtpAuth(configuracion.smtpAuth());
        entidad.setSmtpStartTls(configuracion.smtpStartTls());
        entidad.setGraphTenantId(configuracion.graphTenantId());
        entidad.setGraphClientId(configuracion.graphClientId());
        entidad.setGraphClientSecret(configuracion.graphClientSecret());
        entidad.setGraphUserId(configuracion.graphUserId());
        entidad.setGraphCertificateThumbprint(configuracion.graphCertificateThumbprint());
        entidad.setGraphPrivateKeyPem(configuracion.graphPrivateKeyPem());

        return new ServicioConfiguracionCorreoEmpresa(
                crearRepositorio(entidad),
                new PropiedadesCorreo(true, "SMTP", "fallback@agenda.local", "Agenda Fallback", null, null, null, null, null, null, null, "llave-prueba", 20, 60),
                crearMailProperties("smtp.global.local", 2525, "global-user", "secret"),
                new ProtectorSecretosCorreo(new PropiedadesCorreo(true, "SMTP", "fallback@agenda.local", "Agenda Fallback", null, null, null, null, null, null, null, "llave-prueba", 20, 60))
        );
    }

    private MailProperties crearMailProperties(String host, int port, String username, String password) {
        MailProperties properties = new MailProperties();
        properties.setHost(host);
        properties.setPort(port);
        properties.setUsername(username);
        properties.setPassword(password);
        properties.getProperties().put("mail.smtp.auth", "true");
        properties.getProperties().put("mail.smtp.starttls.enable", "true");
        return properties;
    }

    private ConfiguracionCorreoEmpresaRepositorio crearRepositorio(ConfiguracionCorreoEmpresaEntidad entidad) {
        return (ConfiguracionCorreoEmpresaRepositorio) java.lang.reflect.Proxy.newProxyInstance(
                ConfiguracionCorreoEmpresaRepositorio.class.getClassLoader(),
                new Class[]{ConfiguracionCorreoEmpresaRepositorio.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> java.util.Optional.ofNullable(entidad);
                    case "toString" -> "RepositorioCorreoStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Metodo no soportado en prueba: " + method.getName());
                }
        );
    }

    private static final class ClienteCorreoFalso implements ClienteCorreoSaliente {

        private boolean fueInvocado;
        private ConfiguracionCorreoResolvida configuracion;
        private MensajeCorreoSaliente mensaje;

        @Override
        public void enviar(ConfiguracionCorreoResolvida configuracion, MensajeCorreoSaliente mensaje) {
            this.fueInvocado = true;
            this.configuracion = configuracion;
            this.mensaje = mensaje;
        }
    }
}
