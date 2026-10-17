package com.techprotech.agenda.modulos.politicas.api;

import com.techprotech.agenda.modulos.politicas.api.dto.*;
import com.techprotech.agenda.modulos.politicas.aplicacion.ServicioPoliticaEmpresa;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/politica")
@PreAuthorize("hasAuthority('PARAMETROS_SISTEMA_GESTIONAR')")
@RequiredArgsConstructor
public class ControladorPoliticaAdmin {
    private final ServicioPoliticaEmpresa servicio;
    @GetMapping public PoliticaAdminResponse obtener(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return servicio.obtener(actor.empresaId());
    }
    @PostMapping("/vista-previa") public Map<String,String> vistaPrevia(@Valid @RequestBody PublicarPoliticaRequest r) {
        return Map.of("texto", servicio.vistaPrevia(r));
    }
    @PostMapping("/versiones") public PoliticaAdminResponse publicar(@AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody PublicarPoliticaRequest r) { return servicio.publicar(actor, r); }
}
