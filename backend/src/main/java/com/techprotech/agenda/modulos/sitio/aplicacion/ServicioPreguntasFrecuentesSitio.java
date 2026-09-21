package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.sitio.api.dto.*;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioPreguntaFrecuenteEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioPreguntaFrecuenteRepositorio;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Comparator;
import java.util.List;

@Service
public class ServicioPreguntasFrecuentesSitio {
    private final EmpresaSitioPreguntaFrecuenteRepositorio repositorio;
    private final EmpresaSitioConfigRepositorio sitioRepositorio;
    private final AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio;
    private final ObjectMapper objectMapper;

    public ServicioPreguntasFrecuentesSitio(EmpresaSitioPreguntaFrecuenteRepositorio repositorio,
            EmpresaSitioConfigRepositorio sitioRepositorio,
            AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio, ObjectMapper objectMapper) {
        this.repositorio = repositorio;
        this.sitioRepositorio = sitioRepositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.objectMapper = objectMapper;
    }

    public List<PreguntaFrecuenteSitioAdminResponse> obtenerParaAdministracion(Long empresaId) {
        validarSitio(empresaId);
        return mapearAdmin(empresaId);
    }

    @Transactional
    public List<PreguntaFrecuenteSitioAdminResponse> actualizar(Long empresaId, Long usuarioActorId,
            ActualizarPreguntasFrecuentesSitioRequest request) {
        validarSitio(empresaId);
        String antes = serializar(mapearAdmin(empresaId));
        List<PreguntaFrecuenteSitioRequest> preguntas = request.preguntas().stream()
                .sorted(Comparator.comparingInt(PreguntaFrecuenteSitioRequest::orden)).toList();
        repositorio.deleteByEmpresaId(empresaId);
        repositorio.flush();
        for (int orden = 0; orden < preguntas.size(); orden++) {
            PreguntaFrecuenteSitioRequest item = preguntas.get(orden);
            EmpresaSitioPreguntaFrecuenteEntidad entidad = new EmpresaSitioPreguntaFrecuenteEntidad();
            entidad.setEmpresaId(empresaId);
            entidad.setPregunta(item.pregunta().trim());
            entidad.setRespuesta(item.respuesta().trim());
            entidad.setActiva(item.activa());
            entidad.setOrden(orden);
            repositorio.save(entidad);
        }
        List<PreguntaFrecuenteSitioAdminResponse> respuesta = mapearAdmin(empresaId);
        registrarAuditoria(empresaId, usuarioActorId, antes, serializar(respuesta));
        return respuesta;
    }

    public List<PreguntaFrecuenteSitioPublicaResponse> obtenerParaSitioPublico(Long empresaId) {
        return repositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .filter(EmpresaSitioPreguntaFrecuenteEntidad::isActiva)
                .map(item -> new PreguntaFrecuenteSitioPublicaResponse(item.getId(), item.getPregunta(), item.getRespuesta()))
                .toList();
    }

    private List<PreguntaFrecuenteSitioAdminResponse> mapearAdmin(Long empresaId) {
        return repositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .map(item -> new PreguntaFrecuenteSitioAdminResponse(item.getId(), item.getPregunta(), item.getRespuesta(), item.isActiva(), item.getOrden()))
                .toList();
    }

    private void validarSitio(Long empresaId) {
        if (!sitioRepositorio.existsById(empresaId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El tenant no tiene configuración de sitio");
    }

    private void registrarAuditoria(Long empresaId, Long usuarioActorId, String antes, String despues) {
        AuditoriaConfiguracionEmpresaEntidad auditoria = new AuditoriaConfiguracionEmpresaEntidad();
        auditoria.setEmpresaId(empresaId);
        auditoria.setUsuarioActorId(usuarioActorId);
        auditoria.setModulo("SITIO");
        auditoria.setAccion("PREGUNTAS_FRECUENTES_SITIO_ACTUALIZADAS");
        auditoria.setResumen("Se actualizaron las preguntas frecuentes del sitio");
        auditoria.setDetalleAntesJson(antes);
        auditoria.setDetalleDespuesJson(despues);
        auditoriaRepositorio.save(auditoria);
    }

    private String serializar(Object valor) {
        try { return objectMapper.writeValueAsString(valor); }
        catch (JsonProcessingException excepcion) {
            throw new IllegalStateException("No fue posible serializar las preguntas frecuentes", excepcion);
        }
    }
}
