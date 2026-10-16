package com.techprotech.agenda.modulos.contactos.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.contactos.api.dto.SolicitudContactoAdminResponse;
import com.techprotech.agenda.modulos.contactos.infraestructura.entidad.SolicitudContactoEntidad;
import com.techprotech.agenda.modulos.contactos.infraestructura.repositorio.SolicitudContactoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static com.techprotech.agenda.compartido.tiempo.MomentoUtc.de;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.techprotech.agenda.modulos.contactos.api.dto.ResumenContactosResponse;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioGestionSolicitudesContacto {
    private final java.time.Clock reloj;

    private final SolicitudContactoRepositorio solicitudContactoRepositorio;
    private final EmpresaRepositorio empresaRepositorio;

    public ServicioGestionSolicitudesContacto(
            SolicitudContactoRepositorio solicitudContactoRepositorio,
            EmpresaRepositorio empresaRepositorio,
            java.time.Clock reloj
    ) {
        this.reloj = reloj;
        this.solicitudContactoRepositorio = solicitudContactoRepositorio;
        this.empresaRepositorio = empresaRepositorio;
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoAdminResponse> listarPorEmpresa(Long empresaId) {
        obtenerEmpresa(empresaId);

        return solicitudContactoRepositorio.findByEmpresaIdOrderByCreadaEnDesc(empresaId).stream()
                .map(solicitud -> mapear(solicitud))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SolicitudContactoAdminResponse> listarPorEmpresa(Long empresaId, Pageable pageable) {
        obtenerEmpresa(empresaId);
        return solicitudContactoRepositorio.findByEmpresaIdOrderByCreadaEnDesc(empresaId, pageable)
                .map(solicitud -> mapear(solicitud));
    }

    @Transactional(readOnly = true)
    public ResumenContactosResponse resumen(Long empresaId) {
        obtenerEmpresa(empresaId);
        return new ResumenContactosResponse(
                solicitudContactoRepositorio.countByEmpresaId(empresaId),
                solicitudContactoRepositorio.countByEmpresaIdAndEstado(empresaId, EstadoSolicitudContacto.NUEVO.name()),
                solicitudContactoRepositorio.countByEmpresaIdAndEstado(empresaId, EstadoSolicitudContacto.EN_PROCESO.name()),
                solicitudContactoRepositorio.countByEmpresaIdAndEstado(empresaId, EstadoSolicitudContacto.ATENDIDO.name()),
                solicitudContactoRepositorio.findFirstByEmpresaIdAndEstadoOrderByCreadaEnDesc(empresaId, EstadoSolicitudContacto.NUEVO.name())
                        .map(solicitud -> mapear(solicitud)).orElse(null)
        );
    }

    @Transactional
    public SolicitudContactoAdminResponse actualizarEstado(Long empresaId, Long solicitudId, String estado) {
        if (!EstadoSolicitudContacto.esValido(estado)) {
            throw new ResponseStatusException(BAD_REQUEST, "El estado indicado no es válido para la solicitud de contacto");
        }

        obtenerEmpresa(empresaId);
        SolicitudContactoEntidad solicitud = solicitudContactoRepositorio.findByIdAndEmpresaId(solicitudId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La solicitud de contacto no existe para la empresa"));

        solicitud.setEstado(estado);
        return mapear(solicitudContactoRepositorio.save(solicitud));
    }

    @Transactional
    public void marcarNotificada(Long solicitudId) {
        solicitudContactoRepositorio.findById(solicitudId).ifPresent(solicitud -> {
            solicitud.setNotificadaEn(java.time.LocalDateTime.now(reloj));
            solicitudContactoRepositorio.save(solicitud);
        });
    }

    private EmpresaEntidad obtenerEmpresa(Long empresaId) {
        return empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La empresa no existe"));
    }

    private SolicitudContactoAdminResponse mapear(SolicitudContactoEntidad solicitud) {
        return new SolicitudContactoAdminResponse(
                solicitud.getId(),
                solicitud.getEmpresaId(),
                solicitud.getNombreCompleto(),
                solicitud.getTelefono(),
                solicitud.getCorreo(),
                solicitud.getAsunto(),
                solicitud.getMensaje(),
                solicitud.getCanal(),
                solicitud.getEstado(),
                solicitud.isNotificacionCorreoProgramada(),
                de(solicitud.getNotificadaEn()),
                de(solicitud.getCreadaEn())
        );
    }

}
