package com.techprotech.agenda.modulos.marketplace.api;
import com.techprotech.agenda.modulos.marketplace.aplicacion.ServicioMarketplace;
import com.techprotech.agenda.modulos.marketplace.api.dto.*;
import com.techprotech.agenda.modulos.sitio.seguridad.PermisosSitio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/configuracion-sitio/marketplace")
@PreAuthorize(PermisosSitio.EXPRESION_PANEL_ADMIN+" and "+PermisosSitio.EXPRESION_GESTIONAR)
public class ControladorMarketplaceAdmin {
 private final ServicioMarketplace servicio;
 public ControladorMarketplaceAdmin(ServicioMarketplace servicio){this.servicio=servicio;}
 @GetMapping public ParticipacionMarketplaceResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario){return servicio.obtener(usuario.empresaId());}
 @PutMapping public ParticipacionMarketplaceResponse guardar(@AuthenticationPrincipal UsuarioAutenticado usuario,
  @Valid @RequestBody ParticipacionMarketplaceRequest request){return servicio.actualizar(usuario.empresaId(),request.participante());}
}
