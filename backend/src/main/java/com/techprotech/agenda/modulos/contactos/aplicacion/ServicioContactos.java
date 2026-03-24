package com.techprotech.agenda.modulos.contactos.aplicacion;

import com.techprotech.agenda.compartido.correo.NotificacionSolicitudContactoCorreo;
import com.techprotech.agenda.compartido.correo.ServicioCorreoContactos;
import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoContactos;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.contactos.api.dto.CrearSolicitudContactoRequest;
import com.techprotech.agenda.modulos.contactos.api.dto.SolicitudContactoCreadaResponse;
import com.techprotech.agenda.modulos.contactos.infraestructura.entidad.SolicitudContactoEntidad;
import com.techprotech.agenda.modulos.contactos.infraestructura.repositorio.SolicitudContactoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioContactos {

    private final SolicitudContactoRepositorio solicitudContactoRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final ServicioCorreoContactos servicioCorreoContactos;
    private final ServicioOutboxCorreoContactos servicioOutboxCorreoContactos;

    public ServicioContactos(
            SolicitudContactoRepositorio solicitudContactoRepositorio,
            EmpresaRepositorio empresaRepositorio,
            ServicioCorreoContactos servicioCorreoContactos,
            ServicioOutboxCorreoContactos servicioOutboxCorreoContactos
    ) {
        this.solicitudContactoRepositorio = solicitudContactoRepositorio;
        this.empresaRepositorio = empresaRepositorio;
        this.servicioCorreoContactos = servicioCorreoContactos;
        this.servicioOutboxCorreoContactos = servicioOutboxCorreoContactos;
    }

    @Transactional
    public SolicitudContactoCreadaResponse crearSolicitud(CrearSolicitudContactoRequest request) {
        Long empresaId = request.empresaId() != null ? request.empresaId() : 1L;
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La empresa no existe"));
        ZoneId zonaEmpresa = ZoneId.of(empresa.getZonaHoraria());

        SolicitudContactoEntidad solicitud = new SolicitudContactoEntidad();
        solicitud.setEmpresaId(empresaId);
        solicitud.setNombreCompleto(request.nombreCompleto().trim());
        solicitud.setTelefono(normalizarOpcional(request.telefono()));
        solicitud.setCorreo(request.correo().trim().toLowerCase());
        solicitud.setAsunto(request.asunto().trim());
        solicitud.setMensaje(request.mensaje().trim());
        solicitud.setCanal("WEB");
        solicitud.setEstado("NUEVO");
        solicitud.setNotificacionCorreoProgramada(false);
        solicitud = solicitudContactoRepositorio.save(solicitud);
        OffsetDateTime creadaEn = resolverFechaCreacion(solicitud, zonaEmpresa)
                .atZone(zonaEmpresa)
                .toOffsetDateTime();

        boolean correoNotificacionProgramado = servicioOutboxCorreoContactos.programarNotificacion(
                empresaId,
                new NotificacionSolicitudContactoCorreo(
                        solicitud.getId(),
                        empresa.getNombre(),
                        solicitud.getNombreCompleto(),
                        solicitud.getCorreo(),
                        solicitud.getTelefono(),
                        solicitud.getAsunto(),
                        solicitud.getMensaje(),
                        solicitud.getCanal(),
                        creadaEn
                )
        );

        solicitud.setNotificacionCorreoProgramada(correoNotificacionProgramado);
        solicitud = solicitudContactoRepositorio.save(solicitud);

        return new SolicitudContactoCreadaResponse(
                solicitud.getId(),
                solicitud.getEmpresaId(),
                solicitud.getEstado(),
                resolverMensajeRespuesta(empresaId, correoNotificacionProgramado),
                correoNotificacionProgramado,
                creadaEn
        );
    }

    private LocalDateTime resolverFechaCreacion(SolicitudContactoEntidad solicitud, ZoneId zonaEmpresa) {
        if (solicitud.getCreadaEn() != null) {
            return solicitud.getCreadaEn();
        }
        return LocalDateTime.now(zonaEmpresa);
    }

    private String resolverMensajeRespuesta(Long empresaId, boolean correoNotificacionProgramado) {
        if (!servicioCorreoContactos.estaHabilitado(empresaId)) {
            return "Tu mensaje fue enviado correctamente. Nuestro equipo te contactara pronto.";
        }

        if (correoNotificacionProgramado) {
            return "Tu mensaje fue enviado correctamente. El equipo ya fue notificado por correo.";
        }

        return "Tu mensaje fue enviado correctamente. Nuestro equipo te contactara pronto.";
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isBlank() ? null : normalizado;
    }
}
