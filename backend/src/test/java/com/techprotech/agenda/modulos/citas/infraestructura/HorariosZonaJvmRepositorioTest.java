package com.techprotech.agenda.modulos.citas.infraestructura;

import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ReglaDisponibilidadEntidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.TimeZone;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import static org.assertj.core.api.Assertions.assertThat;

@Isolated("Cambia la zona predeterminada de la JVM y la restaura al finalizar")
class HorariosZonaJvmRepositorioTest {
    static Map<String,Object> configuracionJpa() {
        var yaml=new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
        yaml.setResources(new org.springframework.core.io.ClassPathResource("application.yml"));
        var propiedades=java.util.Objects.requireNonNull(yaml.getObject());
        String prefijo="spring.jpa.properties.";
        var configuracion=new java.util.HashMap<String,Object>();
        propiedades.forEach((clave,valor) -> {
            if (clave.toString().startsWith(prefijo)) configuracion.put(clave.toString().substring(prefijo.length()),valor);
        });
        assertThat(configuracion).doesNotContainKey("hibernate.jdbc.time_zone");
        return configuracion;
    }

    @Test void mantieneHorariosLocalesYCreacionUtcEnJvmMexico() {
        var anterior=TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
            var registro=new StandardServiceRegistryBuilder().applySettings(configuracionJpa()).applySettings(Map.of(
                    "hibernate.connection.driver_class","org.h2.Driver",
                    "hibernate.connection.url","jdbc:h2:mem:horarios_jvm;MODE=MySQL",
                    "hibernate.hbm2ddl.auto","create-drop")).build();
            try (var factory=new MetadataSources(registro).addAnnotatedClass(CitaEntidad.class)
                    .addAnnotatedClass(ReglaDisponibilidadEntidad.class).buildMetadata().buildSessionFactory();
                 var em=factory.createEntityManager()) {
                em.getTransaction().begin();
                em.createNativeQuery("INSERT INTO regla_disponibilidad (empresa_id,tipo_sujeto,sujeto_id,dia_semana,hora_inicio,hora_fin,intervalo_minutos) VALUES (7,'SUCURSAL',3,2,TIME '09:00:00',TIME '19:00:00',30)").executeUpdate();
                var regla=em.createQuery("select r from ReglaDisponibilidadEntidad r",ReglaDisponibilidadEntidad.class).getSingleResult();
                assertThat(regla.getHoraInicio()).isEqualTo(LocalTime.of(9,0));
                assertThat(regla.getHoraFin()).isEqualTo(LocalTime.of(19,0));
                var cita=new CitaEntidad();cita.setEmpresaId(7L);cita.setSucursalId(3L);cita.setServicioId(8L);
                cita.setPrestadorId(9L);cita.setClienteId(10L);cita.setCreadaPorUsuarioId(10L);
                var inicio=LocalDateTime.of(2026,10,13,10,0);cita.setInicio(inicio);cita.setFin(inicio.plusMinutes(30));
                cita.setEstado("PENDIENTE");cita.setPrecio(new BigDecimal("200"));cita.setMoneda("MXN");
                var utc=LocalDateTime.of(2026,10,13,16,45,12);cita.setCreadaEn(utc);
                var repo=new SimpleJpaRepository<CitaEntidad,Long>(CitaEntidad.class,em);
                repo.saveAndFlush(cita);var id=cita.getId();em.clear();
                var releida=repo.findById(id).orElseThrow();
                assertThat(releida.getInicio()).isEqualTo(inicio);
                assertThat(releida.getFin()).isEqualTo(inicio.plusMinutes(30));
                assertThat(releida.getCreadaEn()).isEqualTo(utc);
                var hora=(Number)em.createNativeQuery("SELECT EXTRACT(HOUR FROM inicio) FROM cita WHERE id=:id").setParameter("id",id).getSingleResult();
                assertThat(hora.intValue()).isEqualTo(10);
                em.getTransaction().rollback();
            } finally {StandardServiceRegistryBuilder.destroy(registro);}
        } finally {TimeZone.setDefault(anterior);}
        assertThat(TimeZone.getDefault()).isEqualTo(anterior);
    }
}
