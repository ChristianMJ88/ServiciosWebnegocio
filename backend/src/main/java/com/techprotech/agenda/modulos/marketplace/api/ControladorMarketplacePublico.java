package com.techprotech.agenda.modulos.marketplace.api;
import com.techprotech.agenda.modulos.marketplace.aplicacion.ServicioMarketplace;
import com.techprotech.agenda.modulos.marketplace.api.dto.DirectorioResponse;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @Validated @RequestMapping("/api/v1/publico/marketplace")
public class ControladorMarketplacePublico {
 private final ServicioMarketplace servicio;
 public ControladorMarketplacePublico(ServicioMarketplace servicio){this.servicio=servicio;}
 @GetMapping public DirectorioResponse buscar(@RequestParam(defaultValue="") @Size(max=100) String q,
  @RequestParam(defaultValue="0") @Min(0) long cursor){return servicio.buscar(q,cursor);}
}
