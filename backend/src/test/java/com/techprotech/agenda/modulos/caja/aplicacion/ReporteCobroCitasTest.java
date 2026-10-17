package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.compartido.tiempo.*;
import com.techprotech.agenda.modulos.admin.aplicacion.ServicioReporteCobroCitas;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.ReporteCobroCitasRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReporteCobroCitasTest {
    @Test void separaFechaDeServicioCobroYDeudaHistoricaSinMezclarMonedasNiDuplicarVentaMixta() {
        var registro=new StandardServiceRegistryBuilder().applySettings(TiempoPruebas.configuracionJpa()).applySettings(Map.of(
                "hibernate.connection.driver_class","org.h2.Driver","hibernate.connection.url","jdbc:h2:mem:reportes_005;MODE=MySQL",
                "hibernate.hbm2ddl.auto","create-drop")).build();
        var anterior=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
        try(var factory=new MetadataSources(registro).addAnnotatedClass(CitaEntidad.class).addAnnotatedClass(PagoCitaEntidad.class)
                .addAnnotatedClass(VentaEntidad.class).addAnnotatedClass(VentaDetalleEntidad.class).addAnnotatedClass(PagoVentaEntidad.class)
                .buildMetadata().buildSessionFactory();var em=factory.createEntityManager()) {
            em.getTransaction().begin();
            var vieja=PendientesCitasCajaTest.cita("2026-04-01T10:00",7L,3L,"FINALIZADA",false);em.persist(vieja);
            var usd=PendientesCitasCajaTest.cita("2026-04-01T10:00",7L,3L,"FINALIZADA",false);usd.setMoneda("USD");em.persist(usd);
            var parcial=PendientesCitasCajaTest.cita("2026-10-15T10:00",7L,3L,"FINALIZADA",false);em.persist(parcial);
            SaldoCitaRepositorioTest.pago(em,7L,parcial.getId(),"100");
            em.createQuery("update PagoCitaEntidad p set p.registradoEn=:momento where p.citaId=:id")
                    .setParameter("momento",LocalDateTime.parse("2026-10-16T05:59:59")).setParameter("id",parcial.getId()).executeUpdate();
            var futura=PendientesCitasCajaTest.cita("2026-10-20T10:00",7L,3L,"CONFIRMADA",false);em.persist(futura);
            var anticipo=SaldoCitaRepositorioTest.venta(em,7L,"PAGADA");SaldoCitaRepositorioTest.detalle(em,anticipo,7L,futura.getId(),"SERVICIO","80");
            pagoVenta(em,anticipo,"80","2026-10-15T06:00");
            var mixta=PendientesCitasCajaTest.cita("2026-10-15T12:00",7L,3L,"FINALIZADA",false);em.persist(mixta);
            var venta=SaldoCitaRepositorioTest.venta(em,7L,"PAGADA");venta.setTotal(new BigDecimal("500"));
            SaldoCitaRepositorioTest.detalle(em,venta,7L,mixta.getId(),"SERVICIO","200");
            SaldoCitaRepositorioTest.detalle(em,venta,7L,mixta.getId(),"PRODUCTO","300");
            pagoVenta(em,venta,"250","2026-10-15T06:00");pagoVenta(em,venta,"250","2026-10-16T06:00");
            var exceso=PendientesCitasCajaTest.cita("2026-10-15T13:00",7L,3L,"FINALIZADA",false);em.persist(exceso);
            SaldoCitaRepositorioTest.pago(em,7L,exceso.getId(),"600");
            em.createQuery("update PagoCitaEntidad p set p.registradoEn=:momento where p.citaId=:id")
                    .setParameter("momento",LocalDateTime.parse("2026-10-15T05:59:59")).setParameter("id",exceso.getId()).executeUpdate();
            var noShow=PendientesCitasCajaTest.cita("2026-10-15T14:00",7L,3L,"NO_ASISTIO",false);em.persist(noShow);
            var anticipoNoShow=SaldoCitaRepositorioTest.venta(em,7L,"PAGADA");SaldoCitaRepositorioTest.detalle(em,anticipoNoShow,7L,noShow.getId(),"SERVICIO","100");pagoVenta(em,anticipoNoShow,"100","2026-10-15T16:00");
            var cancelada=PendientesCitasCajaTest.cita("2026-10-15T15:00",7L,3L,"CANCELADA",false);em.persist(cancelada);SaldoCitaRepositorioTest.pago(em,7L,cancelada.getId(),"50");
            var ajena=PendientesCitasCajaTest.cita("2026-10-15T16:00",8L,3L,"FINALIZADA",false);em.persist(ajena);SaldoCitaRepositorioTest.pago(em,8L,ajena.getId(),"999");
            var otraSucursal=PendientesCitasCajaTest.cita("2026-10-15T16:00",7L,4L,"FINALIZADA",false);em.persist(otraSucursal);SaldoCitaRepositorioTest.pago(em,7L,otraSucursal.getId(),"999");
            SaldoCitaRepositorioTest.pago(em,8L,parcial.getId(),"999"); // Relación inconsistente de otro tenant.
            em.flush();
            var ds=em.unwrap(org.hibernate.Session.class).doReturningWork(c -> new SingleConnectionDataSource(c,true));
            var repositorio=new ReporteCobroCitasRepositorio(new NamedParameterJdbcTemplate(ds));
            var sucursales=mock(SucursalRepositorio.class);var zonas=mock(ZonaNegocio.class);
            var a=new SucursalEntidad();a.setId(3L);var b=new SucursalEntidad();b.setId(4L);
            when(sucursales.findByEmpresaIdOrderByNombreAsc(7L)).thenReturn(List.of(a,b));
            when(zonas.de(7L,3L)).thenReturn(ZoneId.of("America/Mexico_City"));
            var servicio=new ServicioReporteCobroCitas(repositorio,sucursales,zonas);
            var usuario=new UsuarioAutenticado(null,11L,7L,"EMPRESA",List.of(),List.of("REPORTES_ADMIN_VER"),List.of(3L));
            var resultado=servicio.consultar(usuario,LocalDate.of(2026,10,15),LocalDate.of(2026,10,15));
            assertThat(resultado).hasSize(2);
            var mxn=resultado.stream().filter(r -> r.moneda().equals("MXN")).findFirst().orElseThrow();
            assertThat(mxn.ingresosDevengados()).isEqualByComparingTo("1500");
            assertThat(mxn.ingresosCobrados()).isEqualByComparingTo("430");
            assertThat(mxn.saldoPorCobrar()).isEqualByComparingTo("1200");
            assertThat(mxn.anticiposEnCustodia()).isNull();assertThat(mxn.custodiaDisponible()).isFalse();
            assertThat(resultado.stream().filter(r -> r.moneda().equals("USD")).findFirst().orElseThrow().saldoPorCobrar()).isEqualByComparingTo("500");
            verify(zonas,never()).de(7L,4L);
            var historico=servicio.consultar(usuario,null,null).stream().filter(r -> r.moneda().equals("MXN")).findFirst().orElseThrow();
            assertThat(historico.ingresosDevengados()).isEqualByComparingTo("2000");
            assertThat(historico.ingresosCobrados()).isEqualByComparingTo("1130");
            assertThat(historico.saldoPorCobrar()).isEqualByComparingTo("1200");
            em.getTransaction().rollback();
        } finally {StandardServiceRegistryBuilder.destroy(registro);TimeZone.setDefault(anterior);}
    }

    static void pagoVenta(jakarta.persistence.EntityManager em,VentaEntidad v,String monto,String momento) {
        var p=new PagoVentaEntidad();p.setEmpresaId(v.getEmpresaId());p.setVentaId(v.getId());p.setMonto(new BigDecimal(monto));
        p.setMetodoPago("TARJETA");p.setRegistradoEn(LocalDateTime.parse(momento));em.persist(p);
    }
}
