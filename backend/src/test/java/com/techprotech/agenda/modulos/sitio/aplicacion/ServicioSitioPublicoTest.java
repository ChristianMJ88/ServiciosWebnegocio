package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServicioSitioPublicoTest {

    @Test
    void devuelveUnicamenteLosSlugsPublicadosOrdenadosPorRepositorio() {
        EmpresaSitioConfigRepositorio repositorio = (EmpresaSitioConfigRepositorio) Proxy.newProxyInstance(
                EmpresaSitioConfigRepositorio.class.getClassLoader(),
                new Class<?>[]{EmpresaSitioConfigRepositorio.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findSlugsPublicados")) {
                        return List.of("barberia-centro", "nail-air");
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        ServicioSitioPublico servicio = new ServicioSitioPublico(repositorio);

        IndiceSitiosPublicosResponse response = servicio.obtenerIndicePublicado();

        assertEquals(List.of("barberia-centro", "nail-air"), response.slugs());
    }
}
