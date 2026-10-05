package com.techprotech.agenda.modulos.pagos.api;

import static com.techprotech.agenda.modulos.plataforma.dominio.CodigosModuloPlataforma.PAGOS_ONLINE;
import com.techprotech.agenda.modulos.pagos.api.dto.ConexionProveedorPagoResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.InicioOAuthPagoResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.WebhookMercadoPagoResponse;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioOAuthMercadoPago;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.PropiedadesMercadoPago;
import com.techprotech.agenda.modulos.pagos.seguridad.PermisosPagos;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import com.techprotech.agenda.modulos.plataforma.seguridad.RequiereModulo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/pagos/mercado-pago/oauth")
@RequiereModulo(PAGOS_ONLINE)
public class ControladorOAuthMercadoPago {
  private final ServicioOAuthMercadoPago servicio;
  private final PropiedadesMercadoPago propiedades;

  public ControladorOAuthMercadoPago(ServicioOAuthMercadoPago servicio, PropiedadesMercadoPago propiedades) {
    this.servicio = servicio;
    this.propiedades = propiedades;
  }

  @GetMapping("/webhook")
  @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
  public WebhookMercadoPagoResponse webhook() {
    String base = propiedades.webhookPublicBaseUrl();
    if (base == null || base.isBlank()) return new WebhookMercadoPagoResponse(null, false);
    String limpia = base.trim().replaceAll("/+$", "");
    return new WebhookMercadoPagoResponse(
      limpia + "/api/v1/publico/pagos/mercado-pago/webhook", true);
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

  @PostMapping("/prueba")
  @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
  public ConexionProveedorPagoResponse conectarPruebas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.conectarPruebas(usuario.empresaId());
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
