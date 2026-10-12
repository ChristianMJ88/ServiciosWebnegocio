package com.techprotech.agenda.modulos.disponibilidad.api;

import com.techprotech.agenda.modulos.disponibilidad.api.dto.ExcepcionDisponibilidadRequest;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ExcepcionDisponibilidadResponse;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ReglaDisponibilidadRequest;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ReglaDisponibilidadResponse;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.MetadatosDisponibilidadResponse;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.CatalogosDisponibilidad;
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
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/v1/admin/disponibilidad")
@PreAuthorize("hasAuthority('PANEL_ADMIN_ACCESO')")
public class ControladorDisponibilidadAdmin {

    private final com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioSolicitudesHorario solicitudes;
    private final ServicioGestionDisponibilidad servicioGestionDisponibilidad;
    private final com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioHorarioSemanal horarioSemanal;

    public ControladorDisponibilidadAdmin(ServicioGestionDisponibilidad servicioGestionDisponibilidad, com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioHorarioSemanal horarioSemanal, com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioSolicitudesHorario solicitudes) {
        this.solicitudes = solicitudes;
        this.servicioGestionDisponibilidad = servicioGestionDisponibilidad;
        this.horarioSemanal = horarioSemanal;
    }

    @GetMapping("/metadatos")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public MetadatosDisponibilidadResponse metadatos() {
        return CatalogosDisponibilidad.metadatos();
    }

    @GetMapping("/solicitudes-horario")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public List<com.techprotech.agenda.modulos.disponibilidad.api.dto.SolicitudHorarioResponse> pendientes(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudes.pendientes(usuario.empresaId());
    }
    @PostMapping("/solicitudes-horario/{id}/resolver")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public void resolver(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
        @Valid @RequestBody com.techprotech.agenda.modulos.disponibilidad.api.dto.ResolverHorarioRequest request) {
        solicitudes.resolver(usuario.empresaId(), usuario.usuarioId(), id, request);
    }

    @GetMapping("/reglas")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public List<ReglaDisponibilidadResponse> reglas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioGestionDisponibilidad.listarReglasAdmin(usuario.empresaId());
    }

    @PostMapping("/reglas")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public ReglaDisponibilidadResponse crearRegla(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ReglaDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.crearReglaAdmin(usuario.empresaId(), request);
    }

    @PostMapping("/reglas/semana")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public List<ReglaDisponibilidadResponse> guardarSemana(@AuthenticationPrincipal UsuarioAutenticado usuario,
       @Valid @RequestBody com.techprotech.agenda.modulos.disponibilidad.api.dto.HorarioSemanalRequest request){
        return horarioSemanal.guardar(usuario.empresaId(),request);
    }

    @PatchMapping("/reglas/{id}")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public ReglaDisponibilidadResponse actualizarRegla(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ReglaDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.actualizarReglaAdmin(usuario.empresaId(), id, request);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/reglas/{id}")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public void eliminarRegla(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        servicioGestionDisponibilidad.eliminarReglaAdmin(usuario.empresaId(), id);
    }

    @GetMapping("/excepciones")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public List<ExcepcionDisponibilidadResponse> excepciones(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioGestionDisponibilidad.listarExcepcionesAdmin(usuario.empresaId());
    }

    @PostMapping("/excepciones")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public ExcepcionDisponibilidadResponse crearExcepcion(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ExcepcionDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.crearExcepcionAdmin(usuario.empresaId(), request);
    }

    @PatchMapping("/excepciones/{id}")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public ExcepcionDisponibilidadResponse actualizarExcepcion(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ExcepcionDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.actualizarExcepcionAdmin(usuario.empresaId(), id, request);
    }
}
