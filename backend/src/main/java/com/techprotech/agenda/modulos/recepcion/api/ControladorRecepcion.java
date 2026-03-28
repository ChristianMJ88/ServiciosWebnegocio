package com.techprotech.agenda.modulos.recepcion.api;

import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.CitaRecepcionResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.ClienteRecepcionResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.CrearCitaRecepcionRequest;
import com.techprotech.agenda.modulos.recepcion.api.dto.ReagendarRecepcionRequest;
import com.techprotech.agenda.modulos.recepcion.aplicacion.ServicioRecepcion;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/recepcion")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
public class ControladorRecepcion {

    private final ServicioRecepcion servicioRecepcion;

    public ControladorRecepcion(ServicioRecepcion servicioRecepcion) {
        this.servicioRecepcion = servicioRecepcion;
    }

    @GetMapping("/agenda")
    public List<CitaRecepcionResponse> agenda(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long sucursalId
    ) {
        return servicioRecepcion.agenda(usuario.empresaId(), fecha, sucursalId);
    }

    @GetMapping("/clientes")
    public List<ClienteRecepcionResponse> clientes(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam String texto
    ) {
        return servicioRecepcion.buscarClientes(usuario.empresaId(), texto);
    }

    @PostMapping("/citas")
    public CitaCreadaResponse crearCita(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CrearCitaRecepcionRequest request
    ) {
        return servicioRecepcion.crearCita(usuario.empresaId(), request);
    }

    @PatchMapping("/citas/{citaId}/check-in")
    public CitaRecepcionResponse checkIn(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        return servicioRecepcion.checkIn(usuario.empresaId(), usuario.usuarioId(), citaId);
    }

    @PatchMapping("/citas/{citaId}/confirmar")
    public CitaRecepcionResponse confirmar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        return servicioRecepcion.confirmar(usuario.empresaId(), usuario.usuarioId(), citaId);
    }

    @PatchMapping("/citas/{citaId}/cancelar")
    public CitaRecepcionResponse cancelar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        return servicioRecepcion.cancelar(usuario.empresaId(), usuario.usuarioId(), citaId);
    }

    @PatchMapping("/citas/{citaId}/reagendar")
    public CitaClienteResponse reagendar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId,
            @Valid @RequestBody ReagendarRecepcionRequest request
    ) {
        return servicioRecepcion.reprogramar(usuario.empresaId(), usuario.usuarioId(), citaId, request);
    }

    @PatchMapping("/citas/{citaId}/finalizar")
    public CitaRecepcionResponse finalizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        return servicioRecepcion.finalizar(usuario.empresaId(), usuario.usuarioId(), citaId);
    }
}
