package com.techprotech.agenda.modulos.marketing.api;
import com.techprotech.agenda.modulos.marketing.aplicacion.ServicioLandingMarketing;
import com.techprotech.agenda.modulos.marketing.api.dto.LandingMarketingDto;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import java.util.List;
@RestController @RequestMapping("/api/v1/plataforma/marketing/landings")
@PreAuthorize("hasAuthority('PLATAFORMA_MARKETING_GESTIONAR')")
public class ControladorLandingPlataforma {
 private final ServicioLandingMarketing servicio;
 public ControladorLandingPlataforma(ServicioLandingMarketing servicio){this.servicio=servicio;}
 @GetMapping public List<LandingMarketingDto> listar(){return servicio.listar();}
 @PutMapping("/{slug}") public LandingMarketingDto guardar(@PathVariable String slug,@Valid @RequestBody LandingMarketingDto solicitud,@AuthenticationPrincipal UsuarioAutenticado usuario){return servicio.guardar(slug,solicitud,usuario.usuarioId());}
}
