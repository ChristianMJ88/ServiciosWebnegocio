package com.techprotech.agenda.modulos.caja.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.tiempo.TiempoPruebas;
import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
import com.techprotech.agenda.modulos.caja.infraestructura.config.ConfiguracionCaja;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PendientesCitasCajaTest {
    @Test void sqlAcotaVentanaEstadoTenantYSucursalYSigueMostrandoSaldoAlDiaSiguiente() {
        var registro=new StandardServiceRegistryBuilder().applySettings(TiempoPruebas.configuracionJpa()).applySettings(Map.of(
                "hibernate.connection.driver_class","org.h2.Driver","hibernate.connection.url","jdbc:h2:mem:pendientes_005;MODE=MySQL",
                "hibernate.hbm2ddl.auto","create-drop","hibernate.generate_statistics","true")).build();
        try(var factory=new MetadataSources(registro).addAnnotatedClass(CitaEntidad.class).addAnnotatedClass(PagoCitaEntidad.class)
                .addAnnotatedClass(VentaEntidad.class).addAnnotatedClass(VentaDetalleEntidad.class).addAnnotatedClass(com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad.class).buildMetadata().buildSessionFactory();var em=factory.createEntityManager()) {
            em.getTransaction().begin();
            var hoy=cita("2026-10-15T10:00",7L,3L,"FINALIZADA",false);em.persist(hoy);
            var ayer=cita("2026-10-14T10:00",7L,3L,"FINALIZADA",false);em.persist(ayer);
            var limite=cita("2026-10-13T00:00",7L,3L,"FINALIZADA",false);em.persist(limite);
            var fuera=cita("2026-10-12T23:59:59",7L,3L,"FINALIZADA",false);em.persist(fuera);
            var manana=cita("2026-10-16T00:00",7L,3L,"FINALIZADA",false);em.persist(manana);
            em.persist(cita("2026-10-15T10:00",8L,3L,"FINALIZADA",false));
            em.persist(cita("2026-10-15T10:00",7L,4L,"FINALIZADA",false));
            em.persist(cita("2026-10-15T12:00",7L,3L,"CONFIRMADA",false));
            em.persist(cita("2026-10-15T12:00",7L,3L,"NO_ASISTIO",true));
            var llegada=cita("2026-10-15T12:00",7L,3L,"CONFIRMADA",true);em.persist(llegada);
            var gratis=cita("2026-10-15T12:00",7L,3L,"FINALIZADA",false);gratis.setPrecio(BigDecimal.ZERO);em.persist(gratis);
            var pagada=cita("2026-10-14T12:00",7L,3L,"FINALIZADA",false);em.persist(pagada);
            SaldoCitaRepositorioTest.pago(em,7L,pagada.getId(),"500");
            var venta=SaldoCitaRepositorioTest.venta(em,7L,"PAGADA");
            SaldoCitaRepositorioTest.detalle(em,venta,7L,ayer.getId(),"SERVICIO","80");
            SaldoCitaRepositorioTest.pago(em,8L,limite.getId(),"999");
            var ajena=SaldoCitaRepositorioTest.venta(em,8L,"PAGADA");
            var sinPagar=SaldoCitaRepositorioTest.venta(em,7L,"PENDIENTE");
            SaldoCitaRepositorioTest.detalle(em,ajena,7L,limite.getId(),"SERVICIO","999");
            SaldoCitaRepositorioTest.detalle(em,venta,8L,limite.getId(),"SERVICIO","999");
            SaldoCitaRepositorioTest.detalle(em,venta,7L,limite.getId(),"PRODUCTO","999");
            SaldoCitaRepositorioTest.detalle(em,sinPagar,7L,limite.getId(),"SERVICIO","999");
            for(int i=0;i<30;i++) {
                var saldada=cita("2026-10-14T13:00",7L,3L,"FINALIZADA",false);em.persist(saldada);
                if(i%3==0) SaldoCitaRepositorioTest.pago(em,7L,saldada.getId(),"600");
                else if(i%3==1) SaldoCitaRepositorioTest.detalle(em,venta,7L,saldada.getId(),"SERVICIO","500");
                else {SaldoCitaRepositorioTest.pago(em,7L,saldada.getId(),"100");SaldoCitaRepositorioTest.detalle(em,venta,7L,saldada.getId(),"SERVICIO","400");}
            }
            em.flush();em.clear();
            var repos=new JpaRepositoryFactory(em);var citas=repos.getRepository(CitaRepositorio.class);
            var pagos=repos.getRepository(PagoCitaRepositorio.class);var ventas=repos.getRepository(VentaDetalleRepositorio.class);
            var sucursales=mock(SucursalRepositorio.class);var zonas=mock(ZonaNegocio.class);
            var sucursal=new SucursalEntidad();sucursal.setId(3L);sucursal.setEmpresaId(7L);sucursal.setNombre("Sucursal de prueba");
            when(sucursales.findByEmpresaIdOrderByNombreAsc(7L)).thenReturn(List.of(sucursal));
            when(sucursales.findAllById(any())).thenReturn(List.of(sucursal));
            when(zonas.de(7L,3L)).thenReturn(ZoneId.of("America/Mexico_City"));
            var config=new ConfiguracionCaja();config.setDiasPendientes(2);
            // En UTC ya es día 16, pero la sucursal sigue operando el 15.
            var reloj=Clock.fixed(Instant.parse("2026-10-16T02:00:00Z"),ZoneOffset.UTC);
            assertThat(citas.buscarCobrablesEnRango(7L,3L,LocalDateTime.parse("2026-10-14T00:00"),
                    LocalDateTime.parse("2026-10-15T00:00"),ServicioSaldoCitas.ESTADOS_COBRABLES))
                    .extracting(CitaEntidad::getId).containsExactly(ayer.getId());
            var consulta=new ServicioPendientesCitasCaja(citas,sucursales,zonas,reloj,config);
            var candidatos=consulta.consultar(7L,List.of(3L),3L).citas();
            assertThat(candidatos).extracting(CitaEntidad::getId).containsExactlyInAnyOrder(hoy.getId(),ayer.getId(),limite.getId(),llegada.getId());
            var caja=new ServicioCaja(mock(CajaSesionRepositorio.class), pagos, mock(PagoVentaRepositorio.class), mock(MovimientoCajaRepositorio.class), citas, mock(ClienteRepositorio.class), mock(ServicioRepositorio.class), sucursales, new ObjectMapper(), reloj, new ServicioSaldoCitas(citas,pagos,ventas), consulta, org.mockito.Mockito.mock(com.techprotech.agenda.modulos.citas.aplicacion.PublicadorPagosCitas.class));
            var resultado=caja.listarCitasPorCobrar(7L,List.of(3L),3L);
            assertThat(resultado).hasSize(4).allSatisfy(c -> assertThat(c.sucursalId()).isEqualTo(3L));
            assertThat(resultado.stream().filter(c -> c.citaId().equals(hoy.getId())).findFirst().orElseThrow().periodoCobro()).isEqualTo("HOY");
            assertThat(resultado.stream().filter(c -> c.citaId().equals(ayer.getId())).findFirst().orElseThrow().diasAtraso()).isEqualTo(1);
            assertThat(resultado.stream().filter(c -> c.citaId().equals(limite.getId())).findFirst().orElseThrow().diasAtraso()).isEqualTo(2);
            var parcial=resultado.stream().filter(c -> c.citaId().equals(ayer.getId())).findFirst().orElseThrow();
            assertThat(parcial.pendiente()).isEqualByComparingTo("420");
            assertThat(parcial.estadoCobro()).isEqualTo(EstadoCobroCita.PARCIAL);
            var json=new ObjectMapper().findAndRegisterModules().valueToTree(parcial);
            assertThat(json.get("estadoCobro").asText()).isEqualTo("PARCIAL");
            assertThat(json.get("estadoPago").asText()).isEqualTo("PARCIAL");
            assertThat(consulta.contar(7L,List.of(3L),3L)).isEqualTo(resultado.size());
            // Resumen ejecuta un único COUNT SQL: cero entidades/materialización de citas o pagos.
            em.clear();var estadisticas=factory.getStatistics();estadisticas.clear();
            assertThat(caja.resumen(7L,List.of(3L),3L).citasPendientesDeCobro()).isEqualTo(4);
            assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(1);
            assertThat(estadisticas.getEntityLoadCount()).isZero();
            var consultaManana=new ServicioPendientesCitasCaja(citas,sucursales,zonas,Clock.offset(reloj,Duration.ofDays(1)),config);
            var lote=consultaManana.consultar(7L,List.of(3L),3L);
            assertThat(lote.citas()).extracting(CitaEntidad::getId).contains(hoy.getId()).doesNotContain(limite.getId());
            assertThat(lote.diasAtraso(hoy)).isEqualTo(1);
            config.setDiasPendientes(0);
            assertThat(consulta.consultar(7L,List.of(3L),3L).citas()).allMatch(c -> c.getInicio().toLocalDate().equals(LocalDate.of(2026,10,15)));
            em.getTransaction().rollback();
        } finally {StandardServiceRegistryBuilder.destroy(registro);}
    }

    @Test void cadaSucursalTieneSuRangoYNoSeConsultaUnaSucursalFueraDelAlcance() {
        var citas=mock(CitaRepositorio.class);var sucursales=mock(SucursalRepositorio.class);var zonas=mock(ZonaNegocio.class);
        var a=new SucursalEntidad();a.setId(3L);var b=new SucursalEntidad();b.setId(4L);
        when(sucursales.findByEmpresaIdOrderByNombreAsc(7L)).thenReturn(List.of(a,b));
        when(zonas.de(7L,3L)).thenReturn(ZoneId.of("America/Mexico_City"));when(zonas.de(7L,4L)).thenReturn(ZoneId.of("Asia/Tokyo"));
        var config=new ConfiguracionCaja();config.setDiasPendientes(2);
        var servicio=new ServicioPendientesCitasCaja(citas,sucursales,zonas,Clock.fixed(Instant.parse("2026-10-16T02:00:00Z"),ZoneOffset.UTC),config);
        servicio.consultar(7L,List.of(),null);
        verify(citas).buscarCobrablesEnRango(7L,3L,LocalDateTime.parse("2026-10-13T00:00"),LocalDateTime.parse("2026-10-16T00:00"),ServicioSaldoCitas.ESTADOS_COBRABLES);
        verify(citas).buscarCobrablesEnRango(7L,4L,LocalDateTime.parse("2026-10-14T00:00"),LocalDateTime.parse("2026-10-17T00:00"),ServicioSaldoCitas.ESTADOS_COBRABLES);
        clearInvocations(citas,zonas);
        servicio.consultar(7L,List.of(3L),null);
        verify(citas).buscarCobrablesEnRango(eq(7L),eq(3L),any(),any(),any());verifyNoMoreInteractions(citas);
        verify(zonas,never()).de(7L,4L);
    }

    static CitaEntidad cita(String inicio,Long empresa,Long sucursal,String estado,boolean checkin) {
        var c=new CitaEntidad();c.setEmpresaId(empresa);c.setSucursalId(sucursal);c.setServicioId(5L);c.setPrestadorId(11L);c.setClienteId(20L);
        c.setEstado(estado);c.setPrecio(new BigDecimal("500"));c.setMoneda("MXN");c.setCreadaPorUsuarioId(11L);c.setInicio(LocalDateTime.parse(inicio));c.setFin(c.getInicio().plusHours(1));
        if(checkin)c.setCheckInEn(LocalDateTime.parse("2026-10-15T16:00"));return c;
    }
}
