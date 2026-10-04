package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.AuditoriaPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.AuditoriaPlataformaRepositorio;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class ServicioAuditoriaPlataforma {
    private final AuditoriaPlataformaRepositorio repositorio;
    private final ObjectMapper json;
    public ServicioAuditoriaPlataforma(AuditoriaPlataformaRepositorio repositorio, ObjectMapper json) {
        this.repositorio=repositorio; this.json=json;
    }
    public void registrar(Long usuarioId, String accion, String recurso, String recursoId, Map<String,?> detalle) {
        AuditoriaPlataformaEntidad evento=new AuditoriaPlataformaEntidad();
        evento.setUsuarioId(usuarioId); evento.setAccion(accion); evento.setRecurso(recurso); evento.setRecursoId(recursoId);
        try { evento.setDetalle(json.writeValueAsString(detalle)); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("No fue posible registrar la auditoría",ex); }
        repositorio.save(evento);
    }
}
