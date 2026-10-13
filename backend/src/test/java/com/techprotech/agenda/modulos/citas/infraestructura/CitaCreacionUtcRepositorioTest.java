package com.techprotech.agenda.modulos.citas.infraestructura;

import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import static org.assertj.core.api.Assertions.assertThat;

class CitaCreacionUtcRepositorioTest {
    @Test void releeElUtcEscritoExplicitamenteAunqueLaSesionSqlUseOtraZona() {
        var registro=new StandardServiceRegistryBuilder().applySettings(Map.of(
                "hibernate.connection.driver_class","org.h2.Driver",
                "hibernate.connection.url","jdbc:h2:mem:cita_utc;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "hibernate.hbm2ddl.auto","create-drop",
                "hibernate.jdbc.time_zone","UTC")).build();
        try (var factory=new MetadataSources(registro).addAnnotatedClass(CitaEntidad.class).buildMetadata().buildSessionFactory();
             var em=factory.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("SET TIME ZONE '-05:00'").executeUpdate();
            var repo=new SimpleJpaRepository<CitaEntidad,Long>(CitaEntidad.class,em);
            var cita=new CitaEntidad();cita.setEmpresaId(7L);cita.setSucursalId(3L);cita.setServicioId(8L);
            cita.setPrestadorId(9L);cita.setClienteId(10L);cita.setCreadaPorUsuarioId(10L);
            cita.setInicio(LocalDateTime.of(2026,10,13,10,0));cita.setFin(cita.getInicio().plusMinutes(30));
            cita.setEstado("PENDIENTE");cita.setPrecio(new BigDecimal("200"));cita.setMoneda("MXN");
            var utc=LocalDateTime.of(2026,10,13,15,45,12);cita.setCreadaEn(utc);
            repo.saveAndFlush(cita);Long id=cita.getId();em.clear();
            assertThat(repo.findById(id).orElseThrow().getCreadaEn()).isEqualTo(utc);
            assertThat(((Number)em.createNativeQuery("SELECT EXTRACT(HOUR FROM creada_en) FROM cita WHERE id=:id").setParameter("id",id).getSingleResult()).intValue()).isEqualTo(15);
            em.getTransaction().rollback();
        } finally { StandardServiceRegistryBuilder.destroy(registro); }
    }
}
