package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServicioEquipoSitioTest {

    @Test
    void publicaUnicamenteColaboradoresActivosDeSucursalesDelTenant() {
        SucursalEntidad sucursal = new SucursalEntidad();
        sucursal.setId(10L);
        sucursal.setEmpresaId(7L);
        sucursal.setNombre("Centro");
        sucursal.setActiva(true);

        PrestadorServicioEntidad activo = prestador(21L, 10L, "Ana López", true);
        activo.setBiografia("Especialista en cuidado de manos.");
        activo.setFotoUrl("https://cdn.example/ana.jpg");
        PrestadorServicioEntidad inactivo = prestador(22L, 10L, "Persona inactiva", false);

        SucursalRepositorio sucursales = proxy(SucursalRepositorio.class, "findByEmpresaIdAndActivaTrue", List.of(sucursal));
        PrestadorServicioRepositorio prestadores = proxy(
                PrestadorServicioRepositorio.class,
                "findBySucursalIdInOrderByNombreMostrarAsc",
                List.of(activo, inactivo)
        );

        List<IntegranteEquipoSitioPublicoResponse> equipo = new ServicioEquipoSitio(sucursales, prestadores)
                .obtenerParaSitioPublico(7L);

        assertEquals(1, equipo.size());
        assertEquals("Ana López", equipo.getFirst().nombre());
        assertEquals("Centro", equipo.getFirst().sucursalNombre());
        assertEquals("https://cdn.example/ana.jpg", equipo.getFirst().fotoUrl());
    }

    private PrestadorServicioEntidad prestador(Long id, Long sucursalId, String nombre, boolean activo) {
        PrestadorServicioEntidad prestador = new PrestadorServicioEntidad();
        prestador.setUsuarioId(id);
        prestador.setSucursalId(sucursalId);
        prestador.setNombreMostrar(nombre);
        prestador.setActivo(activo);
        return prestador;
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> tipo, String metodoEsperado, Object respuesta) {
        return (T) Proxy.newProxyInstance(
                tipo.getClassLoader(),
                new Class<?>[]{tipo},
                (proxy, method, args) -> {
                    if (method.getName().equals(metodoEsperado)) return respuesta;
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
