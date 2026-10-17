package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.citas.dominio.eventos.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.TransactionalEventListener;
import static org.assertj.core.api.Assertions.*;

class EventosCitaArquitecturaTest {
    @Test void productoresMigradosNoProgramanOutboxNiRegistranEfectosAsincronos() throws Exception {
        for (String ruta : List.of("citas/aplicacion/ServicioCierreCita", "citas/aplicacion/ServicioCancelacionCita",
                "citas/aplicacion/ServicioCitas", "citas/aplicacion/ServicioCitasCliente",
                "recepcion/aplicacion/ServicioRecepcion", "admin/aplicacion/ServicioAdminCitas",
                "pagos/aplicacion/ServicioConfirmacionCitasCheckout")) {
            var codigo = Files.readString(Path.of("src/main/java/com/techprotech/agenda/modulos/" + ruta + ".java"));
            assertThat(codigo).as(ruta).doesNotContain("ServicioOutbox", ".programar", "@Async", "@TransactionalEventListener");
        }
        assertThat(NotificacionesEventosCita.class.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.MANDATORY);
        assertThat(NotificacionesEventosCita.class.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class)).isFalse();
        var manejadores = Arrays.stream(NotificacionesEventosCita.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(EventListener.class)).toList();
        assertThat(manejadores).hasSize(7);
        assertThat(manejadores).allSatisfy(m -> {
            assertThat(m.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class)).isFalse();
            assertThat(m.getReturnType()).isEqualTo(void.class);
            assertThat(m.isAnnotationPresent(TransactionalEventListener.class)).isFalse();
        });
        var permitidos = Set.of(
                Path.of("src/main/java/com/techprotech/agenda/modulos/citas/aplicacion/NotificacionesEventosCita.java"),
                Path.of("src/main/java/com/techprotech/agenda/modulos/citas/aplicacion/NotificacionesRegistroCita.java"),
                Path.of("src/main/java/com/techprotech/agenda/compartido/whatsapp/ProgramadorRecordatoriosWhatsapp.java"),
                Path.of("src/main/java/com/techprotech/agenda/compartido/whatsapp/ServicioOutboxWhatsappCitas.java"),
                Path.of("src/main/java/com/techprotech/agenda/compartido/correo/ServicioOutboxCorreoCitas.java"));
        var referenciaOutbox = java.util.regex.Pattern.compile("\\bServicioOutbox(?:Correo|Whatsapp)Citas\\b");
        try (var rutas = Files.walk(Path.of("src/main/java"))) {
            for (var ruta : rutas.filter(p -> p.toString().endsWith(".java")).toList()) {
                var codigo = Files.readString(ruta);
                assertThat(codigo).as(ruta.toString()).doesNotContain("setTaskExecutor(");
                if (referenciaOutbox.matcher(codigo).find()) assertThat(permitidos).as(ruta.toString()).contains(ruta);
            }
        }
    }

    @Test void eventosSonRecordsConSnapshotYNoReferenciasJpa() {
        for (var tipo : List.of(CitaFinalizada.class, CitaNoAsistio.class, CitaCancelada.class,
                CitaConfirmada.class, CitaReprogramada.class, PagoCitaRegistrado.class,
                CitaApartadoVencido.class, PagoCitaConIncidencia.class)) {
            assertThat(tipo.isRecord()).isTrue();
            assertThat(tipo.getRecordComponents()).anySatisfy(c -> assertThat(c.getType()).isEqualTo(DatosEventoCita.class));
            assertThat(tipo.getRecordComponents()).allSatisfy(c ->
                    assertThat(c.getType().getPackageName()).doesNotContain("infraestructura"));
        }
    }
}
