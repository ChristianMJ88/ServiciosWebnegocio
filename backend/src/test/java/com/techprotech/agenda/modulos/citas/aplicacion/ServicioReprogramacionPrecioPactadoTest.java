package com.techprotech.agenda.modulos.citas.aplicacion;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.*;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ServicioReprogramacionPrecioPactadoTest {
    @Test void reprogramarNoModificaPrecioNiSnapshotAunqueLaOfertaHayaTerminado() {
        var citas=mock(CitaRepositorio.class);var sucursales=mock(SucursalRepositorio.class);var disponibilidad=mock(ServicioConsultaDisponibilidad.class);
        var servicio=new ServicioCitasCliente(citas,mock(HistorialEstadoCitaRepositorio.class),sucursales,mock(ServicioRepositorio.class),
            mock(PrestadorServicioRepositorio.class),disponibilidad,mock(ClienteRepositorio.class),mock(UsuarioRepositorio.class),
            mock(ServicioOutboxWhatsappCitas.class),mock(ServicioOutboxCorreoCitas.class));
        var cita=new CitaEntidad();cita.setId(90L);cita.setEmpresaId(1L);cita.setSucursalId(3L);cita.setServicioId(10L);cita.setPrestadorId(20L);cita.setClienteId(70L);cita.setEstado("CONFIRMADA");
        cita.setInicio(LocalDateTime.now().plusDays(10));cita.setFin(cita.getInicio().plusHours(1));cita.setMoneda("MXN");cita.setPrecio(new BigDecimal("200"));cita.setPrecioLista(new BigDecimal("250"));cita.setDescuento(new BigDecimal("50"));cita.setPromocionId(8L);cita.setPromocionSnapshotJson("{\"id\":8,\"titulo\":\"Oferta original\"}");
        when(citas.findByIdAndEmpresaIdAndClienteId(90L,1L,70L)).thenReturn(Optional.of(cita));
        var sucursal=new SucursalEntidad();sucursal.setZonaHoraria("America/Mexico_City");sucursal.setNombre("Sucursal");when(sucursales.findById(3L)).thenReturn(Optional.of(sucursal));
        var inicio=OffsetDateTime.now(ZoneId.of(sucursal.getZonaHoraria())).plusDays(30).withSecond(0).withNano(0);
        when(disponibilidad.obtenerFranjasDisponibles(1L,3L,10L,20L,inicio.toLocalDate())).thenReturn(List.of(new FranjaDisponibleResponse(inicio.toString(),inicio.plusHours(1).toString(),20L,10L,3L)));
        servicio.reprogramar(1L,70L,90L,inicio);
        assertThat(cita.getPrecio()).isEqualByComparingTo("200");assertThat(cita.getPrecioLista()).isEqualByComparingTo("250");assertThat(cita.getDescuento()).isEqualByComparingTo("50");assertThat(cita.getPromocionId()).isEqualTo(8L);assertThat(cita.getPromocionSnapshotJson()).contains("Oferta original");
    }
}
