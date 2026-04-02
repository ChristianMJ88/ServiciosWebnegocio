package com.techprotech.agenda.modulos.recepcion.api;

import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.CatalogoRecepcionResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.CitaRecepcionResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.ClienteRecepcionResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.CrearCitaRecepcionRequest;
import com.techprotech.agenda.modulos.recepcion.api.dto.CrearSolicitudEsperaRecepcionRequest;
import com.techprotech.agenda.modulos.recepcion.api.dto.FranjaRecepcionDisponibleResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.ReagendarRecepcionRequest;
import com.techprotech.agenda.modulos.recepcion.api.dto.SolicitudEsperaRecepcionResponse;
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
@PreAuthorize("hasAuthority('RECEPCION_ACCESO')")
public class ControladorRecepcion {

    private final ServicioRecepcion servicioRecepcion;

    public ControladorRecepcion(ServicioRecepcion servicioRecepcion) {
        this.servicioRecepcion = servicioRecepcion;
    }

    @GetMapping("/catalogo")
    @PreAuthorize("hasAuthority('RECEPCION_ACCESO')")
    public CatalogoRecepcionResponse catalogo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long sucursalId
    ) {
        return servicioRecepcion.catalogo(usuario.empresaId(), usuario.sucursalesPermitidas(), sucursalId);
    }

    @GetMapping("/agenda")
    @PreAuthorize("hasAuthority('RECEPCION_ACCESO')")
    public List<CitaRecepcionResponse> agenda(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long sucursalId
    ) {
        return servicioRecepcion.agenda(usuario.empresaId(), usuario.sucursalesPermitidas(), fecha, sucursalId);
    }

    @GetMapping("/clientes")
    @PreAuthorize("hasAuthority('RECEPCION_CLIENTES_VER')")
    public List<ClienteRecepcionResponse> clientes(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam String texto
    ) {
        return servicioRecepcion.buscarClientes(usuario.empresaId(), texto);
    }

    @GetMapping("/franjas-disponibles")
    @PreAuthorize("hasAuthority('RECEPCION_CITAS_GESTIONAR')")
    public List<FranjaRecepcionDisponibleResponse> franjasDisponibles(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam Long sucursalId,
            @RequestParam Long servicioId,
            @RequestParam(required = false) Long prestadorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return servicioRecepcion.franjasDisponibles(
                usuario.empresaId(),
                usuario.sucursalesPermitidas(),
                sucursalId,
                servicioId,
                prestadorId,
                fecha
        );
    }

    @PostMapping("/citas")
    @PreAuthorize("hasAuthority('RECEPCION_CITAS_GESTIONAR')")
    public CitaCreadaResponse crearCita(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CrearCitaRecepcionRequest request
    ) {
        return servicioRecepcion.crearCita(usuario.empresaId(), usuario.sucursalesPermitidas(), request);
    }

    @GetMapping("/espera")
    @PreAuthorize("hasAuthority('RECEPCION_CITAS_GESTIONAR')")
    public List<SolicitudEsperaRecepcionResponse> solicitudesEspera(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long sucursalId
    ) {
        return servicioRecepcion.solicitudesEspera(usuario.empresaId(), usuario.sucursalesPermitidas(), fecha, sucursalId);
    }

    @PostMapping("/espera")
    @PreAuthorize("hasAuthority('RECEPCION_CITAS_GESTIONAR')")
    public SolicitudEsperaRecepcionResponse registrarEspera(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CrearSolicitudEsperaRecepcionRequest request
    ) {
        return servicioRecepcion.registrarEspera(
                usuario.empresaId(),
                usuario.usuarioId(),
                usuario.sucursalesPermitidas(),
                request
        );
    }

    @PatchMapping("/espera/{solicitudId}/notificar")
    @PreAuthorize("hasAuthority('RECEPCION_CITAS_GESTIONAR')")
    public SolicitudEsperaRecepcionResponse notificarEspera(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long solicitudId
    ) {
        return servicioRecepcion.notificarEspera(
                usuario.empresaId(),
                usuario.usuarioId(),
                usuario.sucursalesPermitidas(),
                solicitudId
        );
    }

    @PatchMapping("/citas/{citaId}/check-in")
    @PreAuthorize("hasAuthority('RECEPCION_CHECKIN')")
    public CitaRecepcionResponse checkIn(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        return servicioRecepcion.checkIn(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), citaId);
    }

    @PatchMapping("/citas/{citaId}/confirmar")
    @PreAuthorize("hasAuthority('RECEPCION_CITAS_GESTIONAR')")
    public CitaRecepcionResponse confirmar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        return servicioRecepcion.confirmar(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), citaId);
    }

    @PatchMapping("/citas/{citaId}/cancelar")
    @PreAuthorize("hasAuthority('RECEPCION_CITAS_GESTIONAR')")
    public CitaRecepcionResponse cancelar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        return servicioRecepcion.cancelar(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), citaId);
    }

    @PatchMapping("/citas/{citaId}/reagendar")
    @PreAuthorize("hasAuthority('RECEPCION_CITAS_GESTIONAR')")
    public CitaClienteResponse reagendar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId,
            @Valid @RequestBody ReagendarRecepcionRequest request
    ) {
        return servicioRecepcion.reprogramar(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), citaId, request);
    }

    @PatchMapping("/citas/{citaId}/finalizar")
    @PreAuthorize("hasAuthority('RECEPCION_CITAS_GESTIONAR')")
    public CitaRecepcionResponse finalizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        return servicioRecepcion.finalizar(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), citaId);
    }
}
