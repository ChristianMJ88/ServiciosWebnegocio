package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.compartido.tiempo.TiempoPruebas;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import jakarta.persistence.EntityManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarPagoRequest;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import java.time.Clock;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import static org.assertj.core.api.Assertions.*;

class SaldoCitaRepositorioTest {
    @Test void consultasRealesAislanCitaPagosVentaYDetalleYSoloAcreditanServiciosDeVentasPagadas() {
        var registro=new StandardServiceRegistryBuilder().applySettings(TiempoPruebas.configuracionJpa()).applySettings(Map.of(
                "hibernate.connection.driver_class","org.h2.Driver","hibernate.connection.url","jdbc:h2:mem:saldo_005;MODE=MySQL",
                "hibernate.hbm2ddl.auto","create-drop")).build();
        try(var factory=new MetadataSources(registro).addAnnotatedClass(CitaEntidad.class).addAnnotatedClass(PagoCitaEntidad.class)
                .addAnnotatedClass(VentaEntidad.class).addAnnotatedClass(VentaDetalleEntidad.class).addAnnotatedClass(com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad.class).buildMetadata().buildSessionFactory();var em=factory.createEntityManager()) {
            em.getTransaction().begin();
            var cita=cita(em,7L);var ajena=cita(em,8L);
            var venta=venta(em,7L,"PAGADA");var pendiente=venta(em,7L,"PENDIENTE");var ventaAjena=venta(em,8L,"PAGADA");
            detalle(em,venta,7L,cita.getId(),"SERVICIO","80");
            detalle(em,venta,7L,cita.getId(),"PRODUCTO","999");
            detalle(em,pendiente,7L,cita.getId(),"SERVICIO","999");
            detalle(em,ventaAjena,7L,cita.getId(),"SERVICIO","999"); // Join inconsistente, no debe acreditarse.
            detalle(em,venta,8L,cita.getId(),"SERVICIO","999");
            pago(em,7L,cita.getId(),"20");pago(em,8L,cita.getId(),"999");
            em.flush();em.clear();
            var repos=new JpaRepositoryFactory(em);var citas=repos.getRepository(CitaRepositorio.class);
            var pagos=repos.getRepository(PagoCitaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);
            var servicio=new ServicioSaldoCitas(citas,pagos,detalles);
            assertThat(detalles.sumarServiciosPagadosPorCita(7L,cita.getId())).isEqualByComparingTo("80");
            var lote=servicio.saldos(7L,List.of(cita.getId(),ajena.getId()));
            assertThat(lote).containsOnlyKeys(cita.getId());
            var saldo=lote.get(cita.getId());assertThat(saldo.pagadoCaja()).isEqualByComparingTo("20");
            assertThat(saldo.pagadoVenta()).isEqualByComparingTo("80");assertThat(saldo.pendiente()).isEqualByComparingTo("400");
            assertThat(saldo.estadoCobro()).isEqualTo(EstadoCobroCita.PARCIAL);
            // El resto pagado en Caja hace que la misma fuente devuelva PAGADA.
            var sesiones=mock(CajaSesionRepositorio.class);
            var sesion=new CajaSesionEntidad();sesion.setId(6L);sesion.setEmpresaId(7L);sesion.setSucursalId(3L);sesion.setEstado("ABIERTA");
            when(sesiones.findByEmpresaIdAndSucursalIdAndEstado(7L,3L,"ABIERTA")).thenReturn(Optional.of(sesion));
            var bus=mock(org.springframework.context.ApplicationEventPublisher.class);
            var caja=new ServicioCaja(sesiones, pagos, mock(PagoVentaRepositorio.class), mock(MovimientoCajaRepositorio.class), citas, mock(ClienteRepositorio.class), mock(ServicioRepositorio.class), mock(SucursalRepositorio.class), new ObjectMapper(), Clock.systemUTC(), servicio, mock(ServicioPendientesCitasCaja.class), new com.techprotech.agenda.modulos.citas.aplicacion.PublicadorPagosCitas(citas, new com.techprotech.agenda.modulos.citas.aplicacion.PublicadorEventosCita(bus, Clock.systemUTC())));
            assertThatThrownBy(() -> caja.registrarPago(7L,11L,List.of(3L),cita.getId(),new RegistrarPagoRequest(new BigDecimal("401"),"TARJETA",null,null)))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            caja.registrarPago(7L,11L,List.of(3L),cita.getId(),new RegistrarPagoRequest(new BigDecimal("400"),"TARJETA",null,null));em.flush();
            assertThatThrownBy(() -> caja.registrarPago(7L,11L,List.of(3L),cita.getId(),new RegistrarPagoRequest(BigDecimal.ONE,"TARJETA",null,null)))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            assertThat(servicio.saldos(7L,List.of(cita.getId())).get(cita.getId()).estadoCobro()).isEqualTo(EstadoCobroCita.PAGADA);
            assertThat(servicio.saldos(9L,List.of(cita.getId(),ajena.getId()))).isEmpty();
            verify(bus).publishEvent(argThat((Object e) -> e instanceof com.techprotech.agenda.modulos.citas.dominio.eventos.PagoCitaRegistrado p
                    && p.monto().compareTo(new BigDecimal("400"))==0 && p.datos().empresaId().equals(7L)));
            verifyNoMoreInteractions(bus);
            em.getTransaction().rollback();
        } finally {StandardServiceRegistryBuilder.destroy(registro);}
    }
    static CitaEntidad cita(EntityManager em,Long empresa) {
        var c=new CitaEntidad();c.setEmpresaId(empresa);c.setSucursalId(3L);c.setServicioId(5L);c.setPrestadorId(11L);c.setClienteId(20L);
        c.setEstado("FINALIZADA");c.setPrecio(new BigDecimal("500"));c.setMoneda("MXN");c.setCreadaPorUsuarioId(11L);
        c.setInicio(LocalDateTime.parse("2026-10-15T10:00"));c.setFin(c.getInicio().plusHours(1));em.persist(c);return c;
    }
    static VentaEntidad venta(EntityManager em,Long empresa,String estado) {
        var v=new VentaEntidad();v.setEmpresaId(empresa);v.setSucursalId(3L);v.setFolio(UUID.randomUUID().toString());v.setEstado(estado);v.setCanal("ONLINE");
        v.setMoneda("MXN");v.setSubtotal(BigDecimal.TEN);v.setTotal(BigDecimal.TEN);v.setDescuentoTotal(BigDecimal.ZERO);v.setImpuestosTotal(BigDecimal.ZERO);v.setCreadaEn(LocalDateTime.parse("2026-10-15T16:00"));em.persist(v);return v;
    }
    static void detalle(EntityManager em,VentaEntidad venta,Long empresa,Long cita,String tipo,String monto) {
        var d=new VentaDetalleEntidad();d.setEmpresaId(empresa);d.setVentaId(venta.getId());d.setCitaId(cita);d.setTipoConcepto(tipo);d.setNombre("Concepto");
        d.setCantidad(BigDecimal.ONE);d.setPrecioUnitario(new BigDecimal(monto));d.setTotal(new BigDecimal(monto));d.setDescuento(BigDecimal.ZERO);d.setImpuestos(BigDecimal.ZERO);em.persist(d);
    }
    static void pago(EntityManager em,Long empresa,Long cita,String monto) {
        var p=new PagoCitaEntidad();p.setEmpresaId(empresa);p.setCitaId(cita);p.setMonto(new BigDecimal(monto));p.setMetodoPago("EFECTIVO");p.setRegistradoPorUsuarioId(11L);p.setRegistradoEn(LocalDateTime.parse("2026-10-15T16:00"));em.persist(p);
    }
}
