package com.techprotech.agenda.compartido.correo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioConfiguracionCorreoEmpresaTest {

    @Test
    void usaFallbackGlobalCuandoLaEmpresaNoTieneConfiguracion() {
        ServicioConfiguracionCorreoEmpresa servicio = new ServicioConfiguracionCorreoEmpresa(
                crearRepositorio(Optional.empty()),
                new PropiedadesCorreo(true, "SMTP", "global@agenda.local", "Agenda Global", "reply@agenda.local", null, null, null, null, null, null, "llave-prueba", 20, 60),
                crearMailProperties("smtp.global.local", 2525, "global-user", "secret"),
                new ProtectorSecretosCorreo(new PropiedadesCorreo(true, "SMTP", "global@agenda.local", "Agenda Global", "reply@agenda.local", null, null, null, null, null, null, "llave-prueba", 20, 60))
        );

        ConfiguracionCorreoResolvida configuracion = servicio.resolver(8L);

        assertTrue(configuracion.habilitado());
        assertEquals("global@agenda.local", configuracion.remitente());
        assertEquals("Agenda Global", configuracion.nombreRemitente());
        assertEquals("reply@agenda.local", configuracion.responderA());
        assertEquals("smtp.global.local", configuracion.smtpHost());
        assertEquals(2525, configuracion.smtpPort());
        assertEquals("global-user", configuracion.smtpUsername());
    }

    @Test
    void respetaLaConfiguracionEspecificaDelTenant() {
        ConfiguracionCorreoEmpresaEntidad entidad = new ConfiguracionCorreoEmpresaEntidad();
        entidad.setEmpresaId(15L);
        entidad.setHabilitado(true);
        entidad.setRemitente("hola@tenant.com");
        entidad.setNombreRemitente("Tenant Uno");
        entidad.setResponderA("soporte@tenant.com");
        entidad.setSmtpHost("smtp.tenant.com");
        entidad.setSmtpPort(587);
        entidad.setSmtpUsername("tenant-user");
        entidad.setSmtpPassword(new ProtectorSecretosCorreo(
                new PropiedadesCorreo(true, "SMTP", "global@agenda.local", "Agenda Global", null, null, null, null, null, null, null, "llave-prueba", 20, 60)
        ).encriptar("tenant-pass"));
        entidad.setSmtpAuth(true);
        entidad.setSmtpStartTls(true);

        ServicioConfiguracionCorreoEmpresa servicio = new ServicioConfiguracionCorreoEmpresa(
                crearRepositorio(Optional.of(entidad)),
                new PropiedadesCorreo(true, "SMTP", "global@agenda.local", "Agenda Global", null, null, null, null, null, null, null, "llave-prueba", 20, 60),
                crearMailProperties("smtp.global.local", 2525, "global-user", "secret"),
                new ProtectorSecretosCorreo(new PropiedadesCorreo(true, "SMTP", "global@agenda.local", "Agenda Global", null, null, null, null, null, null, null, "llave-prueba", 20, 60))
        );

        ConfiguracionCorreoResolvida configuracion = servicio.resolver(15L);

        assertTrue(configuracion.habilitado());
        assertEquals(ProveedorCorreo.SMTP, configuracion.proveedor());
        assertEquals("hola@tenant.com", configuracion.remitente());
        assertEquals("Tenant Uno", configuracion.nombreRemitente());
        assertEquals("soporte@tenant.com", configuracion.responderA());
        assertEquals("smtp.tenant.com", configuracion.smtpHost());
        assertEquals(587, configuracion.smtpPort());
        assertEquals("tenant-user", configuracion.smtpUsername());
        assertEquals("tenant-pass", configuracion.smtpPassword());
        assertTrue(configuracion.smtpAuth());
        assertTrue(configuracion.smtpStartTls());
    }

    @Test
    void aceptaTemporalmentePasswordPlanoMientrasSeMigra() {
        ConfiguracionCorreoEmpresaEntidad entidad = new ConfiguracionCorreoEmpresaEntidad();
        entidad.setEmpresaId(16L);
        entidad.setHabilitado(true);
        entidad.setRemitente("hola@tenant.com");
        entidad.setSmtpHost("smtp.tenant.com");
        entidad.setSmtpPort(587);
        entidad.setSmtpPassword("password-plano");

        ServicioConfiguracionCorreoEmpresa servicio = new ServicioConfiguracionCorreoEmpresa(
                crearRepositorio(Optional.of(entidad)),
                new PropiedadesCorreo(true, "SMTP", "global@agenda.local", "Agenda Global", null, null, null, null, null, null, null, "llave-prueba", 20, 60),
                crearMailProperties("smtp.global.local", 2525, "global-user", "secret"),
                new ProtectorSecretosCorreo(new PropiedadesCorreo(true, "SMTP", "global@agenda.local", "Agenda Global", null, null, null, null, null, null, null, "llave-prueba", 20, 60))
        );

        ConfiguracionCorreoResolvida configuracion = servicio.resolver(16L);

        assertEquals("password-plano", configuracion.smtpPassword());
    }

    @Test
    void deshabilitaElCorreoCuandoElModuloGlobalEstaApagado() {
        ServicioConfiguracionCorreoEmpresa servicio = new ServicioConfiguracionCorreoEmpresa(
                crearRepositorio(Optional.empty()),
                new PropiedadesCorreo(false, "SMTP", "global@agenda.local", "Agenda Global", null, null, null, null, null, null, null, "llave-prueba", 20, 60),
                crearMailProperties("smtp.global.local", 2525, "global-user", "secret"),
                new ProtectorSecretosCorreo(new PropiedadesCorreo(false, "SMTP", "global@agenda.local", "Agenda Global", null, null, null, null, null, null, null, "llave-prueba", 20, 60))
        );

        ConfiguracionCorreoResolvida configuracion = servicio.resolver(99L);

        assertFalse(configuracion.habilitado());
        assertNull(configuracion.remitente());
    }

    @Test
    void resuelveConfiguracionGraphDelTenant() {
        ConfiguracionCorreoEmpresaEntidad entidad = new ConfiguracionCorreoEmpresaEntidad();
        entidad.setEmpresaId(21L);
        entidad.setHabilitado(true);
        entidad.setProveedor("GRAPH");
        entidad.setRemitente("no-reply@notificaciones.tecprotech.com.mx");
        entidad.setNombreRemitente("TecProTech");
        entidad.setResponderA("soporte@tecprotech.com.mx");
        entidad.setGraphTenantId("tenant-id");
        entidad.setGraphClientId("client-id");
        entidad.setGraphClientSecret(new ProtectorSecretosCorreo(
                new PropiedadesCorreo(true, "GRAPH", "global@agenda.local", "Agenda Global", null, null, null, null, null, null, null, "llave-prueba", 20, 60)
        ).encriptar("graph-secret"));
        entidad.setGraphUserId("no-reply@notificaciones.tecprotech.com.mx");

        ServicioConfiguracionCorreoEmpresa servicio = new ServicioConfiguracionCorreoEmpresa(
                crearRepositorio(Optional.of(entidad)),
                new PropiedadesCorreo(true, "SMTP", "global@agenda.local", "Agenda Global", null, null, null, null, null, null, null, "llave-prueba", 20, 60),
                crearMailProperties("smtp.global.local", 2525, "global-user", "secret"),
                new ProtectorSecretosCorreo(new PropiedadesCorreo(true, "SMTP", "global@agenda.local", "Agenda Global", null, null, null, null, null, null, null, "llave-prueba", 20, 60))
        );

        ConfiguracionCorreoResolvida configuracion = servicio.resolver(21L);

        assertTrue(configuracion.habilitado());
        assertEquals(ProveedorCorreo.GRAPH, configuracion.proveedor());
        assertEquals("tenant-id", configuracion.graphTenantId());
        assertEquals("client-id", configuracion.graphClientId());
        assertEquals("graph-secret", configuracion.graphClientSecret());
        assertEquals("no-reply@notificaciones.tecprotech.com.mx", configuracion.graphUserId());
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

    private ConfiguracionCorreoEmpresaRepositorio crearRepositorio(Optional<ConfiguracionCorreoEmpresaEntidad> resultado) {
        return (ConfiguracionCorreoEmpresaRepositorio) Proxy.newProxyInstance(
                ConfiguracionCorreoEmpresaRepositorio.class.getClassLoader(),
                new Class[]{ConfiguracionCorreoEmpresaRepositorio.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> resultado;
                    case "toString" -> "RepositorioCorreoStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Metodo no soportado en prueba: " + method.getName());
                }
        );
    }
}
