package com.techprotech.agenda.modulos.pagos.api;

import com.techprotech.agenda.modulos.pagos.api.dto.ConexionProveedorPagoResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.InicioOAuthPagoResponse;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioOAuthMercadoPago;
import com.techprotech.agenda.modulos.pagos.seguridad.PermisosPagos;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/pagos/mercado-pago/oauth")
public class ControladorOAuthMercadoPago {
    private final ServicioOAuthMercadoPago servicio;

    public ControladorOAuthMercadoPago(ServicioOAuthMercadoPago servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/estado")
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public ConexionProveedorPagoResponse estado(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.estado(usuario.empresaId());
    }

    @GetMapping("/iniciar")
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public InicioOAuthPagoResponse iniciar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.iniciar(usuario.empresaId(), usuario.usuarioId());
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam String state,
            @RequestParam(required = false) String error
    ) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(servicio.completar(code, state, error)).toString())
                .build();
    }

    @DeleteMapping
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public ConexionProveedorPagoResponse desconectar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.desconectar(usuario.empresaId());
    }
}
