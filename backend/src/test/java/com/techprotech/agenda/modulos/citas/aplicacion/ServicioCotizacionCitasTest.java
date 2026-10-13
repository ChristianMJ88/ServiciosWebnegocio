package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.citas.api.dto.CotizarCitasRequest.ItemCotizacion;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sitio.dominio.TipoDescuentoPromocion;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EmpresaCheckoutConfigRepositorio;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class ServicioCotizacionCitasTest {
    final SucursalRepositorio sucursales=mock(SucursalRepositorio.class);
    final ServicioRepositorio servicios=mock(ServicioRepositorio.class);
    final ServicioSucursalRepositorio asignaciones=mock(ServicioSucursalRepositorio.class);
    final EmpresaSitioPromocionRepositorio promociones=mock(EmpresaSitioPromocionRepositorio.class);
    final EmpresaSitioPromocionServicioRepositorio relaciones=mock(EmpresaSitioPromocionServicioRepositorio.class);
    final EmpresaSitioPromocionSucursalRepositorio ramas=mock(EmpresaSitioPromocionSucursalRepositorio.class);
    final EmpresaCheckoutConfigRepositorio checkout=mock(EmpresaCheckoutConfigRepositorio.class);
    final ServicioCotizacionCitas cotizador = new ServicioCotizacionCitas(sucursales,servicios,asignaciones,promociones,relaciones,ramas,checkout,
        Clock.fixed(Instant.parse("2026-10-13T02:00:00Z"),ZoneOffset.UTC));
    EmpresaSitioPromocionEntidad promocion;
    ServicioEntidad servicio;
    @BeforeEach void preparar() {
        var sucursal=new SucursalEntidad(); sucursal.setId(3L); sucursal.setEmpresaId(1L); sucursal.setZonaHoraria("America/Mexico_City");
        when(sucursales.findByIdAndEmpresaIdAndActivaTrue(3L,1L)).thenReturn(Optional.of(sucursal));
        servicio=new ServicioEntidad(); servicio.setId(10L);servicio.setEmpresaId(1L);servicio.setActivo(true);servicio.setMoneda("MXN");servicio.setPrecio(new BigDecimal("250"));servicio.setNombre("Servicio real");
        when(servicios.findByEmpresaIdAndIdIn(eq(1L),any())).thenReturn(List.of(servicio));
        var asignacion=new ServicioSucursalEntidad();asignacion.setId(new ServicioSucursalId(10L,3L));
        when(asignaciones.findByEmpresaIdAndIdSucursalIdAndActivoTrue(1L,3L)).thenReturn(List.of(asignacion));
        promocion=new EmpresaSitioPromocionEntidad();promocion.setId(8L);promocion.setEmpresaId(1L);promocion.setActiva(true);promocion.setTitulo("Oferta");promocion.setMoneda("MXN");
        promocion.setTipoDescuento(TipoDescuentoPromocion.PORCENTAJE);promocion.setValorDescuento(new BigDecimal("20"));
        promocion.setFechaInicio(LocalDate.of(2026,10,12));promocion.setFechaFin(LocalDate.of(2026,10,12));
        when(promociones.findByIdAndEmpresaId(8L,1L)).thenReturn(Optional.of(promocion));
        var relacion=new EmpresaSitioPromocionServicioEntidad();relacion.setServicioId(10L);
        when(relaciones.findByEmpresaIdAndPromocionId(1L,8L)).thenReturn(List.of(relacion));
    }
    private com.techprotech.agenda.modulos.citas.api.dto.ResultadoCotizacionCitas cotizar() {
        return cotizador.cotizar(1L,3L,8L,List.of(new ItemCotizacion(10L,OffsetDateTime.parse("2026-10-12T21:00:00-06:00"))));
    }
    private void precioNormal() { var r=cotizar();assertThat(r.total()).isEqualByComparingTo("250");assertThat(r.promocion()).isNull();assertThat(r.motivoNoAplicada()).isNotBlank(); }
    @Test void evaluaHoyYFechaEnZonaDeSucursalAunqueEnUtcYaEsOtroDia() { assertThat(cotizar().total()).isEqualByComparingTo("200"); }
    @Test void vencida() { promocion.setFechaFin(LocalDate.of(2026,10,11));precioNormal(); }
    @Test void futura() { promocion.setFechaInicio(LocalDate.of(2026,10,14));precioNormal(); }
    @Test void inactiva() { promocion.setActiva(false);precioNormal(); }
    @Test void eliminada() { promocion.setEliminadaEn(LocalDateTime.now());precioNormal(); }
    @Test void otroTenant() { when(promociones.findByIdAndEmpresaId(8L,1L)).thenReturn(Optional.empty());precioNormal();verify(promociones).findByIdAndEmpresaId(8L,1L); }
    @Test void otraSucursal() { var rama=new EmpresaSitioPromocionSucursalEntidad();rama.setSucursalId(99L);when(ramas.findByEmpresaIdAndPromocionId(1L,8L)).thenReturn(List.of(rama));precioNormal(); }
    @Test void distintaMoneda() { promocion.setMoneda("USD");precioNormal(); }
    @Test void servicioNoIncluido() { when(relaciones.findByEmpresaIdAndPromocionId(1L,8L)).thenReturn(List.of());precioNormal(); }
    @Test void fechaDeCitaFueraDeVigencia() {
        var r=cotizador.cotizar(1L,3L,8L,List.of(new ItemCotizacion(10L,OffsetDateTime.parse("2026-10-13T00:00:00-06:00"))));
        assertThat(r.total()).isEqualByComparingTo("250");assertThat(r.motivoNoAplicada()).contains("fecha");
    }
    @Test void cotizacionPreliminarRespetaDiaElegidoSinHorario() {
        var r=cotizador.cotizar(1L,3L,8L,List.of(new ItemCotizacion(10L,null,LocalDate.of(2026,10,13))));
        assertThat(r.promocion()).isNull();assertThat(r.total()).isEqualByComparingTo("250");
    }
    @Test void inicioRealTienePrioridadSobreFechaPreliminarDelFrontend() {
        var r=cotizador.cotizar(1L,3L,8L,List.of(new ItemCotizacion(10L,OffsetDateTime.parse("2026-10-13T00:00:00-06:00"),LocalDate.of(2026,10,12))));
        assertThat(r.promocion()).isNull();assertThat(r.total()).isEqualByComparingTo("250");
    }
    @Test void cotizaReservaMixtaSinDescontarServiciosNoIncluidos() {
        var otro=new ServicioEntidad();otro.setId(11L);otro.setActivo(true);otro.setMoneda("MXN");otro.setPrecio(new BigDecimal("100"));otro.setNombre("Otro servicio");
        when(servicios.findByEmpresaIdAndIdIn(eq(1L),any())).thenReturn(List.of(servicio,otro));
        var a=new ServicioSucursalEntidad();a.setId(new ServicioSucursalId(10L,3L));var b=new ServicioSucursalEntidad();b.setId(new ServicioSucursalId(11L,3L));
        when(asignaciones.findByEmpresaIdAndIdSucursalIdAndActivoTrue(1L,3L)).thenReturn(List.of(a,b));
        var r=cotizador.cotizar(1L,3L,8L,List.of(new ItemCotizacion(10L,null),new ItemCotizacion(11L,null)));
        assertThat(r.total()).isEqualByComparingTo("300");assertThat(r.lineas().get(1).promocionAplicada()).isFalse();
    }
    @Test void exigeServicioActivoDeLaSucursalYTenant() {
        servicio.setActivo(false);
        assertThatThrownBy(this::cotizar).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }@Test void crearPrecioNoConsultaAnticipoInvalido() {
    servicio.setRequiereAnticipo(true); servicio.setAnticipoValor(null);
    var r=cotizador.cotizarPrecio(1L,3L,null,List.of(new ItemCotizacion(10L,null)));
    assertThat(r.total()).isEqualByComparingTo("250");
    verifyNoInteractions(checkout);
}
@Test void previewNoBloqueaConCheckoutDeshabilitadoOAnticipoInvalido() {
    servicio.setRequiereAnticipo(true); servicio.setAnticipoValor(null);
    var config=new com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EmpresaCheckoutConfigEntidad();
    when(checkout.findById(1L)).thenReturn(Optional.of(config));
    assertThat(cotizar().anticipoTotal()).isEqualByComparingTo("200");
    config.setHabilitado(true); config.setPermitirPagoServicios(true);
    assertThat(cotizar().anticipoTotal()).isEqualByComparingTo("200");
    servicio.setAnticipoTipo("PORCENTAJE"); servicio.setAnticipoValor(new BigDecimal("50"));
    assertThat(cotizar().anticipoTotal()).isEqualByComparingTo("100");
    config.setPermitirPagoServicios(false);
    assertThat(cotizar().anticipoTotal()).isEqualByComparingTo("200");
}

}
