package com.techprotech.agenda.modulos.marketing.api;
import com.techprotech.agenda.modulos.marketing.aplicacion.ServicioSoportePlataforma;
import com.techprotech.agenda.modulos.marketing.api.dto.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
@RestController @RequestMapping("/api/v1/publico/plataforma")
public class ControladorContactoPublico {
    private final ServicioSoportePlataforma servicio;
    public ControladorContactoPublico(ServicioSoportePlataforma servicio){this.servicio=servicio;}
    @GetMapping("/soporte") public ResponseEntity<SoportePublicoResponse> soporte(){return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic()).body(servicio.publico());}
    @GetMapping("/contacto-legal") public ResponseEntity<ContactoLegalResponse> legal(){return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic()).body(servicio.contactoLegal());}
}
