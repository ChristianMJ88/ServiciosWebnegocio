package com.techprotech.agenda.modulos.admin.aplicacion;

import com.techprotech.agenda.modulos.admin.api.dto.*;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaRolEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaRolEmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioInternoPerfilRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class ServicioBloqueoAcceso {
    private final UsuarioRepositorio usuarios;
    private final UsuarioInternoPerfilRepositorio internos;
    private final PrestadorServicioRepositorio prestadores;
    private final AuditoriaRolEmpresaRepositorio auditoria;

    public ServicioBloqueoAcceso(UsuarioRepositorio usuarios, UsuarioInternoPerfilRepositorio internos,
                                PrestadorServicioRepositorio prestadores, AuditoriaRolEmpresaRepositorio auditoria) {
        this.usuarios = usuarios; this.internos = internos; this.prestadores = prestadores; this.auditoria = auditoria;
    }

    @Transactional
    public BloqueoAccesoResponse cambiar(UsuarioAutenticado actor, Long id, boolean prestador, BloqueoAccesoRequest request) {
        if (actor.esPlataforma() || actor.empresaId() == null) throw new ResponseStatusException(FORBIDDEN);
        var usuario = usuarios.bloquearPorIdYEmpresa(id, actor.empresaId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario no disponible en este negocio"));
        if (id.equals(actor.usuarioId())) throw new ResponseStatusException(CONFLICT, "No puedes suspender tu propio acceso");
        boolean perfilExiste = prestador ? prestadores.existsById(id) : internos.existsById(id);
        if (!perfilExiste) throw new ResponseStatusException(NOT_FOUND, "El perfil solicitado no existe");
        if (usuario.isBloqueado() == request.bloqueado()) throw new ResponseStatusException(CONFLICT, "El acceso ya tiene ese estado");
        usuario.setBloqueado(request.bloqueado());
        usuario.invalidarSesiones();
        usuarios.save(usuario);
        var evento = new AuditoriaRolEmpresaEntidad();
        evento.setEmpresaId(actor.empresaId()); evento.setUsuarioActorId(actor.usuarioId()); evento.setActorCorreo(actor.correo());
        evento.setAccion(request.bloqueado() ? "ACCESO_BLOQUEADO" : "ACCESO_DESBLOQUEADO");
        evento.setResumen("Usuario " + id + ": " + request.motivo().trim().substring(0, Math.min(request.motivo().trim().length(), 220)));
        try {
            evento.setDetalleDespuesJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                    java.util.Map.of("usuarioId", id, "bloqueado", request.bloqueado(), "motivo", request.motivo().trim())));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException(e); }
        auditoria.save(evento);
        return new BloqueoAccesoResponse(id, usuario.isBloqueado(), usuario.isHabilitado());
    }
}
