package com.techprotech.agenda.modulos.admin.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.correo.ConfiguracionCorreoEmpresaEntidad;
import com.techprotech.agenda.compartido.correo.ConfiguracionCorreoEmpresaRepositorio;
import com.techprotech.agenda.compartido.correo.ProtectorSecretosCorreo;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionCorreoAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionCorreoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.MigracionSecretosCorreoResponse;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaRolEmpresaRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioAdminCitasCorreoTest {

    @Test
    void guardaPasswordSmtpCifradoDesdeAdmin() {
        Map<Long, ConfiguracionCorreoEmpresaEntidad> store = new HashMap<>();
        ProtectorSecretosCorreo protector = new ProtectorSecretosCorreo(
                new com.techprotech.agenda.compartido.correo.PropiedadesCorreo(
                        true,
                        "SMTP",
                        "fallback@agenda.local",
                        "Agenda",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "llave-prueba",
                        20,
                        60
                )
        );
        ServicioAdminCitas servicio = crearServicio(store, protector);

        ConfiguracionCorreoAdminResponse response = servicio.actualizarConfiguracionCorreo(
                1L,
                null,
                new ConfiguracionCorreoAdminRequest(
                        true,
                        "SMTP",
                        "citas@tenant.com",
                        "Tenant Uno",
                        "soporte@tenant.com",
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
                )
        );

        assertTrue(response.smtpPasswordConfigurada());
        assertTrue(response.smtpPasswordCifrada());
        assertFalse(response.requiereMigracionSecretos());
        assertNotNull(store.get(1L));
        assertTrue(store.get(1L).getSmtpPassword().startsWith("enc:v1:"));
        assertEquals("tenant-pass", protector.desencriptarSiNecesario(store.get(1L).getSmtpPassword()));
    }

    @Test
    void migraPasswordPlanoExistente() {
        Map<Long, ConfiguracionCorreoEmpresaEntidad> store = new HashMap<>();
        ConfiguracionCorreoEmpresaEntidad entidad = new ConfiguracionCorreoEmpresaEntidad();
        entidad.setEmpresaId(2L);
        entidad.setHabilitado(true);
        entidad.setSmtpPassword("legacy-plano");
        store.put(2L, entidad);

        ProtectorSecretosCorreo protector = new ProtectorSecretosCorreo(
                new com.techprotech.agenda.compartido.correo.PropiedadesCorreo(
                        true,
                        "SMTP",
                        "fallback@agenda.local",
                        "Agenda",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "llave-prueba",
                        20,
                        60
                )
        );
        ServicioAdminCitas servicio = crearServicio(store, protector);

        MigracionSecretosCorreoResponse response = servicio.migrarSecretosCorreo(2L, null);

        assertTrue(response.actualizada());
        assertTrue(store.get(2L).getSmtpPassword().startsWith("enc:v1:"));
        assertEquals("legacy-plano", protector.desencriptarSiNecesario(store.get(2L).getSmtpPassword()));
    }

    @Test
    void guardaSecretoGraphCifradoDesdeAdmin() {
        Map<Long, ConfiguracionCorreoEmpresaEntidad> store = new HashMap<>();
        ProtectorSecretosCorreo protector = new ProtectorSecretosCorreo(
                new com.techprotech.agenda.compartido.correo.PropiedadesCorreo(
                        true,
                        "SMTP",
                        "fallback@agenda.local",
                        "Agenda",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "llave-prueba",
                        20,
                        60
                )
        );
        ServicioAdminCitas servicio = crearServicio(store, protector);

        ConfiguracionCorreoAdminResponse response = servicio.actualizarConfiguracionCorreo(
                3L,
                null,
                new ConfiguracionCorreoAdminRequest(
                        true,
                        "GRAPH",
                        "no-reply@notificaciones.tecprotech.com.mx",
                        "TecProTech",
                        "soporte@tecprotech.com.mx",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "tenant-id",
                        "client-id",
                        "graph-secret",
                        "no-reply@notificaciones.tecprotech.com.mx",
                        null,
                        null
                )
        );

        assertEquals("GRAPH", response.proveedor());
        assertTrue(response.graphClientSecretConfigurado());
        assertTrue(response.graphClientSecretCifrado());
        assertEquals("graph-secret", protector.desencriptarSiNecesario(store.get(3L).getGraphClientSecret()));
    }

    private ServicioAdminCitas crearServicio(
            Map<Long, ConfiguracionCorreoEmpresaEntidad> store,
            ProtectorSecretosCorreo protector
    ) {
        return new ServicioAdminCitas(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new BCryptPasswordEncoder(),
                null,
                null,
                crearRepositorio(store),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                crearAuditoriaConfiguracionRepositorio(),
                crearAuditoriaRolRepositorio(),
                new ObjectMapper(),
                protector
        );
    }

    private ConfiguracionCorreoEmpresaRepositorio crearRepositorio(Map<Long, ConfiguracionCorreoEmpresaEntidad> store) {
        return (ConfiguracionCorreoEmpresaRepositorio) Proxy.newProxyInstance(
                ConfiguracionCorreoEmpresaRepositorio.class.getClassLoader(),
                new Class[]{ConfiguracionCorreoEmpresaRepositorio.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.ofNullable(store.get((Long) args[0]));
                    case "save" -> {
                        ConfiguracionCorreoEmpresaEntidad entidad = (ConfiguracionCorreoEmpresaEntidad) args[0];
                        store.put(entidad.getEmpresaId(), entidad);
                        yield entidad;
                    }
                    case "toString" -> "RepositorioCorreoAdminStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Metodo no soportado en prueba: " + method.getName());
                }
        );
    }

    private AuditoriaConfiguracionEmpresaRepositorio crearAuditoriaConfiguracionRepositorio() {
        return (AuditoriaConfiguracionEmpresaRepositorio) Proxy.newProxyInstance(
                AuditoriaConfiguracionEmpresaRepositorio.class.getClassLoader(),
                new Class[]{AuditoriaConfiguracionEmpresaRepositorio.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> args[0];
                    case "findTop30ByEmpresaIdOrderByCreadoEnDesc" -> List.of();
                    case "toString" -> "AuditoriaConfiguracionRepositorioStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Metodo no soportado en prueba: " + method.getName());
                }
        );
    }

    private AuditoriaRolEmpresaRepositorio crearAuditoriaRolRepositorio() {
        return (AuditoriaRolEmpresaRepositorio) Proxy.newProxyInstance(
                AuditoriaRolEmpresaRepositorio.class.getClassLoader(),
                new Class[]{AuditoriaRolEmpresaRepositorio.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> args[0];
                    case "findTop30ByEmpresaIdOrderByCreadoEnDesc" -> List.of();
                    case "toString" -> "AuditoriaRolRepositorioStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Metodo no soportado en prueba: " + method.getName());
                }
        );
    }
}
