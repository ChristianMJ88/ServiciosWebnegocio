package com.techprotech.agenda.modulos.dominios.aplicacion;
import com.techprotech.agenda.modulos.dominios.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.dominios.infraestructura.repositorio.EmpresaDominioRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import com.techprotech.agenda.modulos.plataforma.api.dto.*;
import org.junit.jupiter.api.Test;import java.util.*;
import static org.mockito.Mockito.*;import static org.assertj.core.api.Assertions.*;
class ServicioConsultaDominiosTest {
 @Test void capacidadVigenteSinCacheYRecuperacionSinVerificar(){
 var repo=mock(EmpresaDominioRepositorio.class);var cap=mock(ServicioCapacidadesEmpresa.class);var servicio=new ServicioConsultaDominios(repo,cap,new ValidadorHostname("https://platform.example","https://console.example"));
 var d=new EmpresaDominioEntidad();d.setEmpresaId(5L);d.setHostname("marca.mx");d.setEstado(EstadoDominio.ACTIVO);when(repo.findByHostnameAndEstado("marca.mx",EstadoDominio.ACTIVO)).thenReturn(Optional.of(d));
 var yes=new CapacidadesEmpresaResponse(true,1L,4L,"FIXTURE_NEUTRO","Fixture","ACTIVA",null,null,List.of(new CapacidadModuloEmpresaResponse("DOMINIO_PROPIO","Dominio",null)),List.of(),List.of());
 when(cap.obtener(5L)).thenReturn(yes);assertThat(servicio.empresaActiva("marca.mx")).isEqualTo(5L);
 when(cap.obtener(5L)).thenReturn(CapacidadesEmpresaResponse.sinSuscripcion());assertThatThrownBy(()->servicio.empresaActiva("marca.mx")).isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(404));assertThat(servicio.dominioPublico(5L,"marca.mx")).isNull();assertThat(d.getEstado()).isEqualTo(EstadoDominio.ACTIVO);
 when(cap.obtener(5L)).thenReturn(yes);assertThat(servicio.empresaActiva("marca.mx")).isEqualTo(5L);assertThat(servicio.dominioPublico(5L,"marca.mx")).isEqualTo("marca.mx");
 when(repo.findByHostnameAndEstado("marca.mx",EstadoDominio.ACTIVO)).thenReturn(Optional.empty());assertThatThrownBy(()->servicio.empresaActiva("marca.mx")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
}
