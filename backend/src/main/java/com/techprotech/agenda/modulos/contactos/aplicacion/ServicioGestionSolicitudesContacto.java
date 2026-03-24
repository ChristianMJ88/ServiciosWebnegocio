package com.techprotech.agenda.modulos.contactos.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.contactos.api.dto.SolicitudContactoAdminResponse;
import com.techprotech.agenda.modulos.contactos.infraestructura.entidad.SolicitudContactoEntidad;
import com.techprotech.agenda.modulos.contactos.infraestructura.repositorio.SolicitudContactoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioGestionSolicitudesContacto {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("NUEVO", "EN_PROCESO", "ATENDIDO", "CERRADO");

    private final SolicitudContactoRepositorio solicitudContactoRepositorio;
    private final EmpresaRepositorio empresaRepositorio;

    public ServicioGestionSolicitudesContacto(
            SolicitudContactoRepositorio solicitudContactoRepositorio,
            EmpresaRepositorio empresaRepositorio
    ) {
        this.solicitudContactoRepositorio = solicitudContactoRepositorio;
        this.empresaRepositorio = empresaRepositorio;
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoAdminResponse> listarPorEmpresa(Long empresaId) {
        EmpresaEntidad empresa = obtenerEmpresa(empresaId);
        ZoneId zona = ZoneId.of(empresa.getZonaHoraria());

        return solicitudContactoRepositorio.findByEmpresaIdOrderByCreadaEnDesc(empresaId).stream()
                .map(solicitud -> mapear(solicitud, zona))
                .toList();
    }

    @Transactional
    public SolicitudContactoAdminResponse actualizarEstado(Long empresaId, Long solicitudId, String estado) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new ResponseStatusException(BAD_REQUEST, "El estado indicado no es válido para la solicitud de contacto");
        }

        EmpresaEntidad empresa = obtenerEmpresa(empresaId);
        ZoneId zona = ZoneId.of(empresa.getZonaHoraria());
        SolicitudContactoEntidad solicitud = solicitudContactoRepositorio.findByIdAndEmpresaId(solicitudId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La solicitud de contacto no existe para la empresa"));

        solicitud.setEstado(estado);
        return mapear(solicitudContactoRepositorio.save(solicitud), zona);
    }

    @Transactional
    public void marcarNotificada(Long solicitudId) {
        solicitudContactoRepositorio.findById(solicitudId).ifPresent(solicitud -> {
            solicitud.setNotificadaEn(java.time.LocalDateTime.now());
            solicitudContactoRepositorio.save(solicitud);
        });
    }

    private EmpresaEntidad obtenerEmpresa(Long empresaId) {
        return empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La empresa no existe"));
    }

    private SolicitudContactoAdminResponse mapear(SolicitudContactoEntidad solicitud, ZoneId zona) {
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
                solicitud.getNotificadaEn() != null ? solicitud.getNotificadaEn().atZone(zona).toOffsetDateTime() : null,
                resolverCreadaEn(solicitud, zona)
        );
    }

    private OffsetDateTime resolverCreadaEn(SolicitudContactoEntidad solicitud, ZoneId zona) {
        LocalDateTime creadaEn = solicitud.getCreadaEn();
        if (creadaEn == null) {
            return null;
        }
        return creadaEn.atZone(zona).toOffsetDateTime();
    }
}
