package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioPreguntaFrecuenteEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioPreguntaFrecuenteRepositorio;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ServicioPreguntasFrecuentesSitioTest {
    @Test
    void publicaSoloPreguntasActivasDelTenantEnOrden() {
        EmpresaSitioPreguntaFrecuenteEntidad activa = pregunta(1L, "¿Cómo reservo?", true, 0);
        EmpresaSitioPreguntaFrecuenteEntidad inactiva = pregunta(2L, "Oculta", false, 1);
        EmpresaSitioPreguntaFrecuenteRepositorio repositorio = proxy(
                EmpresaSitioPreguntaFrecuenteRepositorio.class, "findByEmpresaIdOrderByOrdenAsc", List.of(activa, inactiva));
        ServicioPreguntasFrecuentesSitio servicio = new ServicioPreguntasFrecuentesSitio(
                repositorio, proxy(EmpresaSitioConfigRepositorio.class, "existsById", true), null, new ObjectMapper());

        List<PreguntaFrecuenteSitioPublicaResponse> respuesta = servicio.obtenerParaSitioPublico(7L);

        assertEquals(1, respuesta.size());
        assertEquals("¿Cómo reservo?", respuesta.getFirst().pregunta());
    }

    private EmpresaSitioPreguntaFrecuenteEntidad pregunta(Long id, String texto, boolean activa, int orden) {
        EmpresaSitioPreguntaFrecuenteEntidad entidad = new EmpresaSitioPreguntaFrecuenteEntidad();
        entidad.setId(id); entidad.setEmpresaId(7L); entidad.setPregunta(texto);
        entidad.setRespuesta("Respuesta"); entidad.setActiva(activa); entidad.setOrden(orden);
        return entidad;
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> tipo, String metodoEsperado, Object respuesta) {
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo},
                (proxy, metodo, argumentos) -> metodo.getName().equals(metodoEsperado) ? respuesta : null);
    }
}
