package com.techprotech.agenda.modulos.disponibilidad.api;

import com.techprotech.agenda.modulos.disponibilidad.api.dto.ExcepcionDisponibilidadRequest;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ExcepcionDisponibilidadResponse;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ReglaDisponibilidadRequest;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ReglaDisponibilidadResponse;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioGestionDisponibilidad;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/disponibilidad")
@PreAuthorize("hasAuthority('STAFF_PANEL_ACCESO')")
public class ControladorDisponibilidadStaff {

    private final com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioSolicitudesHorario solicitudes;
    private final ServicioGestionDisponibilidad servicioGestionDisponibilidad;
    private final com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioHorarioSemanal horarioSemanal;

    public ControladorDisponibilidadStaff(ServicioGestionDisponibilidad servicioGestionDisponibilidad,
            com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioHorarioSemanal horarioSemanal, com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioSolicitudesHorario solicitudes) {
        this.solicitudes = solicitudes;
        this.servicioGestionDisponibilidad = servicioGestionDisponibilidad;
        this.horarioSemanal = horarioSemanal;
    }

    @GetMapping("/metadatos")
    @PreAuthorize("hasAuthority('STAFF_DISPONIBILIDAD_GESTIONAR')")
    public com.techprotech.agenda.modulos.disponibilidad.api.dto.MetadatosDisponibilidadResponse metadatos() {
        return com.techprotech.agenda.modulos.disponibilidad.aplicacion.CatalogosDisponibilidad.metadatos();
    }

    @PostMapping("/reglas/semana")
    @PreAuthorize("hasAuthority('STAFF_DISPONIBILIDAD_GESTIONAR')")
    public List<ReglaDisponibilidadResponse> guardarSemana(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody com.techprotech.agenda.modulos.disponibilidad.api.dto.HorarioSemanalRequest request) {
        return horarioSemanal.guardarStaff(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @PostMapping("/solicitudes-horario")
    @PreAuthorize("hasAuthority('STAFF_DISPONIBILIDAD_GESTIONAR')")
    public com.techprotech.agenda.modulos.disponibilidad.api.dto.SolicitudHorarioResponse solicitar(
        @AuthenticationPrincipal UsuarioAutenticado usuario,
        @Valid @RequestBody com.techprotech.agenda.modulos.disponibilidad.api.dto.SolicitudHorarioRequest request) {
        return solicitudes.solicitar(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @GetMapping("/reglas")
    @PreAuthorize("hasAuthority('STAFF_DISPONIBILIDAD_GESTIONAR')")
    public List<ReglaDisponibilidadResponse> reglas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioGestionDisponibilidad.listarReglasStaff(usuario.empresaId(), usuario.usuarioId());
    }

    @PostMapping("/reglas")
    @PreAuthorize("hasAuthority('STAFF_DISPONIBILIDAD_GESTIONAR')")
    public ReglaDisponibilidadResponse crearRegla(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ReglaDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.guardarReglaStaff(usuario.empresaId(), usuario.usuarioId(), null, request);
    }

    @PatchMapping("/reglas/{id}")
    @PreAuthorize("hasAuthority('STAFF_DISPONIBILIDAD_GESTIONAR')")
    public ReglaDisponibilidadResponse actualizarRegla(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ReglaDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.guardarReglaStaff(usuario.empresaId(), usuario.usuarioId(), id, request);
    }

    @GetMapping("/excepciones")
    @PreAuthorize("hasAuthority('STAFF_DISPONIBILIDAD_GESTIONAR')")
    public List<ExcepcionDisponibilidadResponse> excepciones(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioGestionDisponibilidad.listarExcepcionesStaff(usuario.empresaId(), usuario.usuarioId());
    }

    @PostMapping("/excepciones")
    @PreAuthorize("hasAuthority('STAFF_DISPONIBILIDAD_GESTIONAR')")
    public ExcepcionDisponibilidadResponse crearExcepcion(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ExcepcionDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.guardarExcepcionStaff(usuario.empresaId(), usuario.usuarioId(), null, request);
    }

    @PatchMapping("/excepciones/{id}")
    @PreAuthorize("hasAuthority('STAFF_DISPONIBILIDAD_GESTIONAR')")
    public ExcepcionDisponibilidadResponse actualizarExcepcion(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ExcepcionDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.guardarExcepcionStaff(usuario.empresaId(), usuario.usuarioId(), id, request);
    }
}
