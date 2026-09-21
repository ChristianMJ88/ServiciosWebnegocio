package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.sitio.api.dto.ActualizarResenasSitioRequest;
import com.techprotech.agenda.modulos.sitio.dominio.IconoIndicadorConfianza;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioIndicadorConfianzaEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioResenaConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioResenaEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioIndicadorConfianzaRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioResenaConfigRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioResenaRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicioResenasSitioTest {

    @Test
    void publicaSoloResenasEIndicadoresActivosDelTenant() {
        EmpresaSitioResenaConfigEntidad config = new EmpresaSitioResenaConfigEntidad();
        config.setEmpresaId(7L);
        config.setGoogleMapsUrl("https://maps.google.com/example");
        EmpresaSitioResenaConfigRepositorio configRepositorio = repositorio(
                EmpresaSitioResenaConfigRepositorio.class, Map.of("findById", Optional.of(config))
        );

        EmpresaSitioResenaEntidad activa = resena(1L, true);
        EmpresaSitioResenaEntidad inactiva = resena(2L, false);
        EmpresaSitioResenaRepositorio resenaRepositorio = repositorio(
                EmpresaSitioResenaRepositorio.class,
                Map.of("findByEmpresaIdOrderByOrdenAsc", List.of(activa, inactiva))
        );

        EmpresaSitioIndicadorConfianzaEntidad indicadorActivo = indicador(1L, true);
        EmpresaSitioIndicadorConfianzaEntidad indicadorInactivo = indicador(2L, false);
        EmpresaSitioIndicadorConfianzaRepositorio indicadorRepositorio = repositorio(
                EmpresaSitioIndicadorConfianzaRepositorio.class,
                Map.of("findByEmpresaIdOrderByOrdenAsc", List.of(indicadorActivo, indicadorInactivo))
        );

        ServicioResenasSitio servicio = new ServicioResenasSitio(
                configRepositorio, resenaRepositorio, indicadorRepositorio,
                repositorio(EmpresaSitioConfigRepositorio.class, Map.of()),
                null, new ObjectMapper()
        );

        ResenasSitioPublicoResponse respuesta = servicio.obtenerParaSitioPublico(7L);

        assertEquals("https://maps.google.com/example", respuesta.googleMapsUrl());
        assertEquals(1, respuesta.resenas().size());
        assertEquals("Cliente 1", respuesta.resenas().getFirst().nombre());
        assertEquals(1, respuesta.indicadores().size());
        assertEquals("8 años", respuesta.indicadores().getFirst().valor());
    }

    @Test
    void rechazaEnlaceExternoSinEsquemaWeb() {
        EmpresaSitioResenaConfigRepositorio configRepositorio = repositorio(
                EmpresaSitioResenaConfigRepositorio.class, Map.of("findById", Optional.empty())
        );
        EmpresaSitioResenaRepositorio resenaRepositorio = repositorio(
                EmpresaSitioResenaRepositorio.class, Map.of("findByEmpresaIdOrderByOrdenAsc", List.of())
        );
        EmpresaSitioIndicadorConfianzaRepositorio indicadorRepositorio = repositorio(
                EmpresaSitioIndicadorConfianzaRepositorio.class, Map.of("findByEmpresaIdOrderByOrdenAsc", List.of())
        );
        EmpresaSitioConfigRepositorio sitioRepositorio = repositorio(
                EmpresaSitioConfigRepositorio.class, Map.of("existsById", true)
        );

        ServicioResenasSitio servicio = new ServicioResenasSitio(
                configRepositorio, resenaRepositorio, indicadorRepositorio, sitioRepositorio, null, new ObjectMapper()
        );
        ActualizarResenasSitioRequest request = new ActualizarResenasSitioRequest("javascript:alert(1)", List.of(), List.of());

        assertThrows(ResponseStatusException.class, () -> servicio.actualizar(7L, 1L, request));
    }

    @SuppressWarnings("unchecked")
    private <T> T repositorio(Class<T> tipo, Map<String, Object> respuestas) {
        return (T) Proxy.newProxyInstance(
                tipo.getClassLoader(), new Class<?>[]{tipo},
                (proxy, metodo, argumentos) -> {
                    if (respuestas.containsKey(metodo.getName())) return respuestas.get(metodo.getName());
                    if (metodo.getName().equals("toString")) return tipo.getSimpleName() + "TestProxy";
                    if (metodo.getReturnType().equals(boolean.class)) return false;
                    if (metodo.getReturnType().equals(int.class)) return 0;
                    if (metodo.getReturnType().equals(long.class)) return 0L;
                    return null;
                }
        );
    }

    private EmpresaSitioResenaEntidad resena(Long id, boolean activa) {
        EmpresaSitioResenaEntidad entidad = new EmpresaSitioResenaEntidad();
        entidad.setId(id);
        entidad.setEmpresaId(7L);
        entidad.setNombre("Cliente " + id);
        entidad.setCalificacion(5);
        entidad.setComentario("Excelente atención");
        entidad.setActiva(activa);
        entidad.setOrden(id.intValue());
        return entidad;
    }

    private EmpresaSitioIndicadorConfianzaEntidad indicador(Long id, boolean activo) {
        EmpresaSitioIndicadorConfianzaEntidad entidad = new EmpresaSitioIndicadorConfianzaEntidad();
        entidad.setId(id);
        entidad.setEmpresaId(7L);
        entidad.setValor("8 años");
        entidad.setEtiqueta("de experiencia");
        entidad.setIcono(IconoIndicadorConfianza.EXPERIENCIA);
        entidad.setActivo(activo);
        entidad.setOrden(id.intValue());
        return entidad;
    }
}
