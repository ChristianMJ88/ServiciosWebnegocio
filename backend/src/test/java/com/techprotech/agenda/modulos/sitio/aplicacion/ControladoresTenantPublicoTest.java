package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.api.ControladorCitasPublico;
import com.techprotech.agenda.modulos.citas.aplicacion.*;
import com.techprotech.agenda.modulos.disponibilidad.api.ControladorDisponibilidadPublico;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConsultaDisponibilidad;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ControladoresTenantPublicoTest {
    @Test void omitirEmpresaUsaElTenantResueltoEnCotizacionYAmbasCreaciones() throws Exception {
        var tenant=mock(ResolutorTenantPublico.class);
        when(tenant.resolver("negocio",null)).thenReturn(42L);
        var citas=mock(ServicioCitas.class);var mixta=mock(ServicioCotizacionReservaMixta.class);
        var mvc=MockMvcBuilders.standaloneSetup(new ControladorCitasPublico(citas,mock(ServicioCotizacionCitas.class),mock(ServicioCapacidadesEmpresa.class),tenant,mixta)).build();
        mvc.perform(post("/api/v1/publico/citas/cotizar").header("X-Tenant-Slug","negocio").contentType("application/json")
                .content("{\"sucursalId\":3,\"items\":[{\"servicioId\":8}]}" )).andExpect(status().isOk());
        verify(mixta).cotizar(argThat(r -> r.empresaId().equals(42L)));
        String cliente="\"sucursalId\":3,\"nombreCliente\":\"Cliente prueba\",\"correoCliente\":\"cliente@example.com\",\"telefonoCliente\":\"2220000000\"";
        mvc.perform(post("/api/v1/publico/citas").header("X-Tenant-Slug","negocio").contentType("application/json")
                .content("{"+cliente+",\"servicioId\":8,\"inicio\":\"2099-10-12T10:00:00-06:00\"}" )).andExpect(status().isCreated());
        verify(citas).crearCitaPublica(argThat(r -> r.empresaId().equals(42L)));
        mvc.perform(post("/api/v1/publico/citas/multiples").header("X-Tenant-Slug","negocio").contentType("application/json")
                .content("{"+cliente+",\"items\":[{\"servicioId\":8,\"inicio\":\"2099-10-12T10:00:00-06:00\"}]}" )).andExpect(status().isCreated());
        verify(citas).crearCitasMultiplesPublicas(argThat(r -> r.empresaId().equals(42L)));
    }
    @Test void citasCotizacionYFranjasRechazanSucursalAjenaAntesDeLeerOCrearDatos() throws Exception {
        var sitios=mock(EmpresaSitioConfigRepositorio.class);
        var sitio=new EmpresaSitioConfigEntidad(); sitio.setEmpresaId(42L); sitio.setPublicado(true);
        when(sitios.findBySlug("negocio")).thenReturn(Optional.of(sitio));
        var tenant=new ResolutorTenantPublico(sitios,mock(SucursalRepositorio.class),mock(ServicioRepositorio.class),mock(ServicioSucursalRepositorio.class),mock(UsuarioRepositorio.class));
        var citas=mock(ServicioCitas.class); var cotizador=mock(ServicioCotizacionCitas.class); var disponibilidad=mock(ServicioConsultaDisponibilidad.class);
        var mvc=MockMvcBuilders.standaloneSetup(new ControladorCitasPublico(citas,cotizador,mock(ServicioCapacidadesEmpresa.class),tenant,mock(ServicioCotizacionReservaMixta.class)),new ControladorDisponibilidadPublico(disponibilidad,tenant)).build();
        mvc.perform(get("/api/v1/publico/disponibilidad/franjas").header("X-Tenant-Slug","negocio").param("empresaId","42").param("sucursalId","99").param("servicioId","8").param("fecha","2026-10-12")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/publico/citas/cotizar").header("X-Tenant-Slug","negocio").contentType("application/json")
                .content("{\"empresaId\":42,\"sucursalId\":99,\"items\":[{\"servicioId\":8}]}" )).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/publico/citas").header("X-Tenant-Slug","negocio").contentType("application/json")
                .content("{\"empresaId\":42,\"sucursalId\":99,\"servicioId\":8,\"nombreCliente\":\"Cliente prueba\",\"correoCliente\":\"cliente@example.com\",\"telefonoCliente\":\"2220000000\",\"inicio\":\"2099-10-12T10:00:00-06:00\"}" )).andExpect(status().isNotFound());
        verifyNoInteractions(citas,cotizador,disponibilidad);
    }
}
