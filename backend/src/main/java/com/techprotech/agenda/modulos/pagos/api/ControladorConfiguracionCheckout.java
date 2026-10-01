package com.techprotech.agenda.modulos.pagos.api;

import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutPublicaResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.ConfigurarMetodoPagoCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.api.dto.MetodoPagoCheckoutResponse;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioConfiguracionCheckout;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioMetodosPagoCheckout;
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

import java.util.List;

@RestController
public class ControladorConfiguracionCheckout {
    private final ServicioConfiguracionCheckout servicio;
    private final ServicioMetodosPagoCheckout metodos;

    public ControladorConfiguracionCheckout(ServicioConfiguracionCheckout servicio, ServicioMetodosPagoCheckout metodos) {
        this.servicio = servicio;
        this.metodos = metodos;
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

    @GetMapping("/api/v1/admin/pagos/metodos")
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public List<MetodoPagoCheckoutResponse> listarMetodos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return metodos.listarAdmin(usuario.empresaId());
    }

    @PutMapping("/api/v1/admin/pagos/metodos")
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public MetodoPagoCheckoutResponse configurarMetodo(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                        @Valid @RequestBody ConfigurarMetodoPagoCheckoutRequest request) {
        return metodos.configurar(usuario.empresaId(), request);
    }

    @GetMapping("/api/v1/publico/empresas/{slug}/checkout/metodos-pago")
    public List<MetodoPagoCheckoutResponse> listarMetodosPublicos(@PathVariable String slug) {
        return metodos.listarPublicos(slug);
    }
}
