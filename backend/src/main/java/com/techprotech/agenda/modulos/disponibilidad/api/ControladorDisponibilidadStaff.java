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
@PreAuthorize("hasRole('STAFF')")
public class ControladorDisponibilidadStaff {

    private final ServicioGestionDisponibilidad servicioGestionDisponibilidad;

    public ControladorDisponibilidadStaff(ServicioGestionDisponibilidad servicioGestionDisponibilidad) {
        this.servicioGestionDisponibilidad = servicioGestionDisponibilidad;
    }

    @GetMapping("/reglas")
    public List<ReglaDisponibilidadResponse> reglas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioGestionDisponibilidad.listarReglasStaff(usuario.empresaId(), usuario.usuarioId());
    }

    @PostMapping("/reglas")
    public ReglaDisponibilidadResponse crearRegla(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ReglaDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.guardarReglaStaff(usuario.empresaId(), usuario.usuarioId(), null, request);
    }

    @PatchMapping("/reglas/{id}")
    public ReglaDisponibilidadResponse actualizarRegla(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ReglaDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.guardarReglaStaff(usuario.empresaId(), usuario.usuarioId(), id, request);
    }

    @GetMapping("/excepciones")
    public List<ExcepcionDisponibilidadResponse> excepciones(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioGestionDisponibilidad.listarExcepcionesStaff(usuario.empresaId(), usuario.usuarioId());
    }

    @PostMapping("/excepciones")
    public ExcepcionDisponibilidadResponse crearExcepcion(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ExcepcionDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.guardarExcepcionStaff(usuario.empresaId(), usuario.usuarioId(), null, request);
    }

    @PatchMapping("/excepciones/{id}")
    public ExcepcionDisponibilidadResponse actualizarExcepcion(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ExcepcionDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.guardarExcepcionStaff(usuario.empresaId(), usuario.usuarioId(), id, request);
    }
}
