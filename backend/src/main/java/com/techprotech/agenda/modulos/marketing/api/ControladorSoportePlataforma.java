package com.techprotech.agenda.modulos.marketing.api;
import com.techprotech.agenda.modulos.marketing.aplicacion.ServicioSoportePlataforma;
import com.techprotech.agenda.modulos.marketing.api.dto.*;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController @RequestMapping("/api/v1/plataforma/marketing/soporte")
@PreAuthorize("hasAuthority('PLATAFORMA_MARKETING_GESTIONAR')")
public class ControladorSoportePlataforma {
    private final ServicioSoportePlataforma servicio;
    public ControladorSoportePlataforma(ServicioSoportePlataforma servicio){this.servicio=servicio;}
    @GetMapping public SoportePlataformaResponse obtener(){return servicio.obtener();}
    @PutMapping public SoportePlataformaResponse guardar(@Valid @RequestBody SoportePlataformaSolicitud s,@AuthenticationPrincipal UsuarioAutenticado actor){return servicio.guardar(s,actor.usuarioId());}
}
