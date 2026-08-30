package com.techprotech.agenda.compartido.correo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioOutboxCorreoBienvenidaTest {

    @Test
    void programaBienvenidaConIdentidadCanonica() {
        List<BandejaSalidaNotificacionEntidad> guardados = new ArrayList<>();
        BandejaSalidaNotificacionRepositorio repositorio = (BandejaSalidaNotificacionRepositorio) Proxy.newProxyInstance(
                BandejaSalidaNotificacionRepositorio.class.getClassLoader(),
                new Class[]{BandejaSalidaNotificacionRepositorio.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByAgregadoIdAndCanalAndTipoEventoAndEstadoIn" -> false;
                    case "save" -> { guardados.add((BandejaSalidaNotificacionEntidad) args[0]); yield args[0]; }
                    case "toString" -> "OutboxBienvenidaStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        ClienteCorreoSendgrid sendgrid = new ClienteCorreoSendgrid(
                RestClient.builder(),
                new PropiedadesCorreoPlataforma(true, "SG.test", "no-reply@refluora.com", "Fluora", "contacto@refluora.com"));
        ServicioOutboxCorreoBienvenida servicio = new ServicioOutboxCorreoBienvenida(
                repositorio, sendgrid, new ObjectMapper().findAndRegisterModules());

        assertTrue(servicio.programar(new BienvenidaEmpresaCorreo(
                7L, "Estudio Aurora", "Ana Pérez", "ana@example.com", "estudio-aurora")));
        assertEquals("EMPRESA_BIENVENIDA_EMAIL", guardados.getFirst().getTipoEvento());
        assertTrue(guardados.getFirst().getPayloadJson().contains("estudio-aurora"));
    }
}
