package com.techprotech.agenda.modulos.dominios.api;
import com.techprotech.agenda.modulos.dominios.aplicacion.ServicioDominios;
import com.techprotech.agenda.modulos.dominios.api.dto.*;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/dominios")
@PreAuthorize("hasAuthority('CONFIGURACION_EMPRESA_GESTIONAR')")
public class ControladorDominiosAdmin {
 private final ServicioDominios servicio;
 public ControladorDominiosAdmin(ServicioDominios s){servicio=s;}
 @GetMapping public PanelDominiosResponse panel(@AuthenticationPrincipal UsuarioAutenticado a){return servicio.panel(a.empresaId());}
 @PostMapping public DominioResponse solicitar(@AuthenticationPrincipal UsuarioAutenticado a,@Valid @RequestBody DominioSolicitud s){return servicio.solicitar(a.empresaId(),a.usuarioId(),s.hostname());}
 @PostMapping("/{id}/verificar") public DominioResponse verificar(@AuthenticationPrincipal UsuarioAutenticado a,@PathVariable Long id,@Valid @RequestBody VersionDominioSolicitud s){return servicio.verificar(a.empresaId(),a.usuarioId(),id,s.version());}
 @PostMapping("/{id}/retirar") public DominioResponse retirar(@AuthenticationPrincipal UsuarioAutenticado a,@PathVariable Long id,@Valid @RequestBody VersionDominioSolicitud s){return servicio.retirar(a.empresaId(),a.usuarioId(),id,s.version(),false);}
}
