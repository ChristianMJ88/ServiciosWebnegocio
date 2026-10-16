package com.techprotech.agenda.modulos.caja.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisionesDevengadas;
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

class AnticipoIncidenciaCheckoutTest {
    @Test void proyectaIncidenciaOnlineYAcreditaAnticipoEnCajaSinDuplicarlo() {
        var registro=new StandardServiceRegistryBuilder().applySettings(Map.of("hibernate.connection.driver_class","org.h2.Driver",
            "hibernate.connection.url","jdbc:h2:mem:anticipo_incidencia;MODE=MySQL","hibernate.hbm2ddl.auto","create-drop")).build();
        try(var factory=new MetadataSources(registro).addAnnotatedClass(VentaEntidad.class).addAnnotatedClass(VentaDetalleEntidad.class)
                .buildMetadata().buildSessionFactory();var em=factory.createEntityManager()) {
            em.getTransaction().begin();
            var repos=new JpaRepositoryFactory(em);var ventas=repos.getRepository(VentaRepositorio.class);var detalles=repos.getRepository(VentaDetalleRepositorio.class);
            var ordenes=mock(CheckoutOrdenRepositorio.class);var origen=mock(CheckoutOrdenDetalleRepositorio.class);var intentos=mock(IntentoPagoCheckoutRepositorio.class);
            var orden=new CheckoutOrdenEntidad();orden.setId(30L);orden.setPublicId("incidente");orden.setEmpresaId(7L);orden.setSucursalId(3L);
            orden.setEstado("PAGADA_CON_INCIDENCIA");orden.setRequiereConciliacion(true);orden.setMotivoIncidencia("Horario ocupado");orden.setMoneda("MXN");
            orden.setSubtotal(new BigDecimal("100"));orden.setTotal(new BigDecimal("100"));orden.setDescuentoTotal(BigDecimal.ZERO);orden.setImpuestosTotal(BigDecimal.ZERO);
            var intento=new IntentoPagoCheckoutEntidad();intento.setId(5L);intento.setImporte(new BigDecimal("100"));
            var linea=new CheckoutOrdenDetalleEntidad();linea.setCitaId(10L);linea.setServicioId(4L);linea.setTipoConcepto("SERVICIO");linea.setNombre("Servicio");
            linea.setCantidad(BigDecimal.ONE);linea.setPrecioListaUnitario(new BigDecimal("100"));linea.setTotal(new BigDecimal("100"));linea.setDescuento(BigDecimal.ZERO);linea.setImpuestos(BigDecimal.ZERO);
            when(ordenes.bloquearPorEmpresaEId(7L,30L)).thenReturn(Optional.of(orden));
            when(intentos.findFirstByEmpresaIdAndCheckoutOrdenIdAndEstadoOrderByIdDesc(7L,30L,"APROBADO")).thenReturn(Optional.of(intento));
            when(origen.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(7L,30L)).thenReturn(List.of(linea));
            var proyeccion=new ServicioVentasCheckout(ordenes,origen,intentos,ventas,detalles,mock(PagoVentaRepositorio.class),mock(ServicioComisionesDevengadas.class), java.time.Clock.systemUTC());
            proyeccion.proyectar(7L,30L);proyeccion.proyectar(7L,30L);em.flush();
            assertThat(ventas.findAll()).singleElement().satisfies(v -> {assertThat(v.getCanal()).isEqualTo("ONLINE");assertThat(v.getObservaciones()).contains("incidencia");});
            assertThat(detalles.sumarServiciosPagadosPorCita(7L,10L)).isEqualByComparingTo("100");
            assertThat(detalles.sumarServiciosPagadosPorCita(8L,10L)).isEqualByComparingTo("0");
            var citas=mock(CitaRepositorio.class);var sucursales=mock(SucursalRepositorio.class);
            var sucursal=new SucursalEntidad();sucursal.setId(3L);sucursal.setNombre("Sucursal de prueba");sucursal.setZonaHoraria("America/Mexico_City");when(sucursales.findAllById(any())).thenReturn(List.of(sucursal));
            var cita=new CitaEntidad();cita.setId(10L);cita.setSucursalId(3L);cita.setEstado("FINALIZADA");cita.setServicioId(4L);cita.setClienteId(9L);
            cita.setInicio(LocalDateTime.now(ZoneId.of(sucursal.getZonaHoraria())));cita.setPrecio(new BigDecimal("500"));cita.setMoneda("MXN");
            when(citas.findByEmpresaIdAndEstadoInOrderByInicioDesc(eq(7L),any())).thenReturn(List.of(cita));
            var caja=new ServicioCaja(mock(CajaSesionRepositorio.class),mock(PagoCitaRepositorio.class),mock(PagoVentaRepositorio.class),detalles,
                mock(MovimientoCajaRepositorio.class),citas,mock(ClienteRepositorio.class),mock(ServicioRepositorio.class),sucursales,new ObjectMapper(), java.time.Clock.systemUTC());
            var cobrable=caja.listarCitasPorCobrar(7L,List.of(3L),3L).getFirst();
            assertThat(cobrable.pagado()).isEqualByComparingTo("100");assertThat(cobrable.pendiente()).isEqualByComparingTo("400");
            em.getTransaction().rollback();
        } finally {StandardServiceRegistryBuilder.destroy(registro);}
    }
}
