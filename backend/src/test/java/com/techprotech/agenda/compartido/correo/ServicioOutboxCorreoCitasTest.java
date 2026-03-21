package com.techprotech.agenda.compartido.correo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioOutboxCorreoCitasTest {

    @Test
    void programaNotificacionEnOutboxCuandoLaEmpresaTieneCorreoActivo() {
        List<BandejaSalidaNotificacionEntidad> guardados = new ArrayList<>();
        ServicioOutboxCorreoCitas servicio = new ServicioOutboxCorreoCitas(
                crearRepositorioOutbox(guardados),
                new ServicioConfiguracionCorreoEmpresa(
                        crearRepositorioConfiguracion(crearConfigEmpresaActiva()),
                        new PropiedadesCorreo(true, "SMTP", "fallback@agenda.local", "Agenda", null, null, null, null, null, null, null, "llave-prueba", 20, 60),
                        crearMailProperties(),
                        new ProtectorSecretosCorreo(new PropiedadesCorreo(true, "SMTP", "fallback@agenda.local", "Agenda", null, null, null, null, null, null, null, "llave-prueba", 20, 60))
                ),
                new ObjectMapper().findAndRegisterModules()
        );

        boolean programado = servicio.programarConfirmacion(1L, crearConfirmacion());

        assertTrue(programado);
        assertEquals(1, guardados.size());
        assertEquals("PENDIENTE", guardados.getFirst().getEstado());
        assertEquals("EMAIL", guardados.getFirst().getCanal());
    }

    @Test
    void noProgramaNadaSiLaEmpresaNoTieneCorreoDisponible() {
        List<BandejaSalidaNotificacionEntidad> guardados = new ArrayList<>();
        ServicioOutboxCorreoCitas servicio = new ServicioOutboxCorreoCitas(
                crearRepositorioOutbox(guardados),
                new ServicioConfiguracionCorreoEmpresa(
                        crearRepositorioConfiguracion(null),
                        new PropiedadesCorreo(false, "SMTP", "fallback@agenda.local", "Agenda", null, null, null, null, null, null, null, "llave-prueba", 20, 60),
                        crearMailProperties(),
                        new ProtectorSecretosCorreo(new PropiedadesCorreo(false, "SMTP", "fallback@agenda.local", "Agenda", null, null, null, null, null, null, null, "llave-prueba", 20, 60))
                ),
                new ObjectMapper().findAndRegisterModules()
        );

        boolean programado = servicio.programarConfirmacion(1L, crearConfirmacion());

        assertFalse(programado);
        assertTrue(guardados.isEmpty());
    }

    private ConfirmacionCitaCorreo crearConfirmacion() {
        return new ConfirmacionCitaCorreo(
                45L,
                "Empresa Demo",
                "Ana Garcia",
                "cliente@correo.com",
                "Pedicura",
                "Sucursal Centro",
                "Av. Reforma 123",
                "5550000000",
                OffsetDateTime.parse("2026-03-25T10:00:00-06:00"),
                OffsetDateTime.parse("2026-03-25T11:15:00-06:00"),
                ZoneId.of("America/Mexico_City"),
                new BigDecimal("320.00"),
                "MXN"
        );
    }

    private ConfiguracionCorreoEmpresaEntidad crearConfigEmpresaActiva() {
        ConfiguracionCorreoEmpresaEntidad entidad = new ConfiguracionCorreoEmpresaEntidad();
        entidad.setEmpresaId(1L);
        entidad.setHabilitado(true);
        entidad.setRemitente("hola@tenant.com");
        entidad.setSmtpHost("smtp.tenant.com");
        entidad.setSmtpPort(587);
        return entidad;
    }

    private ConfiguracionCorreoEmpresaRepositorio crearRepositorioConfiguracion(ConfiguracionCorreoEmpresaEntidad entidad) {
        return (ConfiguracionCorreoEmpresaRepositorio) Proxy.newProxyInstance(
                ConfiguracionCorreoEmpresaRepositorio.class.getClassLoader(),
                new Class[]{ConfiguracionCorreoEmpresaRepositorio.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> java.util.Optional.ofNullable(entidad);
                    case "toString" -> "RepositorioConfiguracionStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Metodo no soportado en prueba: " + method.getName());
                }
        );
    }

    private BandejaSalidaNotificacionRepositorio crearRepositorioOutbox(List<BandejaSalidaNotificacionEntidad> guardados) {
        return (BandejaSalidaNotificacionRepositorio) Proxy.newProxyInstance(
                BandejaSalidaNotificacionRepositorio.class.getClassLoader(),
                new Class[]{BandejaSalidaNotificacionRepositorio.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        BandejaSalidaNotificacionEntidad entidad = (BandejaSalidaNotificacionEntidad) args[0];
                        guardados.add(entidad);
                        yield entidad;
                    }
                    case "reclamarPendientesEmail" -> List.of();
                    case "toString" -> "RepositorioOutboxStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Metodo no soportado en prueba: " + method.getName());
                }
        );
    }

    private MailProperties crearMailProperties() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setHost("smtp.global.local");
        mailProperties.setPort(2525);
        return mailProperties;
    }
}
