package com.techprotech.agenda.modulos.citas.aplicacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.citas.api.dto.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.*;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioRolesEmpresa;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import com.techprotech.agenda.compartido.correo.*;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ServicioCitasPromocionTest {
    @Mock ServicioConsultaDisponibilidad disponibilidad;
    @Mock SucursalRepositorio sucursales;
    @Mock ServicioRepositorio servicios;
    @Mock ServicioSucursalRepositorio asignaciones;
    @Mock PrestadorServicioRepositorio prestadores;
    @Mock AsignacionServicioPrestadorRepositorio ofertas;
    @Mock UsuarioRepositorio usuarios;
    @Mock ClienteRepositorio clientes;
    @Mock EmpresaRepositorio empresas;
    @Mock ServicioRolesEmpresa roles;
    @Mock CitaRepositorio citas;
    @Mock HistorialEstadoCitaRepositorio historial;
    @Mock PasswordEncoder encoder;
    @Mock ServicioCorreoCitas correo;
    @Mock ServicioOutboxCorreoCitas outbox;
    @Mock ServicioOutboxWhatsappCitas whatsapp;
    @Mock ServicioCapacidadesEmpresa capacidades;
    @Mock ServicioCotizacionCitas cotizador;
    @Spy ObjectMapper mapper=new ObjectMapper();
    @InjectMocks ServicioCitas servicio;
    final OffsetDateTime inicio=OffsetDateTime.parse("2026-12-01T10:00:00-06:00");
    @BeforeEach void preparar() {
        when(empresas.findById(1L)).thenReturn(Optional.of(new EmpresaEntidad()));
        var sucursal=new SucursalEntidad();sucursal.setZonaHoraria("America/Mexico_City");
        when(sucursales.findByIdAndEmpresaIdAndActivaTrue(3L,1L)).thenReturn(Optional.of(sucursal));
        var concepto=new ServicioEntidad();concepto.setId(10L);concepto.setPrecio(new BigDecimal("250"));concepto.setMoneda("MXN");
        when(servicios.findByIdAndEmpresaIdAndActivoTrue(10L,1L)).thenReturn(Optional.of(concepto));
        lenient().when(asignaciones.existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(1L,10L,3L)).thenReturn(true);
        var prestador=new PrestadorServicioEntidad();prestador.setUsuarioId(20L);
        lenient().when(prestadores.findByUsuarioIdAndSucursalIdAndActivoTrue(20L,3L)).thenReturn(Optional.of(prestador));
        lenient().when(ofertas.findByIdPrestadorIdAndIdServicioIdAndActivaTrue(20L,10L)).thenReturn(Optional.of(new AsignacionServicioPrestadorEntidad()));
        when(disponibilidad.obtenerFranjasDisponibles(1L,3L,10L,20L,inicio.toLocalDate())).thenReturn(List.of(new FranjaDisponibleResponse(inicio.toString(),inicio.plusHours(1).toString(),20L,10L,3L)));
        var cliente=new UsuarioEntidad();cliente.setId(70L);
        lenient().when(usuarios.findByEmpresaIdAndCorreo(1L,"cliente@example.com")).thenReturn(Optional.of(cliente));
        lenient().when(citas.save(any())).thenAnswer(i -> { CitaEntidad c=i.getArgument(0);c.setId(90L);return c; });
    }
    private ResultadoCotizacionCitas resultado(boolean valida,boolean enviada) {
        BigDecimal neto=new BigDecimal(valida?"200.00":"250.00"), descuento=new BigDecimal(valida?"50.00":"0.00");
        return new ResultadoCotizacionCitas("MXN",new BigDecimal("250.00"),descuento,neto,neto,
            valida?new ResultadoCotizacionCitas.PromocionAplicada(8L,"Oferta","PORCENTAJE",new BigDecimal("20"),"MXN"):null,
            enviada&&!valida?"Promoción vencida":null,List.of(new ResultadoCotizacionCitas.LineaCotizacionCita(10L,"Servicio",new BigDecimal("250.00"),descuento,neto,neto,valida)));
    }
    private CrearCitaRequest request(Long promocionId) { return new CrearCitaRequest(1L,3L,10L,20L,"Cliente prueba","cliente@example.com","2220000000",inicio,null,promocionId); }
    @Test void guardaPrecioNetoListaDescuentoYFotografiaDePromocion() {
        when(cotizador.cotizarPrecio(eq(1L),eq(3L),eq(8L),any())).thenReturn(resultado(true,true));
        var response=servicio.crearCita(request(8L));
        var captor=ArgumentCaptor.forClass(CitaEntidad.class);verify(citas).save(captor.capture());var cita=captor.getValue();
        assertThat(response.precio()).isEqualByComparingTo("200");assertThat(response.precioLista()).isEqualByComparingTo("250");
        assertThat(cita.getDescuento()).isEqualByComparingTo("50");assertThat(cita.getPromocionId()).isEqualTo(8L);
        assertThat(cita.getPromocionSnapshotJson()).contains("Oferta","PORCENTAJE","MXN");
        when(cotizador.cotizarPrecio(eq(1L),eq(3L),eq(8L),any())).thenReturn(resultado(false,true));
        assertThatThrownBy(() -> servicio.crearCita(request(8L))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(cita.getPrecio()).isEqualByComparingTo("200");
    }
    @Test void promocionInvalidaResponde409SinGuardarCita() {
        when(cotizador.cotizarPrecio(eq(1L),eq(3L),eq(8L),any())).thenReturn(resultado(false,true));
        assertThatThrownBy(() -> servicio.crearCita(request(8L))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("409");
        verify(citas,never()).save(any());
        verifyNoInteractions(clientes);
    }
    @Test void sinPromocionConservaPrecioOriginal() {
        when(cotizador.cotizarPrecio(eq(1L),eq(3L),isNull(),any())).thenReturn(resultado(false,false));
        var response=servicio.crearCita(request(null));assertThat(response.precio()).isEqualByComparingTo("250");assertThat(response.descuento()).isZero();
    }
    @Test void multiplesGuardanElPrecioPactadoEnCadaCita() {
        when(disponibilidad.obtenerFranjasDisponibles(1L,3L,10L,20L,inicio.toLocalDate())).thenReturn(List.of(
            new FranjaDisponibleResponse(inicio.toString(),inicio.plusHours(1).toString(),20L,10L,3L),
            new FranjaDisponibleResponse(inicio.plusHours(1).toString(),inicio.plusHours(2).toString(),20L,10L,3L)));
        var base=resultado(true,true);
        var quote=new ResultadoCotizacionCitas("MXN",new BigDecimal("500"),new BigDecimal("100"),new BigDecimal("400"),new BigDecimal("400"),base.promocion(),null,
            List.of(base.lineas().getFirst(),base.lineas().getFirst()));
        when(cotizador.cotizarPrecio(eq(1L),eq(3L),eq(8L),any())).thenReturn(quote);
        var respuesta=servicio.crearCitasMultiples(new CrearCitasMultiplesRequest(1L,3L,"Cliente prueba","cliente@example.com","2220000000",null,
            List.of(new CrearCitaMultipleItemRequest(10L,20L,inicio),new CrearCitaMultipleItemRequest(10L,20L,inicio.plusHours(1))),8L));
        assertThat(respuesta.citas()).hasSize(2).allSatisfy(cita -> {
            assertThat(cita.precio()).isEqualByComparingTo("200");assertThat(cita.precioLista()).isEqualByComparingTo("250");assertThat(cita.descuento()).isEqualByComparingTo("50");
        });
    }
}
