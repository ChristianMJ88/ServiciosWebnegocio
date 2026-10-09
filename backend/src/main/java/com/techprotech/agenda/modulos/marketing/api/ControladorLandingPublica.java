package com.techprotech.agenda.modulos.marketing.api;
import com.techprotech.agenda.modulos.marketing.aplicacion.ServicioLandingMarketing;
import com.techprotech.agenda.modulos.marketing.api.dto.LandingMarketingDto;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/publico/marketing/landings")
public class ControladorLandingPublica {
 private final ServicioLandingMarketing servicio;
 public ControladorLandingPublica(ServicioLandingMarketing servicio){this.servicio=servicio;}
 @GetMapping("/indice") public List<String> indice(){return servicio.indice();}
 @GetMapping("/{slug}") public LandingMarketingDto obtener(@PathVariable String slug){return servicio.publica(slug);}
}
