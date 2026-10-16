package com.techprotech.agenda.modulos.citas.infraestructura;

import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ReglaDisponibilidadEntidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.TimeZone;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.assertThat;

/** Usa solo tablas TEMPORARY en una conexión; no altera datos ni tablas del negocio. */
@Isolated("Zona predeterminada de JVM temporal")
@EnabledIfEnvironmentVariable(named="FLUORA_REGRESION_MYSQL_URL",matches=".+")
class HorariosZonaJvmMysqlTest {
    @Test void mysqlConSesionUtcConservaReglaCitaLocalYAuditoriaUtc() throws Exception {
        var anterior=TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
            var url=System.getenv("FLUORA_REGRESION_MYSQL_URL");
            // Estas opciones son las que conserva la configuración de la aplicación.
            url+=(url.contains("?")?"&":"?")+"serverTimezone=UTC&forceConnectionTimeZoneToSession=true";
            var datasource=new SingleConnectionDataSource(url,System.getenv("FLUORA_REGRESION_MYSQL_USER"),System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD"),true);
            var proveedor=new DatasourceConnectionProviderImpl();proveedor.setDataSource(datasource);
            proveedor.configure(java.util.Map.of());
            var registro=new StandardServiceRegistryBuilder().applySettings(HorariosZonaJvmRepositorioTest.configuracionJpa())
                    .addService(ConnectionProvider.class,proveedor).build();
            try {
                var metadata=new MetadataSources(registro).addAnnotatedClass(CitaEntidad.class).addAnnotatedClass(ReglaDisponibilidadEntidad.class).addAnnotatedClass(com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionEntidad.class).buildMetadata();
                var ddl=new SchemaCreatorImpl(registro).generateCreationCommands(metadata,false);
                try(var conexion=datasource.getConnection();var statement=conexion.createStatement()) {
                    for (var sql:ddl) {
                        if (!sql.startsWith("create table ")) throw new IllegalStateException("Solo se admiten tablas temporales en esta prueba");
                        sql=sql.replaceFirst("create table ","create temporary table ");
                        // En las migraciones, creada_en es TIMESTAMP; inicio/fin son DATETIME.
                        sql=sql.replace("creada_en datetime(6)","creada_en timestamp(6)");
                        statement.execute(sql);
                    }
                }
                try(var factory=metadata.buildSessionFactory();var em=factory.createEntityManager()) {
                    em.getTransaction().begin();
                    assertThat(em.createNativeQuery("SELECT @@session.time_zone").getSingleResult().toString()).isIn("+00:00","UTC");
                    em.createNativeQuery("INSERT INTO regla_disponibilidad (empresa_id,tipo_sujeto,sujeto_id,dia_semana,hora_inicio,hora_fin,intervalo_minutos) VALUES (7,'SUCURSAL',3,2,'09:00:00','19:00:00',30)").executeUpdate();
                    var regla=em.createQuery("select r from ReglaDisponibilidadEntidad r",ReglaDisponibilidadEntidad.class).getSingleResult();
                    assertThat(regla.getHoraInicio()).isEqualTo(LocalTime.of(9,0));assertThat(regla.getHoraFin()).isEqualTo(LocalTime.of(19,0));
                    var cita=new CitaEntidad();cita.setEmpresaId(7L);cita.setSucursalId(3L);cita.setServicioId(8L);
                    cita.setPrestadorId(9L);cita.setClienteId(10L);cita.setCreadaPorUsuarioId(10L);
                    var inicio=LocalDateTime.of(2026,10,13,10,0);cita.setInicio(inicio);cita.setFin(inicio.plusMinutes(30));
                    cita.setEstado("PENDIENTE");cita.setPrecio(new BigDecimal("200"));cita.setMoneda("MXN");
                    var utc=LocalDateTime.of(2026,10,13,16,45,12);cita.setCreadaEn(utc);
                    em.persist(cita);em.flush();var id=cita.getId();em.clear();
                    var releida=em.find(CitaEntidad.class,id);
                    assertThat(releida.getInicio()).isEqualTo(inicio);assertThat(releida.getFin()).isEqualTo(inicio.plusMinutes(30));assertThat(releida.getCreadaEn()).isEqualTo(utc);
                    var horas=(Object[])em.createNativeQuery("SELECT HOUR(inicio),HOUR(creada_en) FROM cita WHERE id=:id").setParameter("id",id).getSingleResult();
                    assertThat(((Number)horas[0]).intValue()).isEqualTo(10);assertThat(((Number)horas[1]).intValue()).isEqualTo(16);
                    // Connector/J real, misma sesión UTC y JVM México: nunca reclamar antes del vencimiento.
                    var consultas=new com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionRepositorioImpl();
                    org.springframework.test.util.ReflectionTestUtils.setField(consultas,"entityManager",em);
                    for(var canal:java.util.List.of("EMAIL","WHATSAPP")) {
                        var salida=new com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionEntidad();
                        salida.setEmpresaId(7L);salida.setTipoAgregado("CITA");salida.setAgregadoId(id);
                        salida.setTipoEvento("CONFIRMADA");salida.setCanal(canal);salida.setPayloadJson("{}");
                        salida.setEstado("PENDIENTE");salida.setProgramadaEn(utc);em.persist(salida);em.flush();em.clear();
                        assertThat(consultas.reclamarPendientesPorCanal(canal,10,utc.minusSeconds(1))).isEmpty();
                        var reclamadas=consultas.reclamarPendientesPorCanal(canal,10,utc);
                        assertThat(reclamadas).hasSize(1);
                        assertThat(reclamadas.getFirst().getProgramadaEn()).isEqualTo(utc);
                        assertThat(reclamadas.getFirst().getEmpresaId()).isEqualTo(7L);
                        assertThat(reclamadas.getFirst().getEstado()).isEqualTo("PROCESANDO");
                        assertThat(consultas.reclamarPendientesPorCanal(canal,10,utc)).isEmpty();
                    }
                    em.getTransaction().rollback();
                }
            } finally {StandardServiceRegistryBuilder.destroy(registro);datasource.destroy();}
        } finally {TimeZone.setDefault(anterior);}
        assertThat(TimeZone.getDefault()).isEqualTo(anterior);
    }
}
