package com.techprotech.agenda.modulos.caja.api;

import com.techprotech.agenda.modulos.caja.api.dto.AbrirCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.CajaSesionResponse;
import com.techprotech.agenda.modulos.caja.api.dto.CerrarCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.CitaPorCobrarResponse;
import com.techprotech.agenda.modulos.caja.api.dto.MovimientoCajaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.PagoCitaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarMovimientoCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarPagoRequest;
import com.techprotech.agenda.modulos.caja.api.dto.ResumenCajaResponse;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioCaja;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/caja")
@PreAuthorize("hasAnyRole('ADMIN','CAJERO','RECEPCIONISTA')")
public class ControladorCaja {

    private final ServicioCaja servicioCaja;

    public ControladorCaja(ServicioCaja servicioCaja) {
        this.servicioCaja = servicioCaja;
    }

    @PostMapping("/sesiones/abrir")
    public CajaSesionResponse abrirCaja(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody AbrirCajaRequest request
    ) {
        return servicioCaja.abrirSesion(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @PostMapping("/sesiones/{id}/cerrar")
    public CajaSesionResponse cerrarCaja(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody CerrarCajaRequest request
    ) {
        return servicioCaja.cerrarSesion(usuario.empresaId(), usuario.usuarioId(), id, request);
    }

    @GetMapping("/sesiones/actual")
    public CajaSesionResponse sesionActual(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long sucursalId
    ) {
        return servicioCaja.obtenerSesionActual(usuario.empresaId(), sucursalId);
    }

    @GetMapping("/citas-por-cobrar")
    public List<CitaPorCobrarResponse> citasPorCobrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long sucursalId
    ) {
        return servicioCaja.listarCitasPorCobrar(usuario.empresaId(), sucursalId);
    }

    @PostMapping("/citas/{citaId}/pagos")
    public PagoCitaResponse registrarPago(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId,
            @Valid @RequestBody RegistrarPagoRequest request
    ) {
        return servicioCaja.registrarPago(usuario.empresaId(), usuario.usuarioId(), citaId, request);
    }

    @GetMapping("/citas/{citaId}/pagos")
    public List<PagoCitaResponse> pagosCita(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        return servicioCaja.listarPagosCita(usuario.empresaId(), citaId);
    }

    @PostMapping("/movimientos")
    public MovimientoCajaResponse registrarMovimiento(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody RegistrarMovimientoCajaRequest request
    ) {
        return servicioCaja.registrarMovimiento(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @GetMapping("/resumen")
    public ResumenCajaResponse resumen(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long sucursalId
    ) {
        return servicioCaja.resumen(usuario.empresaId(), sucursalId);
    }
}
