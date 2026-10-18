package com.techprotech.agenda.modulos.caja.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.tiempo.TiempoPruebas;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PagosConciliadosMomentoTest {
    @Test void consultaJpaYMapeoConservanUtcImporteTenantYOrdenDePagosMixtos() {
        var reloj=Clock.fixed(Instant.parse("2026-10-15T14:00:00Z"), ZoneOffset.UTC);
        var utc=LocalDateTime.now(reloj);
        var registro=new StandardServiceRegistryBuilder().applySettings(TiempoPruebas.configuracionJpa()).applySettings(Map.of(
                "hibernate.connection.driver_class","org.h2.Driver", "hibernate.connection.url","jdbc:h2:mem:pagos_conciliados_utc;MODE=MySQL",
                "hibernate.hbm2ddl.auto","create-drop")).build();
        try(var factory=new MetadataSources(registro).addAnnotatedClass(VentaEntidad.class).addAnnotatedClass(VentaDetalleEntidad.class).addAnnotatedClass(com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad.class)
                .addAnnotatedClass(PagoVentaEntidad.class).buildMetadata().buildSessionFactory();var em=factory.createEntityManager()) {
            em.getTransaction().begin();
            for(long empresa:List.of(7L,8L)) {
                var venta=new VentaEntidad();venta.setEmpresaId(empresa);venta.setSucursalId(3L);venta.setFolio("V-"+empresa);
                venta.setEstado("PAGADA");venta.setCanal("ONLINE");venta.setMoneda("MXN");venta.setSubtotal(BigDecimal.TEN);
                venta.setDescuentoTotal(BigDecimal.ZERO);venta.setImpuestosTotal(BigDecimal.ZERO);venta.setTotal(BigDecimal.TEN);venta.setCreadaEn(utc);
                em.persist(venta);
                var detalle=new VentaDetalleEntidad();detalle.setEmpresaId(empresa);detalle.setVentaId(venta.getId());detalle.setCitaId(9L);
                detalle.setTipoConcepto("SERVICIO");detalle.setNombre("Servicio");detalle.setCantidad(BigDecimal.ONE);
                detalle.setPrecioUnitario(BigDecimal.TEN);detalle.setDescuento(BigDecimal.ZERO);detalle.setImpuestos(BigDecimal.ZERO);detalle.setTotal(BigDecimal.TEN);
                em.persist(detalle);
                var pago=new PagoVentaEntidad();pago.setEmpresaId(empresa);pago.setVentaId(venta.getId());pago.setMonto(BigDecimal.TEN);
                pago.setMetodoPago("TARJETA");pago.setRegistradoEn(utc.plusHours(1));em.persist(pago);
            }
            em.flush();em.clear();
            var conciliados=new JpaRepositoryFactory(em).getRepository(PagoVentaRepositorio.class);
            assertThat(conciliados.findPagosConciliadosPorCita(7L,9L)).hasSize(1);
            assertThat(conciliados.findPagosConciliadosPorCita(99L,9L)).isEmpty();
            var cita=new CitaEntidad();cita.setId(9L);cita.setEmpresaId(7L);cita.setSucursalId(3L);
            var citas=mock(CitaRepositorio.class);when(citas.findByIdAndEmpresaId(9L,7L)).thenReturn(Optional.of(cita));
            var pagos=mock(PagoCitaRepositorio.class);
            var directo=new PagoCitaEntidad();directo.setId(40L);directo.setCitaId(9L);directo.setMonto(BigDecimal.ONE);directo.setRegistradoEn(utc);
            when(pagos.findByEmpresaIdAndCitaIdOrderByRegistradoEnDesc(7L,9L)).thenReturn(List.of(directo));
            var caja=new ServicioCaja(mock(CajaSesionRepositorio.class), pagos, conciliados, mock(MovimientoCajaRepositorio.class), citas, mock(ClienteRepositorio.class), mock(ServicioRepositorio.class), mock(SucursalRepositorio.class), new ObjectMapper(), reloj, mock(ServicioSaldoCitas.class), mock(ServicioPendientesCitasCaja.class), org.mockito.Mockito.mock(com.techprotech.agenda.modulos.citas.aplicacion.PublicadorPagosCitas.class));
            var respuesta=caja.listarPagosCita(7L,List.of(3L),9L);
            assertThat(respuesta).hasSize(2);
            assertThat(respuesta.getFirst().registradoEn()).isEqualTo(OffsetDateTime.parse("2026-10-15T15:00Z"));
            assertThat(respuesta.getFirst().monto()).isEqualByComparingTo(BigDecimal.TEN);
            assertThat(respuesta.getLast().registradoEn()).isEqualTo(OffsetDateTime.parse("2026-10-15T14:00Z"));
            assertThat(respuesta.getLast().id()).isEqualTo(40L);
            em.getTransaction().rollback();
        } finally { StandardServiceRegistryBuilder.destroy(registro); }
    }
}
