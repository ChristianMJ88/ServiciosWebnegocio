package com.techprotech.agenda.compartido.correo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.tiempo.TiempoPruebas;
import com.techprotech.agenda.compartido.whatsapp.*;
import java.time.*;
import java.util.Map;
import java.util.TimeZone;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Productor, reclamo SQL y consumidor reales; el transporte externo está simulado. */
@Isolated("Verifica UTC con JDBC y JVM México")
class OutboxUtcRepositorioTest {
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules().disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    final Instant instante=Instant.parse("2026-10-15T14:00:00Z");
    final LocalDateTime inicioCita=LocalDateTime.parse("2026-10-16T10:00");

    @ParameterizedTest @ValueSource(strings={"EMAIL","WHATSAPP"})
    void programaReclamaYRegistraEnvioSoloAlLlegarElInstanteUtc(String canal) throws Exception {
        verificar(canal,false);
    }
    @ParameterizedTest @ValueSource(strings={"EMAIL","WHATSAPP"})
    void reintentoConservaUtcYNoSeReclamaAntesDeTiempo(String canal) throws Exception {
        verificar(canal,true);
    }

    private void verificar(String canal, boolean falla) throws Exception {
        var anterior=TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
        var registro=new StandardServiceRegistryBuilder().applySettings(TiempoPruebas.configuracionJpa()).applySettings(Map.of(
                "hibernate.connection.driver_class","org.h2.Driver",
                "hibernate.connection.url","jdbc:h2:mem:outbox_utc;MODE=MySQL",
                "hibernate.hbm2ddl.auto","create-drop")).build();
        try(var factory=new MetadataSources(registro).addAnnotatedClass(BandejaSalidaNotificacionEntidad.class)
                .buildMetadata().buildSessionFactory();var em=factory.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("SET TIME ZONE 'UTC'").executeUpdate();
            var repo=mock(BandejaSalidaNotificacionRepositorio.class);
            when(repo.save(any())).thenAnswer(i -> {
                BandejaSalidaNotificacionEntidad salida=i.getArgument(0);
                if(salida.getId()==null)em.persist(salida);
                em.flush();return salida;
            });
            var consultas=new BandejaSalidaNotificacionRepositorioImpl();
            ReflectionTestUtils.setField(consultas,"entityManager",em);
            lenient().when(repo.reclamarPendientesEmail(anyInt(),any())).thenAnswer(i ->
                    consultas.reclamarPendientesEmail(i.getArgument(0),i.getArgument(1)));
            lenient().when(repo.reclamarPendientesPorCanal(anyString(),anyInt(),any())).thenAnswer(i ->
                    consultas.reclamarPendientesPorCanal(i.getArgument(0),i.getArgument(1),i.getArgument(2)));
            var correo=mock(ServicioCorreoEventosCita.class);
            var whatsapp=mock(ServicioMensajesWhatsappCitas.class);
            if(falla) {
                if(canal.equals("EMAIL")) doThrow(new IllegalStateException("Fallo simulado")).when(correo).enviar(anyLong(),anyString(),any());
                else when(whatsapp.enviarNotificacion(anyLong(),anyString(),any())).thenThrow(new IllegalStateException("Fallo simulado"));
            } else if(canal.equals("WHATSAPP")) {
                when(whatsapp.enviarNotificacion(anyLong(),anyString(),any())).thenReturn(new ResultadoEnvioWhatsapp("prueba","sent",null,null));
            }
            programar(repo,canal);
            var salida=em.createQuery("select b from BandejaSalidaNotificacionEntidad b",BandejaSalidaNotificacionEntidad.class).getSingleResult();
            assertThat(salida.getProgramadaEn()).isEqualTo(LocalDateTime.parse("2026-10-15T14:00"));
            assertThat(em.createNativeQuery("select hour(programada_en) from bandeja_salida_notificacion").getSingleResult()).isEqualTo(14);
            consumir(repo,canal,instante.minusSeconds(1),correo,whatsapp);
            verifyNoInteractions(correo,whatsapp);
            assertThat(salida.getEstado()).isEqualTo("PENDIENTE");
            consumir(repo,canal,instante,correo,whatsapp);
            if(falla) {
                assertThat(salida.getEstado()).isEqualTo("PENDIENTE");
                assertThat(salida.getProgramadaEn()).isEqualTo(LocalDateTime.parse("2026-10-15T14:01"));
                clearInvocations(correo,whatsapp);
                consumir(repo,canal,instante.plusSeconds(59),correo,whatsapp);
                verifyNoInteractions(correo,whatsapp);
            } else {
                assertThat(salida.getEstado()).isEqualTo("ENVIADA");
                assertThat(salida.getEnviadaEn()).isEqualTo(LocalDateTime.parse("2026-10-15T14:00"));
                if(canal.equals("WHATSAPP")) assertThat(salida.getEstadoEntregaActualizadoEn()).isEqualTo(salida.getEnviadaEn());
            }
            assertThat(salida.getEmpresaId()).isEqualTo(7L);
            assertThat(salida.getIntentos()).isEqualTo(1);
            assertThat(salida.getPayloadJson()).contains("2026-10-16T10:00");
            em.getTransaction().rollback();
        } finally {StandardServiceRegistryBuilder.destroy(registro);TimeZone.setDefault(anterior);}
    }

    private void programar(BandejaSalidaNotificacionRepositorio repo,String canal) {
        var reloj=Clock.fixed(instante,ZoneOffset.UTC);
        if(canal.equals("EMAIL")) {
            var config=mock(ServicioConfiguracionCorreoEmpresa.class);
            when(config.resolver(7L)).thenReturn(new ConfiguracionCorreoResolvida(7L,true,ProveedorCorreo.SMTP,
                    null,null,null,null,0,null,null,false,false,null,null,null,null,null,null,null));
            new ServicioOutboxCorreoCitas(repo,config,mapper,reloj).programarConfirmada(7L,9L,inicioCita);
        } else {
            var cliente=mock(ClienteWhatsappTwilio.class);when(cliente.estaHabilitado(7L)).thenReturn(true);
            new ServicioOutboxWhatsappCitas(repo,cliente,mapper,reloj).programarCitaConfirmada(7L,9L,"2220000000",inicioCita);
        }
    }

    private void consumir(BandejaSalidaNotificacionRepositorio repo,String canal,Instant ahora,
            ServicioCorreoEventosCita correo,ServicioMensajesWhatsappCitas whatsapp) {
        var reloj=Clock.fixed(ahora,ZoneOffset.UTC);
        if(canal.equals("EMAIL")) {
            var propiedades=new PropiedadesCorreo(true,null,null,null,null,null,null,null,null,null,null,null,20,60);
            new ProcesadorOutboxCorreo(repo,null,correo,null,null,null,null,null,propiedades,mapper,reloj).procesarPendientes();
        } else {
            var propiedades=new PropiedadesWhatsapp(true,false,null,null,null,null,null,null,null,
                    null,null,null,null,null,null,null,null,null,null,null,null,null,null,20,60,24,5,2);
            new ProcesadorOutboxWhatsapp(repo,whatsapp,propiedades,mapper,reloj).procesarPendientes();
        }
    }
}
