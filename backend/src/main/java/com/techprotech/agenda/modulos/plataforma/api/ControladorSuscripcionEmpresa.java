package com.techprotech.agenda.modulos.plataforma.api;

import com.techprotech.agenda.modulos.plataforma.api.dto.CrearCheckoutSuscripcionRequest;
import com.techprotech.agenda.modulos.plataforma.api.dto.EstadoSuscripcionEmpresaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.RedireccionStripeResponse;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioStripePlataforma;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/suscripcion")
public class ControladorSuscripcionEmpresa {
    private final ServicioStripePlataforma stripe;
    public ControladorSuscripcionEmpresa(ServicioStripePlataforma stripe){this.stripe=stripe;}

    @GetMapping
    @PreAuthorize("hasAuthority('SUSCRIPCION_EMPRESA_GESTIONAR')")
    public EstadoSuscripcionEmpresaResponse estado(@AuthenticationPrincipal UsuarioAutenticado usuario){
        return stripe.estadoSuscripcion(usuario.empresaId());
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAuthority('SUSCRIPCION_EMPRESA_GESTIONAR')")
    public RedireccionStripeResponse checkout(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CrearCheckoutSuscripcionRequest solicitud){
        return stripe.crearCheckout(usuario.empresaId(),usuario.usuarioId(),solicitud);
    }

    @PostMapping("/portal")
    @PreAuthorize("hasAuthority('SUSCRIPCION_EMPRESA_GESTIONAR')")
    public RedireccionStripeResponse portal(@AuthenticationPrincipal UsuarioAutenticado usuario){
        return stripe.crearPortal(usuario.empresaId());
    }
}
