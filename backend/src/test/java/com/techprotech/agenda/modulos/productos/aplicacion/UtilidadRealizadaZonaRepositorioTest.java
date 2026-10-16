package com.techprotech.agenda.modulos.productos.aplicacion;

import com.techprotech.agenda.compartido.tiempo.TiempoPruebas;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaDetalleEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.RentabilidadVentasRepositorio;
import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import java.util.TimeZone;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import static org.assertj.core.api.Assertions.assertThat;

@Isolated("Regresión de rango local sobre venta UTC con JVM México")
class UtilidadRealizadaZonaRepositorioTest {
    @Test void ventaDel14UtcPerteneceAlDia13LocalYSeAislaPorTenant() {
        var anterior=TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
        var registro=new StandardServiceRegistryBuilder().applySettings(TiempoPruebas.configuracionJpa()).applySettings(Map.of(
                "hibernate.connection.driver_class","org.h2.Driver",
                "hibernate.connection.url","jdbc:h2:mem:utilidad_zona;MODE=MySQL",
                "hibernate.hbm2ddl.auto","create-drop")).build();
        try(var factory=new MetadataSources(registro).addAnnotatedClass(VentaEntidad.class)
                .addAnnotatedClass(VentaDetalleEntidad.class).buildMetadata().buildSessionFactory();var em=factory.createEntityManager()) {
            em.getTransaction().begin();
            crearVenta(em,"2026-10-14T01:00",8L);
            crearVenta(em,"2026-10-13T05:59",9L);
            crearVenta(em,"2026-10-13T06:00",10L);
            crearVenta(em,"2026-10-14T05:59",11L);
            crearVenta(em,"2026-10-14T06:00",12L);
            em.flush();em.clear();
            var repo=new JpaRepositoryFactory(em).getRepository(RentabilidadVentasRepositorio.class);
            var servicio=new ServicioUtilidadRealizada(repo,TiempoPruebas.zona(ZoneId.of("America/Mexico_City")));
            var dia13=LocalDate.of(2026,10,13);var dia14=dia13.plusDays(1);
            assertThat(servicio.consultar(7L,dia13,dia13,3L,"",0).filas()).extracting(f -> f.productoId()).containsExactlyInAnyOrder(8L,10L,11L);
            assertThat(servicio.consultar(7L,dia14,dia14,3L,"",0).filas()).extracting(f -> f.productoId()).containsExactly(12L);
            assertThat(servicio.consultar(8L,dia13,dia13,3L,"",0).filas()).isEmpty();
            em.getTransaction().rollback();
        } finally { StandardServiceRegistryBuilder.destroy(registro);TimeZone.setDefault(anterior); }
    }
    private void crearVenta(jakarta.persistence.EntityManager em, String inicio, Long producto) {
            var venta=new VentaEntidad();venta.setEmpresaId(7L);venta.setSucursalId(3L);venta.setFolio("UTC-PRUEBA-"+producto);
            venta.setEstado("PAGADA");venta.setCanal("CAJA");venta.setMoneda("MXN");
            venta.setSubtotal(BigDecimal.TEN);venta.setTotal(BigDecimal.TEN);venta.setDescuentoTotal(BigDecimal.ZERO);venta.setImpuestosTotal(BigDecimal.ZERO);
            venta.setCreadaEn(LocalDateTime.parse(inicio));em.persist(venta);
            var detalle=new VentaDetalleEntidad();detalle.setEmpresaId(7L);detalle.setVentaId(venta.getId());
            detalle.setTipoConcepto("PRODUCTO");detalle.setProductoId(producto);detalle.setNombre("Producto");
            detalle.setCantidad(BigDecimal.ONE);detalle.setPrecioUnitario(BigDecimal.TEN);detalle.setTotal(BigDecimal.TEN);
            detalle.setDescuento(BigDecimal.ZERO);detalle.setImpuestos(BigDecimal.ZERO);detalle.setCostoUnitarioHistorico(BigDecimal.ONE);
            em.persist(detalle);
    }
}
