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
        String db="fluora_test_009_"+UUID.randomUUID().toString().replace("-","");
        String base=System.getenv("FLUORA_REGRESION_MYSQL_URL"),user=System.getenv("FLUORA_REGRESION_MYSQL_USER"),password=System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD");
        try(var c=DriverManager.getConnection(base,user,password);var s=c.createStatement()) {
            com.techprotech.agenda.soporte.MysqlAislado.comprobar(c);
            String hija=com.techprotech.agenda.soporte.MysqlAislado.urlHija(base,db);
            s.execute("CREATE DATABASE `"+db+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            try {
            try(var destino=DriverManager.getConnection(hija,user,password)) {
                com.techprotech.agenda.soporte.MysqlAislado.comprobar(destino);
                assertThat(destino.getCatalog()).isEqualTo(db);
            }
            var registry=new StandardServiceRegistryBuilder().applySettings(Map.of(
                "hibernate.connection.url",hija,"hibernate.connection.username",user,"hibernate.connection.password",password,
                "hibernate.hbm2ddl.auto","create-drop","hibernate.connection.handling_mode","DELAYED_ACQUISITION_AND_HOLD","hibernate.connection.isolation",2,"hibernate.connection.pool_size",8)).build();
            try(var sf=new MetadataSources(registry).addAnnotatedClass(CitaEntidad.class).addAnnotatedClass(PagoCitaEntidad.class)
                    .addAnnotatedClass(VentaEntidad.class).addAnnotatedClass(VentaDetalleEntidad.class).addAnnotatedClass(com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad.class).addAnnotatedClass(PagoVentaEntidad.class).addAnnotatedClass(com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoExistenciaEntidad.class).buildMetadata().buildSessionFactory()) {
                try(var em=sf.createEntityManager()){assertThat(em.createNativeQuery("SELECT DATABASE()",String.class).getSingleResult()).isEqualTo(db);}
                try(var em=sf.createEntityManager()){em.getTransaction().begin();em.createNativeQuery("CREATE INDEX idx_pago_cita_empresa_cita ON pago_cita(empresa_id,cita_id,registrado_en)").executeUpdate();em.createNativeQuery("CREATE INDEX idx_venta_detalle_cita ON venta_detalle(empresa_id,cita_id)").executeUpdate();em.getTransaction().commit();}
                caso.ejecutar(sf);
            } finally {StandardServiceRegistryBuilder.destroy(registry);}
            } finally {s.execute("DROP DATABASE `"+db+"`");}
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
                a.unwrap(org.hibernate.Session.class).doWork(c->c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED));
                a.getTransaction().begin();assertThat(a.createQuery("select count(p) from PagoCitaEntidad p",Long.class).getSingleResult()).isZero();
                try(var b=sf.createEntityManager()){b.getTransaction().begin();b.createNativeQuery("SELECT id FROM cita WHERE id=:id FOR UPDATE").setParameter("id",uno).getSingleResult();SaldoCitaRepositorioTest.pago(b,7L,uno,"100");var v=SaldoCitaRepositorioTest.venta(b,7L,"PAGADA");v.setCheckoutOrdenId(50L);SaldoCitaRepositorioTest.detalle(b,v,7L,dos,"SERVICIO","100");b.getTransaction().commit();}
                // RC ve el anticipo confirmado antes de adquirir el grupo.
                assertThat(a.createQuery("select count(p) from PagoCitaEntidad p",Long.class).getSingleResult()).isEqualTo(1);
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
        for(var entry:List.of(new Object[]{ServicioCaja.class,"registrarPago"},new Object[]{ServicioVentasCaja.class,"registrar"},new Object[]{ServicioVentasCaja.class,"prepararPoint"},new Object[]{ServicioEstadoPagoCheckout.class,"aplicar"})) {
            var metodo=Arrays.stream(((Class<?>)entry[0]).getDeclaredMethods()).filter(m->m.getName().equals(entry[1])).findFirst().orElseThrow();
            assertThat(metodo.getAnnotation(org.springframework.transaction.annotation.Transactional.class).isolation()).isEqualTo(org.springframework.transaction.annotation.Isolation.READ_COMMITTED);
        }
    }
    @Test void camposLegadoYListaNoSeMezclan() {
        assertThatThrownBy(()->new RegistrarVentaRequest(3L,1L,null,List.of(),"TARJETA",null,null,null,List.of(2L)).citasNormalizadas()).isInstanceOf(ResponseStatusException.class);
        assertThat(new RegistrarVentaRequest(3L,1L,null,List.of(),"TARJETA",null,null).citasNormalizadas()).containsExactly(1L);
    }
    @Test void httpPointNoRetieneTransaccionOtraCitaCobraYMismaCitaRecibe409() throws Exception {
        aislado(sf -> {
            Long uno,dos;try(var em=sf.createEntityManager()){em.getTransaction().begin();uno=SaldoCitaRepositorioTest.cita(em,7L).getId();dos=SaldoCitaRepositorioTest.cita(em,7L).getId();em.getTransaction().commit();}
            var gestor=new org.springframework.orm.jpa.JpaTransactionManager(sf);gestor.setJpaDialect(new org.springframework.orm.jpa.vendor.HibernateJpaDialect());
            var compartido=org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(sf);
            var repos=new JpaRepositoryFactory(compartido);var citas=repos.getRepository(CitaRepositorio.class);var pagos=repos.getRepository(PagoCitaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);
            var saldos=new ServicioSaldoCitas(citas,pagos,detalles);var ventaServicio=ventas(compartido,citas,pagos,detalles,saldos,sesiones());
            var terminales=mock(TerminalMercadoPagoRepositorio.class);var terminal=new TerminalMercadoPagoEntidad();terminal.setId(5L);terminal.setEmpresaId(7L);terminal.setSucursalId(3L);terminal.setActiva(true);terminal.setTerminalId("TERMINAL_TEST");
            when(terminales.bloquear(5L,7L)).thenReturn(Optional.of(terminal));when(terminales.findByIdAndEmpresaId(5L,7L)).thenReturn(Optional.of(terminal));
            var cliente=mock(com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ClienteMercadoPagoPoint.class);
            var enHttp=new CountDownLatch(1);var continuar=new CountDownLatch(1);
            when(cliente.crearOrden(any(),any(),any(),any(),any(),any(),any())).thenAnswer(i -> {
                assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                enHttp.countDown();assertThat(continuar.await(20,TimeUnit.SECONDS)).isTrue();
                return new com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ClienteMercadoPagoPoint.OrdenRemota("ORD_TEST","created",null,null,null,null);
            });
            var comisiones=mock(ServicioComisionPlataforma.class);when(comisiones.calcular(anyLong(),any(com.techprotech.agenda.modulos.pagos.dominio.ProveedorPago.class),any(),any())).thenReturn(BigDecimal.ZERO);
            var point=new ServicioMercadoPagoPoint(cliente,mock(ServicioOAuthMercadoPago.class),new com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.PropiedadesMercadoPago(true,"PRUEBA",null,null,null,null,null,null,null,null,null),terminales,repos.getRepository(OrdenPointCajaRepositorio.class),mock(SucursalRepositorio.class),repos.getRepository(VentaRepositorio.class),ventaServicio,mock(ServicioConciliacionPagos.class),comisiones,gestor,Clock.systemUTC(),Duration.ofMinutes(20),new PagosPrevistosPoint(new ObjectMapper()));
            var pool=Executors.newSingleThreadExecutor();
            try {
                var pendiente=pool.submit(() -> point.crearOrden(7L,11L,List.of(3L),"idempotencia-test",new com.techprotech.agenda.modulos.pagos.api.dto.CrearOrdenPointRequest(5L,new RegistrarVentaRequest(3L,uno,null,List.of(),"MERCADO_PAGO_POINT",null,null))));
                if(!enHttp.await(20,TimeUnit.SECONDS)){if(pendiente.isDone())pendiente.get();fail("Point no alcanzó HTTP");}
                var tx=new org.springframework.transaction.support.TransactionTemplate(gestor);tx.setIsolationLevel(2);tx.setTimeout(3);
                // Otra cita avanza aun cuando la llamada HTTP no ha terminado.
                tx.executeWithoutResult(t -> ventaServicio.registrar(7L,11L,List.of(3L),new RegistrarVentaRequest(3L,dos,null,List.of(),"TARJETA",null,null)));
                var caja=new ServicioCaja(sesiones(),pagos,mock(PagoVentaRepositorio.class),mock(MovimientoCajaRepositorio.class),citas,mock(ClienteRepositorio.class),mock(ServicioRepositorio.class),mock(SucursalRepositorio.class),new ObjectMapper(),Clock.systemUTC(),saldos,mock(ServicioPendientesCitasCaja.class),mock(PublicadorPagosCitas.class));
                assertThatThrownBy(() -> tx.executeWithoutResult(t -> caja.registrarPago(7L,11L,List.of(3L),uno,new RegistrarPagoRequest(new BigDecimal("500"),"TARJETA",null,null))))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Point en curso");
                tx.executeWithoutResult(t -> {assertThat(saldos.saldos(7L,List.of(uno)).get(uno).pendiente()).isEqualByComparingTo("500");});
                continuar.countDown();assertThat(pendiente.get(20,TimeUnit.SECONDS).estado()).isEqualTo("CREADO");
            } finally {continuar.countDown();pool.shutdownNow();}
        });
    }

    @Test void webhookYVentaMixtaMismoProductoYCitaNoInviertenBloqueos() throws Exception {
        aislado(sf -> {
            Long cita;try(var em=sf.createEntityManager()) {
                em.getTransaction().begin();cita=SaldoCitaRepositorioTest.cita(em,7L).getId();
                var stock=new com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoExistenciaEntidad();
                stock.setId(new com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoExistenciaId(7L,40L,3L));stock.setExistencia(new BigDecimal("10"));stock.setReservado(BigDecimal.ZERO);em.persist(stock);em.getTransaction().commit();
            }
            var gestor=new org.springframework.orm.jpa.JpaTransactionManager(sf);gestor.setJpaDialect(new org.springframework.orm.jpa.vendor.HibernateJpaDialect());var em=org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(sf);
            var repos=new JpaRepositoryFactory(em);var citas=repos.getRepository(CitaRepositorio.class);var pagos=repos.getRepository(PagoCitaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);
            var servicio=ventas(em,citas,pagos,detalles,new ServicioSaldoCitas(citas,pagos,detalles),sesiones());
            var productos=(ProductoRepositorio)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"productos");
            var producto=new com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad();producto.setId(40L);producto.setEmpresaId(7L);producto.setActivo(true);producto.setPrecioBase(new BigDecimal("50"));producto.setMoneda("MXN");producto.setNombre("Prueba");
            when(productos.findAllById(any())).thenReturn(List.of(producto));
            Runnable bloquearStock=() -> em.createNativeQuery("SELECT producto_id FROM producto_existencia WHERE empresa_id=7 AND producto_id=40 AND sucursal_id=3 FOR UPDATE").getSingleResult();
            var inventario=(ServicioInventarioProductos)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"inventario");
            var stockBloqueado=new CountDownLatch(1);var cajaIntentaStock=new CountDownLatch(1);
            doAnswer(i->{cajaIntentaStock.countDown();bloquearStock.run();return null;}).when(inventario).bloquearParaVenta(eq(7L),eq(3L),any());
            var intento=new IntentoPagoCheckoutEntidad();intento.setId(20L);intento.setEmpresaId(7L);intento.setCheckoutOrdenId(30L);
            var orden=new CheckoutOrdenEntidad();orden.setId(30L);orden.setEmpresaId(7L);orden.setPublicId("orden-prueba");orden.setEstado("PENDIENTE_PAGO");intento.setEstado("PROCESANDO");
            var intentos=mock(IntentoPagoCheckoutRepositorio.class);when(intentos.findById(20L)).thenReturn(Optional.of(intento));
            var ordenes=mock(CheckoutOrdenRepositorio.class);when(ordenes.bloquearPorEmpresaEId(7L,30L)).thenReturn(Optional.of(orden));
            var inventarioWeb=mock(ServicioInventarioCheckout.class);
            when(inventarioWeb.confirmarSiDisponible(7L,30L)).thenAnswer(i->{bloquearStock.run();stockBloqueado.countDown();assertThat(cajaIntentaStock.await(10,TimeUnit.SECONDS)).isTrue();return true;});
            var reservas=mock(ServicioConfirmacionCitasCheckout.class);
            when(reservas.confirmar(7L,30L)).thenAnswer(i->{citas.bloquearPorIdYEmpresa(cita,7L).orElseThrow();SaldoCitaRepositorioTest.pago(em,7L,cita,"500");return true;});
            var webhook=new ServicioEstadoPagoCheckout(intentos,ordenes,inventarioWeb,reservas,mock(ServicioConciliacionPagos.class),mock(EntityManager.class),Clock.systemUTC(),mock(ServicioAceptacionPoliticaCheckout.class));
            var resultado=new com.techprotech.agenda.modulos.pagos.dominio.ResultadoCobro(com.techprotech.agenda.modulos.pagos.dominio.ProveedorPago.MERCADO_PAGO,"ORD_TEST","PAY_TEST",com.techprotech.agenda.modulos.pagos.dominio.EstadoPago.APROBADO,null,null,null,java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
            var pool=Executors.newFixedThreadPool(2);
            try {
                var web=pool.submit(()->{var tx=new org.springframework.transaction.support.TransactionTemplate(gestor);tx.setIsolationLevel(2);return tx.execute(t->webhook.aplicar(20L,resultado));});
                if(!stockBloqueado.await(10,TimeUnit.SECONDS)){if(web.isDone())web.get();fail("Webhook no alcanzó el primer bloqueo");}
                var caja=pool.submit(()->{var tx=new org.springframework.transaction.support.TransactionTemplate(gestor);tx.setIsolationLevel(2);try {tx.execute(t->servicio.registrar(7L,11L,List.of(3L),new RegistrarVentaRequest(3L,cita,null,List.of(new RegistrarVentaRequest.ProductoVentaRequest(40L,BigDecimal.ONE)),"TARJETA",null,null)));return 200;} catch(ResponseStatusException ex){return ex.getStatusCode().value();}});
                assertThat(web.get(15,TimeUnit.SECONDS).estadoOrden()).isEqualTo("PAGADA");assertThat(caja.get(15,TimeUnit.SECONDS)).isEqualTo(409);
            } finally {cajaIntentaStock.countDown();pool.shutdownNow();}
        });
    }

    @Test void ordenFinalNoApartaNiStaffPorCobrarYPointMulticitaConfirmaUnEventoPorLinea() throws Exception {
        aislado(sf -> {try(var em=sf.createEntityManager()) {
            em.getTransaction().begin();var uno=SaldoCitaRepositorioTest.cita(em,7L);var dos=SaldoCitaRepositorioTest.cita(em,7L);em.flush();
            var repos=new JpaRepositoryFactory(em);var citas=repos.getRepository(CitaRepositorio.class);var pagos=repos.getRepository(PagoCitaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);var saldos=new ServicioSaldoCitas(citas,pagos,detalles);var servicio=ventas(em,citas,pagos,detalles,saldos,sesiones());
            var venta=servicio.prepararPoint(7L,11L,List.of(3L),new RegistrarVentaRequest(3L,null,null,List.of(),"MERCADO_PAGO_POINT",null,null,null,List.of(dos.getId(),uno.getId())));
            var orden=new OrdenPointCajaEntidad();orden.setPublicId(UUID.randomUUID().toString());orden.setEmpresaId(7L);orden.setVentaId(venta.id());orden.setTerminalId(5L);orden.setIdempotencia(UUID.randomUUID().toString());orden.setEstado("CREANDO");orden.setImporte(venta.total());orden.setMoneda("MXN");orden.setCreadoPorUsuarioId(11L);em.persist(orden);em.flush();
            assertThat(saldos.saldos(7L,List.of(uno.getId(),dos.getId())).values()).allSatisfy(s -> assertThat(s.cobroPointEnCurso()).isTrue());
            for(String estado:List.of("CANCELADO","RECHAZADO","EXPIRADO","APROBADO","REEMBOLSADO")) {
                orden.setEstado(estado);em.flush();assertThat(saldos.saldos(7L,List.of(uno.getId())).get(uno.getId()).cobroPointEnCurso()).isFalse();
            }
            orden.setEstado("CREADO");em.flush();var confirmada=servicio.confirmarPoint(7L,venta.id(),"PAGO_TEST");
            assertThat(confirmada.estado()).isEqualTo("PAGADA");assertThat(confirmada.detalles()).hasSize(2);
            assertThat(saldos.saldos(7L,List.of(uno.getId(),dos.getId())).values()).allSatisfy(s -> assertThat(s.pendiente()).isZero());
            var eventos=(PublicadorPagosCitas)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"eventosPago");
            verify(eventos).venta(any(),any(),argThat(f -> f.size()==2),eq(com.techprotech.agenda.modulos.citas.dominio.eventos.OrigenEventoCita.WEBHOOK),isNull());
            servicio.confirmarPoint(7L,venta.id(),"PAGO_TEST");assertThat(em.createQuery("select count(p) from PagoVentaEntidad p",Long.class).getSingleResult()).isEqualTo(1);
            var pagoVenta=repos.getRepository(PagoVentaRepositorio.class);var primero=pagoVenta.findByEmpresaIdAndVentaIdOrderByRegistradoEnDesc(7L,venta.id()).getFirst();primero.setMonto(new BigDecimal("300"));
            var segundo=new PagoVentaEntidad();segundo.setEmpresaId(7L);segundo.setVentaId(venta.id());segundo.setCajaSesionId(6L);segundo.setMetodoPago("TARJETA");segundo.setMonto(venta.total().subtract(primero.getMonto()));segundo.setReferencia("SEGUNDO_MISMO_INSTANTE");segundo.setRegistradoEn(primero.getRegistradoEn());segundo.setRegistradoPorUsuarioId(11L);em.persist(segundo);em.flush();
            for(var cita:List.of(uno,dos)) {var historia=pagoVenta.findPagosConciliadosPorCita(7L,cita.getId());assertThat(historia).hasSize(1);assertThat(historia.getFirst().metodoPago()).isEqualTo("MIXTO");assertThat(historia.getFirst().monto()).isEqualByComparingTo(cita.getPrecio());assertThat(historia.getFirst().referencia()).isEqualTo("SEGUNDO_MISMO_INSTANTE");}
            var pendiente=orden(em,venta.id(),"REQUIERE_REVISION","100");for(int i=0;i<6;i++)orden(em,venta.id(),"CANCELADO","100");em.flush();
            assertThat(repos.getRepository(OrdenPointCajaRepositorio.class).noFinales(7L,Set.of(5L))).extracting(OrdenPointCajaEntidad::getId).contains(pendiente.getId());
            em.getTransaction().rollback();
        }});
    }

    @Test void pagoDivididoRegistraCadaMetodoUnaSolaVentaMovimientoYComision() throws Exception {
        aislado(sf -> {try(var em=sf.createEntityManager()) {
            em.getTransaction().begin();var cita=SaldoCitaRepositorioTest.cita(em,7L);cita.setPrecio(new BigDecimal("700"));em.flush();
            var repos=new JpaRepositoryFactory(em);var citas=repos.getRepository(CitaRepositorio.class);var pagos=repos.getRepository(PagoCitaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);var servicio=ventas(em,citas,pagos,detalles,new ServicioSaldoCitas(citas,pagos,detalles),sesiones());
            var partes=List.of(PlanPagosVentaTest.parte("EFECTIVO","300","500"),PlanPagosVentaTest.parte("TARJETA","400",null));
            var respuesta=servicio.registrar(7L,11L,List.of(3L),new RegistrarVentaRequest(3L,cita.getId(),null,List.of(),null,null,null,null,null,partes));em.flush();
            assertThat(respuesta.metodoPago()).isEqualTo("MIXTO");assertThat(respuesta.pagos()).hasSize(2);
            var porMetodo=em.createQuery("select p.metodoPago,sum(p.monto) from PagoVentaEntidad p where p.empresaId=7 and p.cajaSesionId=6 group by p.metodoPago",Object[].class).getResultList();
            assertThat(porMetodo).hasSize(2);assertThat(porMetodo.stream().filter(f -> "EFECTIVO".equals(f[0])).findFirst().orElseThrow()[1]).isEqualTo(new BigDecimal("300.00"));
            assertThat(em.createQuery("select count(p) from PagoVentaEntidad p",Long.class).getSingleResult()).isEqualTo(2);
            var efectivo=respuesta.pagos().stream().filter(p -> "EFECTIVO".equals(p.metodoPago())).findFirst().orElseThrow();assertThat(efectivo.monto()).isEqualByComparingTo("300");assertThat(efectivo.montoRecibido()).isEqualByComparingTo("500");assertThat(efectivo.cambio()).isEqualByComparingTo("200");
            var movimiento=org.mockito.ArgumentCaptor.forClass(MovimientoCajaEntidad.class);verify((MovimientoCajaRepositorio)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"movimientos")).save(movimiento.capture());assertThat(movimiento.getValue().getMonto()).isEqualByComparingTo("300");
            verify((ServicioComisionesDevengadas)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"comisiones"),times(1)).registrar(any(),any());
            assertThat(new ServicioSaldoCitas(citas,pagos,detalles).saldos(7L,List.of(cita.getId())).get(cita.getId()).pendiente()).isZero();em.getTransaction().rollback();
        }});
    }

    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void pointDivididoSoloAcreditaAlAprobarYSuPlanEsInmutable(boolean aprueba) throws Exception {
        aislado(sf -> {try(var em=sf.createEntityManager()) {
            em.getTransaction().begin();var cita=SaldoCitaRepositorioTest.cita(em,7L);cita.setPrecio(new BigDecimal("700"));em.flush();
            var repos=new JpaRepositoryFactory(em);var citas=repos.getRepository(CitaRepositorio.class);var pagos=repos.getRepository(PagoCitaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);var saldos=new ServicioSaldoCitas(citas,pagos,detalles);var servicio=ventas(em,citas,pagos,detalles,saldos,sesiones());
            var partes=List.of(PlanPagosVentaTest.parte("EFECTIVO","300","500"),PlanPagosVentaTest.parte("MERCADO_PAGO_POINT",null,null));
            var solicitud=new RegistrarVentaRequest(3L,cita.getId(),null,List.of(),null,null,null,null,null,partes);
            var venta=servicio.prepararPoint(7L,11L,List.of(3L),solicitud);var plan=PlanPagosVenta.resolver(venta.total(),solicitud,true);var codec=new PagosPrevistosPoint(new ObjectMapper());String json=codec.escribir(plan);
            var orden=new OrdenPointCajaEntidad();orden.setEmpresaId(7L);orden.setPublicId(UUID.randomUUID().toString());orden.setVentaId(venta.id());orden.setTerminalId(5L);orden.setIdempotencia(UUID.randomUUID().toString());orden.setEstado("CREANDO");orden.setImporte(plan.importePoint());orden.setMoneda("MXN");orden.setCreadoPorUsuarioId(11L);orden.setPagosPrevistosJson(json);em.persist(orden);em.flush();
            assertThat(em.createQuery("select count(p) from PagoVentaEntidad p",Long.class).getSingleResult()).isZero();verifyNoInteractions((MovimientoCajaRepositorio)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"movimientos"));
            assertThatThrownBy(()->saldos.calcularBajoBloqueo(7L,List.of(cita))).hasMessageContaining("Point en curso");
            orden.setPagosPrevistosJson("{\"v\":2,\"partes\":[]}");em.flush();em.refresh(orden);assertThat(codec.leer(orden.getPagosPrevistosJson())).isEqualTo(codec.leer(json));
            if(aprueba) {
                orden.setEstado("APROBADO");var respuesta=servicio.confirmarPoint(7L,venta.id(),"PAGO_TEST",orden.getImporte(),codec.leer(orden.getPagosPrevistosJson()));em.flush();
                assertThat(respuesta.pagos()).hasSize(2);assertThat(respuesta.pagos().stream().filter(p->"MERCADO_PAGO_POINT".equals(p.metodoPago())).findFirst().orElseThrow().monto()).isEqualByComparingTo("400");
                assertThat(saldos.saldos(7L,List.of(cita.getId())).get(cita.getId()).pendiente()).isZero();
                servicio.revisarReembolsoPoint(7L,venta.id());em.flush();assertThat(em.find(VentaEntidad.class,venta.id()).getEstado()).isEqualTo("PAGADA");assertThat(saldos.saldos(7L,List.of(cita.getId())).get(cita.getId()).pendiente()).isZero();
                var movimiento=org.mockito.ArgumentCaptor.forClass(MovimientoCajaEntidad.class);verify((MovimientoCajaRepositorio)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"movimientos")).save(movimiento.capture());assertThat(movimiento.getValue().getMonto()).isEqualByComparingTo("300");
            } else {
                orden.setEstado("CANCELADO");servicio.cancelarPoint(7L,venta.id(),"Cancelación de prueba");em.flush();
                assertThat(em.createQuery("select count(p) from PagoVentaEntidad p",Long.class).getSingleResult()).isZero();verifyNoInteractions((MovimientoCajaRepositorio)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"movimientos"));assertThat(saldos.saldos(7L,List.of(cita.getId())).get(cita.getId()).cobroPointEnCurso()).isFalse();
            }
            em.getTransaction().rollback();
        }});
    }

    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void aprobacionTardiaNoAcreditaCuentaCanceladaNiOrdenStaffSustituida(boolean staff) throws Exception {
        aislado(sf -> {try(var em=sf.createEntityManager()) {
            em.getTransaction().begin();var cita=SaldoCitaRepositorioTest.cita(em,7L);cita.setPrecio(new BigDecimal("700"));em.flush();
            var repos=new JpaRepositoryFactory(em);var citas=repos.getRepository(CitaRepositorio.class);var pagos=repos.getRepository(PagoCitaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);var saldos=new ServicioSaldoCitas(citas,pagos,detalles);var servicio=ventas(em,citas,pagos,detalles,saldos,sesiones());
            var venta=servicio.prepararPoint(7L,11L,List.of(3L),new RegistrarVentaRequest(3L,cita.getId(),null,List.of(),"MERCADO_PAGO_POINT",null,null));
            var a=orden(em,venta.id(),"CANCELADO","400");var entidad=em.find(VentaEntidad.class,venta.id());if(staff)entidad.setCanal("STAFF");
            var producto=new VentaDetalleEntidad();producto.setEmpresaId(7L);producto.setVentaId(venta.id());producto.setTipoConcepto("PRODUCTO");producto.setProductoId(40L);producto.setNombre("Muestra incluida");producto.setCantidad(BigDecimal.ONE);producto.setPrecioUnitario(BigDecimal.ZERO);producto.setDescuento(BigDecimal.ZERO);producto.setImpuestos(BigDecimal.ZERO);producto.setTotal(BigDecimal.ZERO);em.persist(producto);em.flush();
            servicio.cancelarPoint(7L,venta.id(),"Cancelada de prueba");
            OrdenPointCajaEntidad b=null;
            if(staff){servicio.prepararPoint(7L,11L,List.of(3L),new RegistrarVentaRequest(3L,null,null,List.of(),"MERCADO_PAGO_POINT",null,null,venta.id()));b=orden(em,venta.id(),"CREANDO","700");}
            em.flush();a.setEstado("APROBADO");
            var resultado=servicio.confirmarPoint(a,"PAY_A",List.of(new PlanPagosVenta.Parte("EFECTIVO",new BigDecimal("300"),null,null,null)));
            assertThat(resultado.requiereRevision()).isTrue();assertThat(em.createQuery("select count(p) from PagoVentaEntidad p",Long.class).getSingleResult()).isZero();
            assertThat(entidad.getObservaciones()).contains("aprobación tardía");
            if(staff){assertThat(entidad.getEstado()).isEqualTo("PENDIENTE_PAGO");assertThat(b.getEstado()).isEqualTo("CREANDO");b.setEstado("APROBADO");assertThat(servicio.confirmarPoint(b,"PAY_B",List.of()).requiereRevision()).isFalse();em.flush();assertThat(em.createQuery("select count(p) from PagoVentaEntidad p",Long.class).getSingleResult()).isEqualTo(1);}
            else assertThat(entidad.getEstado()).isEqualTo("CANCELADA");
            a.setProveedorOrdenId("ORD_A");a.setProveedorPagoId("PAY_A");a.setEstado("REEMBOLSADO");
            assertThat(servicio.marcarReembolsadaPoint(a)).isFalse();em.flush();
            assertThat(entidad.getEstado()).isEqualTo(staff?"PAGADA":"CANCELADA");
            assertThat(em.createQuery("select count(p) from PagoVentaEntidad p",Long.class).getSingleResult()).isEqualTo(staff?1:0);
            if(staff){assertThat(saldos.saldos(7L,List.of(cita.getId())).get(cita.getId()).pendiente()).isZero();assertThat(em.createQuery("select sum(p.monto) from PagoVentaEntidad p where p.referencia='PAY_B'",BigDecimal.class).getSingleResult()).isEqualByComparingTo("700");}
            verify((ServicioInventarioProductos)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"inventario"),times(1)).reintegrarVenta(eq(7L),eq(11L),eq(3L),anyMap(),anyString(),anyString());
            em.getTransaction().rollback();
        }});
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void reembolsoLegitimoRevierteUnaVezAunquePagoIdAparezcaDespues(boolean aprobacionSinPagoId) throws Exception {
        aislado(sf -> {try(var em=sf.createEntityManager()) {
            em.getTransaction().begin();var cita=SaldoCitaRepositorioTest.cita(em,7L);cita.setPrecio(new BigDecimal("700"));em.flush();
            var repos=new JpaRepositoryFactory(em);var citas=repos.getRepository(CitaRepositorio.class);var pagos=repos.getRepository(PagoCitaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);var saldos=new ServicioSaldoCitas(citas,pagos,detalles);var servicio=ventas(em,citas,pagos,detalles,saldos,sesiones());
            var venta=servicio.prepararPoint(7L,11L,List.of(3L),new RegistrarVentaRequest(3L,cita.getId(),null,List.of(),"MERCADO_PAGO_POINT",null,null));
            var producto=new VentaDetalleEntidad();producto.setEmpresaId(7L);producto.setVentaId(venta.id());producto.setTipoConcepto("PRODUCTO");producto.setProductoId(40L);producto.setNombre("Muestra incluida");producto.setCantidad(BigDecimal.ONE);producto.setPrecioUnitario(BigDecimal.ZERO);producto.setDescuento(BigDecimal.ZERO);producto.setImpuestos(BigDecimal.ZERO);producto.setTotal(BigDecimal.ZERO);em.persist(producto);
            var orden=orden(em,venta.id(),"APROBADO","700");orden.setProveedorOrdenId("ORD_REEMBOLSO");em.flush();
            servicio.confirmarPoint(orden,aprobacionSinPagoId?"ORD_REEMBOLSO":"PAY_REEMBOLSO",List.of());orden.setProveedorPagoId("PAY_REEMBOLSO");orden.setEstado("REEMBOLSADO");
            assertThat(servicio.marcarReembolsadaPoint(orden)).isTrue();em.flush();assertThat(em.find(VentaEntidad.class,venta.id()).getEstado()).isEqualTo("REEMBOLSADA");assertThat(saldos.saldos(7L,List.of(cita.getId())).get(cita.getId()).pendiente()).isEqualByComparingTo("700");
            assertThat(servicio.marcarReembolsadaPoint(orden)).isTrue();verify((ServicioInventarioProductos)org.springframework.test.util.ReflectionTestUtils.getField(servicio,"inventario"),times(1)).reintegrarVenta(eq(7L),eq(11L),eq(3L),anyMap(),anyString(),anyString());em.getTransaction().rollback();
        }});
    }
    static OrdenPointCajaEntidad orden(EntityManager em,Long ventaId,String estado,String monto) {
        var orden=new OrdenPointCajaEntidad();orden.setEmpresaId(7L);orden.setPublicId(UUID.randomUUID().toString());orden.setVentaId(ventaId);orden.setTerminalId(5L);orden.setIdempotencia(UUID.randomUUID().toString());orden.setEstado(estado);orden.setImporte(new BigDecimal(monto));orden.setMoneda("MXN");orden.setCreadoPorUsuarioId(11L);em.persist(orden);return orden;
    }

    @Test void controlNegativoOrdenAntiguoCitaInventarioProduceInterbloqueo() throws Exception {
        aislado(sf -> {
            Long id;try(var em=sf.createEntityManager()){em.getTransaction().begin();id=SaldoCitaRepositorioTest.cita(em,7L).getId();var stock=new com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoExistenciaEntidad();stock.setId(new com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoExistenciaId(7L,40L,3L));stock.setExistencia(new BigDecimal("10"));stock.setReservado(BigDecimal.ZERO);em.persist(stock);em.getTransaction().commit();}
            var primeros=new CountDownLatch(2);var pool=Executors.newFixedThreadPool(2);
            try {
                var tareas=new ArrayList<Future<Integer>>();
                for(boolean inventarioPrimero:List.of(false,true))tareas.add(pool.submit(()->{
                    try(var em=sf.createEntityManager()) {
                        em.getTransaction().begin();
                        String stock="SELECT producto_id FROM producto_existencia WHERE empresa_id=7 AND producto_id=40 AND sucursal_id=3 FOR UPDATE";
                        String cita="SELECT id FROM cita WHERE empresa_id=7 AND id="+id+" FOR UPDATE";
                        try {
                            em.createNativeQuery(inventarioPrimero?stock:cita).getSingleResult();primeros.countDown();assertThat(primeros.await(10,TimeUnit.SECONDS)).isTrue();
                            em.createNativeQuery(inventarioPrimero?cita:stock).getSingleResult();em.getTransaction().commit();return 200;
                        }catch(Exception e){em.getTransaction().rollback();Throwable causa=e;while(causa!=null){if(causa instanceof SQLException sql)return sql.getErrorCode();causa=causa.getCause();}throw e;}
                    }
                }));
                assertThat(List.of(tareas.get(0).get(15,TimeUnit.SECONDS),tareas.get(1).get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,1213);
            }finally{pool.shutdownNow();}
        });
    }

    static CajaSesionRepositorio sesiones(){var r=mock(CajaSesionRepositorio.class);var s=new CajaSesionEntidad();s.setId(6L);s.setEmpresaId(7L);s.setSucursalId(3L);s.setEstado("ABIERTA");when(r.findByEmpresaIdAndSucursalIdAndEstado(7L,3L,"ABIERTA")).thenReturn(Optional.of(s));when(r.bloquearPorId(6L,7L)).thenReturn(Optional.of(s));when(r.findParaAperturaConBloqueo(7L,3L,"ABIERTA")).thenReturn(Optional.of(s));return r;}
    static ServicioVentasCaja ventas(EntityManager em,CitaRepositorio citas,PagoCitaRepositorio pagos,VentaDetalleRepositorio detalles,ServicioSaldoCitas saldos,CajaSesionRepositorio sesiones){
        var repos=new JpaRepositoryFactory(em);var sucursales=mock(SucursalRepositorio.class);var s=new SucursalEntidad();s.setId(3L);s.setEmpresaId(7L);s.setActiva(true);when(sucursales.findByIdAndEmpresaIdAndActivaTrue(3L,7L)).thenReturn(Optional.of(s));
        var servicios=mock(ServicioRepositorio.class);var servicio=new ServicioEntidad();servicio.setId(5L);servicio.setNombre("Servicio de prueba");when(servicios.findByIdAndEmpresaId(5L,7L)).thenReturn(Optional.of(servicio));
        return new ServicioVentasCaja(repos.getRepository(VentaRepositorio.class),detalles,repos.getRepository(PagoVentaRepositorio.class),sesiones,mock(MovimientoCajaRepositorio.class),pagos,citas,mock(ClienteRepositorio.class),servicios,mock(ProductoRepositorio.class),mock(ProductoExistenciaRepositorio.class),sucursales,mock(ServicioInventarioProductos.class),mock(ReglaDescuentoProductoRepositorio.class),mock(ZonaPrecioProductoRepositorio.class),mock(PrecioZonaProductoRepositorio.class),mock(ServicioComisionesDevengadas.class),Clock.systemUTC(),com.techprotech.agenda.compartido.tiempo.TiempoPruebas.zonaUtc(),mock(PublicadorPagosCitas.class),saldos);
    }
}
