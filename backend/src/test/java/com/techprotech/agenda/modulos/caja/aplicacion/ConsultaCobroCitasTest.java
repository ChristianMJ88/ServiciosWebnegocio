package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import com.techprotech.agenda.modulos.plataforma.api.dto.*;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultaCobroCitasTest {
    final PagoCitaRepositorio pagos=mock(PagoCitaRepositorio.class);
    final VentaDetalleRepositorio ventas=mock(VentaDetalleRepositorio.class);
    final CajaSesionRepositorio sesiones=mock(CajaSesionRepositorio.class);
    final ServicioCapacidadesEmpresa capacidades=mock(ServicioCapacidadesEmpresa.class);
    final ServicioConsultaCobroCitas servicio=new ServicioConsultaCobroCitas(
            new ServicioSaldoCitas(mock(CitaRepositorio.class),pagos,ventas),sesiones,capacidades);

    @ParameterizedTest
    @CsvSource({"true,true,true,true,true,true", "false,true,true,true,true,false", "true,false,true,true,true,false",
            "true,true,false,true,true,false", "true,true,true,false,true,false", "true,true,true,true,false,false"})
    void backendDecideConPermisosEfectivosModuloSuscripcionYSesion(boolean permiso,boolean acceso,boolean vigente,boolean modulo,boolean abierta,boolean esperado) {
        var permisos=new ArrayList<String>();if(permiso)permisos.add("CAJA_COBRAR");if(acceso)permisos.add("CAJA_ACCESO");
        if(permiso && acceso) when(capacidades.obtener(7L)).thenReturn(capacidad(vigente,modulo));
        if(permiso && acceso && vigente && modulo)when(sesiones.buscarSucursalesConSesion(7L,List.of(3L),"ABIERTA")).thenReturn(abierta?List.of(3L):List.of());
        var respuesta=servicio.de(actor(permisos,List.of(3L)),ServicioSaldoCitasTest.cita(1L,7L,"FINALIZADA","500"));
        assertThat(respuesta.puedeCobrar()).isEqualTo(esperado);
        assertThat(respuesta.saldo().pendiente()).isEqualByComparingTo("500");
        if(!permiso || !acceso)verifyNoInteractions(capacidades,sesiones);
    }

    @Test void alcanceSeRespetaAunqueElUsuarioTengaPermisoYNoSeDeducePorNombreDeRol() {
        var cita=ServicioSaldoCitasTest.cita(1L,7L,"FINALIZADA","500");
        assertThat(servicio.de(actor(List.of("CAJA_ACCESO","CAJA_COBRAR"),List.of(9L)),cita).puedeCobrar()).isFalse();
        var adminSinPermiso=new UsuarioAutenticado(null,11L,7L,"EMPRESA",List.of("ADMIN"),List.of(),List.of());
        assertThat(servicio.de(adminSinPermiso,cita).puedeCobrar()).isFalse();verifyNoInteractions(capacidades,sesiones);
    }

    @Test void loteConsultaCapacidadesYSesionesUnaVezYSoloHabilitaSaldoCobrable() {
        when(capacidades.obtener(7L)).thenReturn(capacidad(true,true));
        when(sesiones.buscarSucursalesConSesion(7L,List.of(3L),"ABIERTA")).thenReturn(List.of(3L));
        var citas=List.of(ServicioSaldoCitasTest.cita(1L,7L,"FINALIZADA","500"),ServicioSaldoCitasTest.cita(2L,7L,"NO_ASISTIO","500"),
                ServicioSaldoCitasTest.cita(3L,7L,"FINALIZADA","0"),ServicioSaldoCitasTest.cita(4L,7L,"CONFIRMADA","500"));
        var resultado=servicio.consultar(actor(List.of("CAJA_ACCESO","CAJA_COBRAR"),List.of()),citas);
        assertThat(resultado.get(1L).puedeCobrar()).isTrue();
        assertThat(List.of(2L,3L,4L)).allMatch(id -> !resultado.get(id).puedeCobrar());
        verify(capacidades).obtener(7L);verifyNoMoreInteractions(capacidades);
        verify(sesiones).buscarSucursalesConSesion(7L,List.of(3L),"ABIERTA");verifyNoMoreInteractions(sesiones);
        verify(pagos).sumarPagadoPorCitas(7L,List.of(1L,2L,3L,4L));
        verify(ventas).sumarServiciosPagadosPorCitas(7L,List.of(1L,2L,3L,4L));
    }

    static UsuarioAutenticado actor(List<String> permisos,List<Long> alcance) {
        return new UsuarioAutenticado(null,11L,7L,"EMPRESA",List.of("ROL_PERSONALIZADO"),permisos,alcance);
    }
    static CapacidadesEmpresaResponse capacidad(boolean vigente,boolean modulo) {
        return new CapacidadesEmpresaResponse(vigente,1L,2L,"PLAN",null,"ACTIVA",null,null,
                modulo?List.of(new CapacidadModuloEmpresaResponse("CAJA","Caja",null)):List.of(),List.of(),List.of());
    }
}
