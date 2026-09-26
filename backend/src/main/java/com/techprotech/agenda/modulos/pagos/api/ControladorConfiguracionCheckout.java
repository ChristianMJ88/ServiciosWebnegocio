package com.techprotech.agenda.modulos.pagos.api;

import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutPublicaResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutResponse;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioConfiguracionCheckout;
import com.techprotech.agenda.modulos.pagos.seguridad.PermisosPagos;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorConfiguracionCheckout {
    private final ServicioConfiguracionCheckout servicio;

    public ControladorConfiguracionCheckout(ServicioConfiguracionCheckout servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/api/v1/admin/pagos/configuracion")
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public ConfiguracionCheckoutResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.obtener(usuario.empresaId());
    }

    @PutMapping("/api/v1/admin/pagos/configuracion")
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public ConfiguracionCheckoutResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                     @Valid @RequestBody ConfiguracionCheckoutRequest request) {
        return servicio.actualizar(usuario.empresaId(), request);
    }

    @GetMapping("/api/v1/publico/empresas/{slug}/checkout/configuracion")
    public ConfiguracionCheckoutPublicaResponse obtenerPublica(@PathVariable String slug) {
        return servicio.obtenerPublica(slug);
    }
}
