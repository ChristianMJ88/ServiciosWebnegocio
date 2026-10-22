package com.techprotech.agenda.modulos.dominios.api;
import com.techprotech.agenda.modulos.dominios.aplicacion.ServicioDominios;
import com.techprotech.agenda.modulos.dominios.api.dto.*;
import com.techprotech.agenda.modulos.dominios.infraestructura.entidad.EstadoDominio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.web.PagedModel;
@RestController @RequestMapping("/api/v1/plataforma/dominios")
@PreAuthorize("hasAuthority('PLATAFORMA_DOMINIOS_GESTIONAR')")
public class ControladorDominiosPlataforma {
 private final ServicioDominios servicio;
 public ControladorDominiosPlataforma(ServicioDominios s){servicio=s;}
 @GetMapping public PagedModel<DominioPlataformaResponse> cola(@RequestParam(required=false) EstadoDominio estado,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamano){return new PagedModel<>(servicio.cola(estado,pagina,tamano));}
 @PutMapping("/{id}/conexion") public DominioResponse conectar(@AuthenticationPrincipal UsuarioAutenticado a,@PathVariable Long id,@Valid @RequestBody ConexionDominioSolicitud s){return servicio.conectar(a.usuarioId(),id,s);}
 @PostMapping("/{id}/verificar") public DominioResponse verificar(@AuthenticationPrincipal UsuarioAutenticado a,@PathVariable Long id,@Valid @RequestBody VersionDominioSolicitud s){return servicio.comprobarPlataforma(a.usuarioId(),id,s.version());}
 @PostMapping("/{id}/retirar") public DominioResponse retirar(@AuthenticationPrincipal UsuarioAutenticado a,@PathVariable Long id,@Valid @RequestBody VersionDominioSolicitud s){return servicio.retirarPlataforma(a.usuarioId(),id,s.version());}
}
