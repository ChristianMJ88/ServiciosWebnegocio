package com.techprotech.agenda.modulos.plataforma.api;

import com.techprotech.agenda.modulos.plataforma.api.dto.CrearCheckoutSuscripcionRequest;
import com.techprotech.agenda.modulos.plataforma.api.dto.CapacidadesEmpresaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.EstadoSuscripcionEmpresaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.RedireccionStripeResponse;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioStripePlataforma;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/suscripcion")
public class ControladorSuscripcionEmpresa {
    private final ServicioStripePlataforma stripe;
    private final ServicioCapacidadesEmpresa capacidades;
    public ControladorSuscripcionEmpresa(ServicioStripePlataforma stripe,ServicioCapacidadesEmpresa capacidades){
        this.stripe=stripe;this.capacidades=capacidades;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUSCRIPCION_EMPRESA_GESTIONAR')")
    public EstadoSuscripcionEmpresaResponse estado(@AuthenticationPrincipal UsuarioAutenticado usuario){
        return stripe.estadoSuscripcion(usuario.empresaId());
    }

    @GetMapping("/capacidades")
    @PreAuthorize("isAuthenticated()")
    public CapacidadesEmpresaResponse capacidades(@AuthenticationPrincipal UsuarioAutenticado usuario){
        return capacidades.obtener(usuario.empresaId());
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
