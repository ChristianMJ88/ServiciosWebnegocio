package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.caja.api.dto.*;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.aplicacion.*;
import com.techprotech.agenda.modulos.pagos.aplicacion.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.politicas.aplicacion.ServicioPoliticaEmpresa;
import com.techprotech.agenda.modulos.productos.aplicacion.*;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.web.server.ResponseStatusException;
import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/** Cada ejecución usa un esquema nuevo; nunca crea ni modifica registros del negocio. */
@EnabledIfEnvironmentVariable(named="FLUORA_REGRESION_MYSQL_URL",matches=".+")
class CobrosConcurrentesMysqlTest {
    interface Caso { void ejecutar(org.hibernate.SessionFactory sf) throws Exception; }
    void aislado(Caso caso) throws Exception {
        String db="fluora_009_test_"+UUID.randomUUID().toString().replace("-","");
        String base=System.getenv("FLUORA_REGRESION_MYSQL_URL"),user=System.getenv("FLUORA_REGRESION_MYSQL_USER"),password=System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD");
        try(var c=DriverManager.getConnection(base,user,password);var s=c.createStatement()) {
            s.execute("CREATE DATABASE `"+db+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            var registry=new StandardServiceRegistryBuilder().applySettings(Map.of(
                "hibernate.connection.url",base.replaceFirst("/[^/?]+(?=\\?|$)","/"+db),"hibernate.connection.username",user,"hibernate.connection.password",password,
                "hibernate.hbm2ddl.auto","create-drop","hibernate.connection.isolation",2,"hibernate.connection.pool_size",8)).build();
            try(var sf=new MetadataSources(registry).addAnnotatedClass(CitaEntidad.class).addAnnotatedClass(PagoCitaEntidad.class)
                    .addAnnotatedClass(VentaEntidad.class).addAnnotatedClass(VentaDetalleEntidad.class).addAnnotatedClass(PagoVentaEntidad.class).buildMetadata().buildSessionFactory()) {
                try(var em=sf.createEntityManager()){assertThat(em.createNativeQuery("SELECT DATABASE()",String.class).getSingleResult()).isEqualTo(db);}
                try(var em=sf.createEntityManager()){em.getTransaction().begin();em.createNativeQuery("CREATE INDEX idx_pago_cita_empresa_cita ON pago_cita(empresa_id,cita_id,registrado_en)").executeUpdate();em.createNativeQuery("CREATE INDEX idx_venta_detalle_cita ON venta_detalle(empresa_id,cita_id)").executeUpdate();em.getTransaction().commit();}
                caso.ejecutar(sf);
            } finally {StandardServiceRegistryBuilder.destroy(registry);s.execute("DROP DATABASE `"+db+"`");}
        }
    }
    @Test void ventaYPagoDirectoCompitenYSoloUnoCobraAunqueAmbosAbrieronInstantanea() throws Exception {
        competir(false,true);
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void citasDistintasSinPagosNoTomanHuecosDePago(boolean primeraVenta) throws Exception { competir(true,primeraVenta); }
    void competir(boolean distintas,boolean primeraVenta) throws Exception {
        aislado(sf->{
            Long id,otro;try(var em=sf.createEntityManager()){em.getTransaction().begin();var c=SaldoCitaRepositorioTest.cita(em,7L);id=c.getId();otro=SaldoCitaRepositorioTest.cita(em,7L).getId();em.getTransaction().commit();}
            var saldosLeidos=new CountDownLatch(2);var listos=new CountDownLatch(2);var iniciar=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
            try {
                var futures=new ArrayList<Future<Integer>>();
                for(int indice=0;indice<2;indice++) {boolean venta=indice==0 && primeraVenta;Long objetivo=distintas && indice==1?otro:id;futures.add(pool.submit(()->{
                    try(var em=sf.createEntityManager()) {
                        em.getTransaction().begin();em.createQuery("select count(p) from PagoCitaEntidad p").getSingleResult();listos.countDown();assertThat(iniciar.await(20,TimeUnit.SECONDS)).isTrue();
                        var repos=new JpaRepositoryFactory(em);var citas=repos.getRepository(CitaRepositorio.class);var pagos=repos.getRepository(PagoCitaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);
                        var saldos=spy(new ServicioSaldoCitas(citas,pagos,detalles));var sesiones=sesiones();
                        if(distintas) doAnswer(i->{var r=i.callRealMethod();saldosLeidos.countDown();assertThat(saldosLeidos.await(20,TimeUnit.SECONDS)).isTrue();return r;}).when(saldos).calcularBajoBloqueo(any(),any());
                        try {
                            if(venta) ventas(em,citas,pagos,detalles,saldos,sesiones).registrar(7L,11L,List.of(3L),new RegistrarVentaRequest(3L,objetivo,null,List.of(),"TARJETA",null,null));
                            else new ServicioCaja(sesiones,pagos,mock(PagoVentaRepositorio.class),mock(MovimientoCajaRepositorio.class),citas,mock(ClienteRepositorio.class),mock(ServicioRepositorio.class),mock(SucursalRepositorio.class),new ObjectMapper(),Clock.systemUTC(),saldos,mock(ServicioPendientesCitasCaja.class),mock(PublicadorPagosCitas.class)).registrarPago(7L,11L,List.of(3L),objetivo,new RegistrarPagoRequest(new BigDecimal("500"),"TARJETA",null,null));
                            em.getTransaction().commit();return 200;
                        } catch(ResponseStatusException e){em.getTransaction().rollback();return e.getStatusCode().value();}
                    }
                }));}
                assertThat(listos.await(20,TimeUnit.SECONDS)).isTrue();iniciar.countDown();
                var resultados=List.of(futures.get(0).get(30,TimeUnit.SECONDS),futures.get(1).get(30,TimeUnit.SECONDS));
                assertThat(resultados).containsExactlyInAnyOrder(200,distintas?200:409);
                try(var em=sf.createEntityManager()){var repos=new JpaRepositoryFactory(em);var saldo=new ServicioSaldoCitas(repos.getRepository(CitaRepositorio.class),repos.getRepository(PagoCitaRepositorio.class),repos.getRepository(VentaDetalleRepositorio.class)).saldos(7L,List.of(id)).get(id);assertThat(saldo.pagado()).isEqualByComparingTo("500");assertThat(saldo.pendiente()).isZero();if(distintas){var segundo=new ServicioSaldoCitas(repos.getRepository(CitaRepositorio.class),repos.getRepository(PagoCitaRepositorio.class),repos.getRepository(VentaDetalleRepositorio.class)).saldos(7L,List.of(otro)).get(otro);assertThat(segundo.pagado()).isEqualByComparingTo("500");assertThat(segundo.pendiente()).isZero();}}
            } finally {iniciar.countDown();pool.shutdownNow();}
        });
    }
    @Test void aceptacionLeeAnticipoConfirmadoDespuesDeAbrirInstantaneaYExcluyeOrdenActual() throws Exception {
        aislado(sf->{
            Long uno,dos;try(var em=sf.createEntityManager()){em.getTransaction().begin();uno=SaldoCitaRepositorioTest.cita(em,7L).getId();dos=SaldoCitaRepositorioTest.cita(em,7L).getId();em.getTransaction().commit();}
            try(var a=sf.createEntityManager()) {
                a.unwrap(org.hibernate.Session.class).doWork(c->c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ));
                a.getTransaction().begin();assertThat(a.createQuery("select count(p) from PagoCitaEntidad p",Long.class).getSingleResult()).isZero();
                try(var b=sf.createEntityManager()){b.getTransaction().begin();b.createNativeQuery("SELECT id FROM cita WHERE id=:id FOR UPDATE").setParameter("id",uno).getSingleResult();SaldoCitaRepositorioTest.pago(b,7L,uno,"100");var v=SaldoCitaRepositorioTest.venta(b,7L,"PAGADA");v.setCheckoutOrdenId(50L);SaldoCitaRepositorioTest.detalle(b,v,7L,dos,"SERVICIO","100");b.getTransaction().commit();}
                // Confirma que el SELECT normal sigue viendo la instantánea vieja.
                assertThat(a.createQuery("select count(p) from PagoCitaEntidad p",Long.class).getSingleResult()).isZero();
                var citas=new JpaRepositoryFactory(a).getRepository(CitaRepositorio.class);var detalles=mock(CheckoutOrdenDetalleRepositorio.class);var confirmacion=mock(ServicioConfirmacionCitasCheckout.class);
                var orden=new CheckoutOrdenEntidad();orden.setId(50L);orden.setEmpresaId(7L);orden.setPoliticaVersionId(91L);orden.setPoliticaAceptadaEn(LocalDateTime.parse("2026-10-17T16:00"));
                var d1=new CheckoutOrdenDetalleEntidad();d1.setTipoConcepto("SERVICIO");d1.setCitaId(uno);var d2=new CheckoutOrdenDetalleEntidad();d2.setTipoConcepto("SERVICIO");d2.setCitaId(dos);
                when(detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(7L,50L)).thenReturn(List.of(d1,d2));
                when(confirmacion.bloquearGrupoOrden(7L,50L)).thenAnswer(i->List.of(citas.bloquearPorIdYEmpresa(uno,7L).orElseThrow(),citas.bloquearPorIdYEmpresa(dos,7L).orElseThrow()));
                var servicio=new ServicioAceptacionPoliticaCheckout(mock(ServicioPoliticaEmpresa.class),mock(com.techprotech.agenda.modulos.politicas.infraestructura.repositorio.PoliticaEmpresaRepositorio.class),detalles,confirmacion,Clock.systemUTC(),citas);
                servicio.acreditarAceptacion(orden);a.flush();a.clear();
                assertThat(a.find(CitaEntidad.class,uno).getPoliticaVersionId()).isNull();assertThat(a.find(CitaEntidad.class,dos).getPoliticaVersionId()).isEqualTo(91L);
                a.getTransaction().rollback();
            }
        });
    }
    @Test void dosCitasMasProductoUnaVentaConTresLineasYSaldoCero() throws Exception {
        aislado(sf->{try(var em=sf.createEntityManager()) {
            em.getTransaction().begin();var uno=SaldoCitaRepositorioTest.cita(em,7L);var dos=SaldoCitaRepositorioTest.cita(em,7L);em.flush();
            var repos=new JpaRepositoryFactory(em);var citas=repos.getRepository(CitaRepositorio.class);var pagos=repos.getRepository(PagoCitaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);var saldo=new ServicioSaldoCitas(citas,pagos,detalles);
            var servicio=ventas(em,citas,pagos,detalles,saldo,sesiones());
            var productos=(ProductoRepositorio)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"productos");
            var producto=new com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad();producto.setId(40L);producto.setEmpresaId(7L);producto.setActivo(true);producto.setPrecioBase(new BigDecimal("50"));producto.setCosto(new BigDecimal("20"));producto.setMoneda("MXN");producto.setNombre("Producto de prueba");
            when(productos.findAllById(any())).thenReturn(List.of(producto));
            var request=new RegistrarVentaRequest(3L,null,null,List.of(new RegistrarVentaRequest.ProductoVentaRequest(40L,BigDecimal.ONE)),"TARJETA",null,null,null,List.of(dos.getId(),uno.getId(),uno.getId()));
            var venta=servicio.registrar(7L,11L,List.of(3L),request);em.flush();
            assertThat(venta.total()).isEqualByComparingTo("1050");assertThat(venta.citaId()).isNull();assertThat(venta.detalles()).hasSize(3);
            assertThat(venta.detalles().stream().filter(d->"SERVICIO".equals(d.tipoConcepto())).map(d->d.citaId()).toList()).containsExactly(uno.getId(),dos.getId());
            assertThat(saldo.saldos(7L,List.of(uno.getId(),dos.getId())).values()).allSatisfy(s->assertThat(s.pendiente()).isZero());
            var eventos=(PublicadorPagosCitas)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"eventosPago");
            verify(eventos).pagoEnVenta(eq(uno),any(),any(),argThat(m->m.compareTo(new BigDecimal("500"))==0),any(),eq(11L));
            verify(eventos).pagoEnVenta(eq(dos),any(),any(),argThat(m->m.compareTo(new BigDecimal("500"))==0),any(),eq(11L));
            em.getTransaction().rollback();
        }});
    }
    @Test void listaIncompatibleRechazaCompletaSinCrearVenta() throws Exception {
        aislado(sf->{for(String caso:List.of("cliente","sucursal","moneda","sin_checkin","pagada","tenant")) {
            try(var em=sf.createEntityManager()) {
                em.getTransaction().begin();var uno=SaldoCitaRepositorioTest.cita(em,7L);var dos=SaldoCitaRepositorioTest.cita(em,7L);
                switch(caso){case "cliente"->dos.setClienteId(99L);case "sucursal"->dos.setSucursalId(99L);case "moneda"->dos.setMoneda("USD");case "sin_checkin"->dos.setEstado("CONFIRMADA");case "pagada"->SaldoCitaRepositorioTest.pago(em,7L,dos.getId(),"500");case "tenant"->dos.setEmpresaId(8L);}
                em.flush();var repos=new JpaRepositoryFactory(em);var citas=repos.getRepository(CitaRepositorio.class);var pagos=repos.getRepository(PagoCitaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);
                var servicio=ventas(em,citas,pagos,detalles,new ServicioSaldoCitas(citas,pagos,detalles),sesiones());
                assertThatThrownBy(()->servicio.registrar(7L,11L,List.of(3L),new RegistrarVentaRequest(3L,null,null,List.of(),"TARJETA",null,null,null,List.of(uno.getId(),dos.getId())))).isInstanceOf(ResponseStatusException.class).extracting(e->((ResponseStatusException)e).getStatusCode().value()).isEqualTo(409);
                assertThat(em.createQuery("select count(v) from VentaEntidad v",Long.class).getSingleResult()).isZero();em.getTransaction().rollback();
            }
        }});
    }
    @Test void lasEntradasDeCobroUsanReadCommittedInclusoLaTransaccionExteriorPoint() throws Exception {
        for(var entry:List.of(new Object[]{ServicioCaja.class,"registrarPago"},new Object[]{ServicioVentasCaja.class,"registrar"},new Object[]{ServicioVentasCaja.class,"prepararPoint"},new Object[]{ServicioMercadoPagoPoint.class,"crearOrden"})) {
            var metodo=Arrays.stream(((Class<?>)entry[0]).getDeclaredMethods()).filter(m->m.getName().equals(entry[1])).findFirst().orElseThrow();
            assertThat(metodo.getAnnotation(org.springframework.transaction.annotation.Transactional.class).isolation()).isEqualTo(org.springframework.transaction.annotation.Isolation.READ_COMMITTED);
        }
    }
    @Test void camposLegadoYListaNoSeMezclan() {
        assertThatThrownBy(()->new RegistrarVentaRequest(3L,1L,null,List.of(),"TARJETA",null,null,null,List.of(2L)).citasNormalizadas()).isInstanceOf(ResponseStatusException.class);
        assertThat(new RegistrarVentaRequest(3L,1L,null,List.of(),"TARJETA",null,null).citasNormalizadas()).containsExactly(1L);
    }
    static CajaSesionRepositorio sesiones(){var r=mock(CajaSesionRepositorio.class);var s=new CajaSesionEntidad();s.setId(6L);s.setEmpresaId(7L);s.setSucursalId(3L);s.setEstado("ABIERTA");when(r.findByEmpresaIdAndSucursalIdAndEstado(7L,3L,"ABIERTA")).thenReturn(Optional.of(s));return r;}
    static ServicioVentasCaja ventas(EntityManager em,CitaRepositorio citas,PagoCitaRepositorio pagos,VentaDetalleRepositorio detalles,ServicioSaldoCitas saldos,CajaSesionRepositorio sesiones){
        var repos=new JpaRepositoryFactory(em);var sucursales=mock(SucursalRepositorio.class);var s=new SucursalEntidad();s.setId(3L);s.setEmpresaId(7L);s.setActiva(true);when(sucursales.findByIdAndEmpresaIdAndActivaTrue(3L,7L)).thenReturn(Optional.of(s));
        var servicios=mock(ServicioRepositorio.class);var servicio=new ServicioEntidad();servicio.setId(5L);servicio.setNombre("Servicio de prueba");when(servicios.findByIdAndEmpresaId(5L,7L)).thenReturn(Optional.of(servicio));
        return new ServicioVentasCaja(repos.getRepository(VentaRepositorio.class),detalles,repos.getRepository(PagoVentaRepositorio.class),sesiones,mock(MovimientoCajaRepositorio.class),pagos,citas,mock(ClienteRepositorio.class),servicios,mock(ProductoRepositorio.class),mock(ProductoExistenciaRepositorio.class),sucursales,mock(ServicioInventarioProductos.class),mock(ReglaDescuentoProductoRepositorio.class),mock(ZonaPrecioProductoRepositorio.class),mock(PrecioZonaProductoRepositorio.class),mock(ServicioComisionesDevengadas.class),Clock.systemUTC(),com.techprotech.agenda.compartido.tiempo.TiempoPruebas.zonaUtc(),mock(PublicadorPagosCitas.class),saldos);
    }
}
