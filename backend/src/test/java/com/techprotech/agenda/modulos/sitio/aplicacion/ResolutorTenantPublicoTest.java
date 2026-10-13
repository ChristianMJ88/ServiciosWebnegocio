package com.techprotech.agenda.modulos.sitio.aplicacion;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ResolutorTenantPublicoTest {
    final EmpresaSitioConfigRepositorio sitios=mock(EmpresaSitioConfigRepositorio.class);
    final SucursalRepositorio sucursales=mock(SucursalRepositorio.class);
    final ServicioRepositorio servicios=mock(ServicioRepositorio.class);
    final ServicioSucursalRepositorio asignaciones=mock(ServicioSucursalRepositorio.class);
    final UsuarioRepositorio usuarios=mock(UsuarioRepositorio.class);
    final ResolutorTenantPublico tenant=new ResolutorTenantPublico(sitios,sucursales,servicios,asignaciones,usuarios);
    @Test void slugDefineTenantYEmpresaAjenaResponde404() {
        var sitio=new EmpresaSitioConfigEntidad(); sitio.setEmpresaId(42L); sitio.setPublicado(true);
        when(sitios.findBySlug("negocio")).thenReturn(Optional.of(sitio));
        assertThat(tenant.resolver("Negocio",null)).isEqualTo(42L);
        assertThatThrownBy(() -> tenant.resolver("negocio",99L)).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }
    @Test void noHayTenantPredeterminado() {
        assertThatThrownBy(() -> tenant.resolver(null,null)).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(sitios);
    }
    @Test void sucursalAjenaNoPermiteConsultarServicios() {
        assertThatThrownBy(() -> tenant.validarServicio(42L,99L,8L,null)).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        verifyNoInteractions(servicios,asignaciones,usuarios);
    }
    @Test void servicioAjenoResponde404() {
        when(sucursales.findByIdAndEmpresaIdAndActivaTrue(3L,42L)).thenReturn(Optional.of(new SucursalEntidad()));
        assertThatThrownBy(() -> tenant.validarServicio(42L,3L,99L,null)).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        verifyNoInteractions(asignaciones,usuarios);
    }
    @Test void prestadorAjenoResponde404() {
        when(sucursales.findByIdAndEmpresaIdAndActivaTrue(3L,42L)).thenReturn(Optional.of(new SucursalEntidad()));
        when(servicios.findByIdAndEmpresaIdAndActivoTrue(8L,42L)).thenReturn(Optional.of(new ServicioEntidad()));
        when(asignaciones.existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(42L,8L,3L)).thenReturn(true);
        assertThatThrownBy(() -> tenant.validarServicio(42L,3L,8L,99L)).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        verify(usuarios).findByIdAndEmpresaId(99L,42L);
    }
}
