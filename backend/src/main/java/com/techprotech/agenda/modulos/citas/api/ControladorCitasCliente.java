package com.techprotech.agenda.modulos.citas.api;

import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.citas.api.dto.ReprogramarCitaClienteRequest;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitasCliente;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cliente/citas")
@PreAuthorize("hasRole('CLIENTE')")
public class ControladorCitasCliente {

    private final ServicioCitasCliente servicioCitasCliente;

    public ControladorCitasCliente(ServicioCitasCliente servicioCitasCliente) {
        this.servicioCitasCliente = servicioCitasCliente;
    }

    @GetMapping
    public List<CitaClienteResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioCitasCliente.listarMisCitas(usuario.empresaId(), usuario.usuarioId());
    }

    @PatchMapping("/{citaId}/confirmar")
    public CitaClienteResponse confirmar(
            @PathVariable Long citaId,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return servicioCitasCliente.confirmar(usuario.empresaId(), usuario.usuarioId(), citaId);
    }

    @PatchMapping("/{citaId}/cancelar")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long citaId,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        servicioCitasCliente.cancelar(usuario.empresaId(), usuario.usuarioId(), citaId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{citaId}/reprogramar")
    public CitaClienteResponse reprogramar(
            @PathVariable Long citaId,
            @Valid @RequestBody ReprogramarCitaClienteRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return servicioCitasCliente.reprogramar(
                usuario.empresaId(),
                usuario.usuarioId(),
                citaId,
                request.nuevoInicio()
        );
    }
}
