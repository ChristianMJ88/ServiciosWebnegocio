package com.techprotech.agenda.modulos.citas.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.compartido.correo.*;
import com.techprotech.agenda.compartido.tiempo.TiempoPruebas;
import com.techprotech.agenda.compartido.whatsapp.*;
import com.techprotech.agenda.modulos.citas.dominio.eventos.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventosCitaTransaccionTest {
    @Configuration @EnableTransactionManagement static class Transacciones {}

    static class Registro {
        final List<EventoCita> recibidos = new ArrayList<>();
        boolean fallar;
        @org.springframework.core.annotation.Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
        @EventListener public void recibir(EventoCita evento) {
            recibidos.add(evento);
            if (fallar) throw new IllegalStateException("Simula un consumidor transaccional de 007");
        }
    }

    @Test void cambioHistorialYDosCanalesSeReviertenJuntosYElReintentoPublicaSoloUnaVez() {
        var registro = new StandardServiceRegistryBuilder().applySettings(TiempoPruebas.configuracionJpa())
                .applySettings(Map.of("hibernate.connection.driver_class", "org.h2.Driver",
                        "hibernate.connection.url", "jdbc:h2:mem:eventos_006;MODE=MySQL",
                        "hibernate.hbm2ddl.auto", "create-drop")).build();
        try (var factory = new MetadataSources(registro).addAnnotatedClass(CitaEntidad.class)
                .addAnnotatedClass(PagoCitaEntidad.class).addAnnotatedClass(VentaEntidad.class).addAnnotatedClass(VentaDetalleEntidad.class).addAnnotatedClass(com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad.class)
                .addAnnotatedClass(HistorialEstadoCitaEntidad.class).addAnnotatedClass(BandejaSalidaNotificacionEntidad.class)
                .buildMetadata().buildSessionFactory(); var contexto = new AnnotationConfigApplicationContext()) {
            var em = SharedEntityManagerCreator.createSharedEntityManager(factory);
            var repositorios = new JpaRepositoryFactory(em);
            var citas = repositorios.getRepository(CitaRepositorio.class);
            var historial = repositorios.getRepository(HistorialEstadoCitaRepositorio.class);
            var reclamos = new BandejaSalidaNotificacionRepositorioImpl();
            org.springframework.test.util.ReflectionTestUtils.setField(reclamos, "entityManager", em);
            var bandeja = repositorios.getRepository(BandejaSalidaNotificacionRepositorio.class,
                    org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments.just(reclamos));
            var reloj = Clock.fixed(Instant.parse("2026-10-16T18:00:00Z"), ZoneId.of("America/Mexico_City"));
            var config = mock(ServicioConfiguracionCorreoEmpresa.class);
            var resuelta = new ConfiguracionCorreoResolvida(7L,true,ProveedorCorreo.SMTP,"prueba@example.test","Prueba",null,
                    "localhost",1025,null,null,false,false,null,null,null,null,null,null,null);
            when(config.resolver(7L)).thenReturn(resuelta);
            var proveedor = mock(ClienteWhatsappTwilio.class); when(proveedor.estaHabilitado(7L)).thenReturn(true);
            var clientes = mock(ClienteRepositorio.class);
            var cliente = new ClienteEntidad(); cliente.setAceptaWhatsapp(true); cliente.setTelefono("2220000000");
            when(clientes.findByClienteIdAndEmpresaId(20L, 7L)).thenReturn(Optional.of(cliente));
            var json = new ObjectMapper().findAndRegisterModules();
            var correo = new ServicioOutboxCorreoCitas(bandeja, config, json, reloj);
            var whatsapp = new ServicioOutboxWhatsappCitas(bandeja, proveedor, json, reloj);
            var transacciones = new JpaTransactionManager(factory);
            contexto.register(Transacciones.class);
            contexto.registerBean("transactionManager", JpaTransactionManager.class, () -> transacciones);
            contexto.registerBean(NotificacionesEventosCita.class, () -> new NotificacionesEventosCita(correo, whatsapp, clientes));
            var recibidos = new Registro(); contexto.registerBean(Registro.class, () -> recibidos);
            contexto.refresh();
            var eventos = new PublicadorEventosCita(contexto, reloj);
            var cierre = new ServicioCierreCita(citas, historial, eventos, reloj, TiempoPruebas.zonaUtc());
            var tx = new TransactionTemplate(transacciones);
            Long id = tx.execute(status -> { var cita = cita(); em.persist(cita); em.flush(); return cita.getId(); });
            var actor = new UsuarioAutenticado("prueba@example.test", 11L, 7L, "EMPRESA", List.of(), List.of(), List.of(3L));
            // La transacción ya escribió estado, historial y outbox antes de fallar.
            assertThatThrownBy(() -> tx.execute(status -> {
                cierre.cerrar(actor, id, "FINALIZADA", OrigenCierreCita.STAFF);
                em.flush(); assertThat(bandeja.count()).isEqualTo(2);
                throw new IllegalStateException("Falla después de programar ambos canales");
            })).isInstanceOf(IllegalStateException.class);
            tx.executeWithoutResult(status -> {
                assertThat(citas.findById(id).orElseThrow().getEstado()).isEqualTo("CONFIRMADA");
                assertThat(historial.count()).isZero(); assertThat(bandeja.count()).isZero();
            });
            recibidos.fallar = true;
            assertThatThrownBy(() -> tx.execute(status -> {
                cierre.cerrar(actor, id, "NO_ASISTIO", OrigenCierreCita.STAFF);
                return null;
            })).isInstanceOf(IllegalStateException.class);
            recibidos.fallar = false;
            tx.executeWithoutResult(status -> {
                assertThat(citas.findById(id).orElseThrow().getEstado()).isEqualTo("CONFIRMADA");
                assertThat(historial.count()).isZero(); assertThat(bandeja.count()).isZero();
            });
            recibidos.recibidos.clear();
            tx.executeWithoutResult(status -> {
                cierre.cerrar(actor, id, "FINALIZADA", OrigenCierreCita.STAFF);
                cierre.cerrar(actor, id, "FINALIZADA", OrigenCierreCita.STAFF);
            });
            assertThat(recibidos.recibidos).hasSize(1).first().isInstanceOf(CitaFinalizada.class);
            assertThat(recibidos.recibidos.getFirst().datos()).satisfies(d -> {
                assertThat(d.empresaId()).isEqualTo(7L); assertThat(d.sucursalId()).isEqualTo(3L);
                assertThat(d.actorId()).isEqualTo(11L); assertThat(d.origen()).isEqualTo(OrigenEventoCita.STAFF);
                assertThat(d.momento()).isEqualTo(Instant.parse("2026-10-16T18:00:00Z"));
                assertThat(d.inicio()).isEqualTo(LocalDateTime.parse("2026-10-16T10:00"));
            });
            tx.executeWithoutResult(status -> {
                assertThat(historial.count()).isEqualTo(1); assertThat(bandeja.findAll()).extracting(BandejaSalidaNotificacionEntidad::getTipoEvento)
                        .containsExactlyInAnyOrder("CITA_GRACIAS_VISITA_EMAIL", "CITA_GRACIAS_VISITA_WHATSAPP");
            });
            assertThatThrownBy(() -> eventos.publicar(new CitaNoAsistio(recibidos.recibidos.getFirst().datos())))
                    .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
            // Nunca se envía al proveedor desde el listener: solo se consulta habilitación.
            verify(proveedor, times(2)).estaHabilitado(7L); verifyNoMoreInteractions(proveedor);
        } finally { StandardServiceRegistryBuilder.destroy(registro); }
    }

    private CitaEntidad cita() {
        var c = new CitaEntidad(); c.setEmpresaId(7L); c.setSucursalId(3L); c.setServicioId(5L);
        c.setPrestadorId(11L); c.setClienteId(20L); c.setCreadaPorUsuarioId(11L);
        c.setEstado("CONFIRMADA"); c.setPrecio(new BigDecimal("500")); c.setMoneda("MXN");
        c.setInicio(LocalDateTime.parse("2026-10-16T10:00")); c.setFin(c.getInicio().plusHours(1));
        return c;
    }
}
