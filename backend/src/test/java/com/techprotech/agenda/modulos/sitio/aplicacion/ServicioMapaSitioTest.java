package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioMapaSitioTest {

    @Test
    void publicaSoloSucursalesActivasConDireccionYGeneraRutas() {
        SucursalEntidad centro = sucursal(2L, "Centro", "Av. Reforma 100, Puebla", "222 123 4567");
        SucursalEntidad sinDireccion = sucursal(3L, "Norte", null, null);
        SucursalRepositorio repositorio = (SucursalRepositorio) Proxy.newProxyInstance(
                SucursalRepositorio.class.getClassLoader(),
                new Class<?>[]{SucursalRepositorio.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByEmpresaIdAndActivaTrue")) return List.of(sinDireccion, centro);
                    throw new UnsupportedOperationException(method.getName());
                }
        );

        List<UbicacionMapaSitioPublicaResponse> resultado = new ServicioMapaSitio(repositorio)
                .obtenerParaSitioPublico(9L);

        assertEquals(1, resultado.size());
        assertEquals("Centro", resultado.getFirst().nombre());
        assertTrue(resultado.getFirst().mapaEmbedUrl().startsWith("https://www.google.com/maps?q="));
        assertTrue(resultado.getFirst().indicacionesUrl().contains("api=1"));
    }

    private SucursalEntidad sucursal(Long id, String nombre, String direccion, String telefono) {
        SucursalEntidad sucursal = new SucursalEntidad();
        sucursal.setId(id);
        sucursal.setEmpresaId(9L);
        sucursal.setNombre(nombre);
        sucursal.setDireccion(direccion);
        sucursal.setTelefono(telefono);
        sucursal.setActiva(true);
        return sucursal;
    }
}
