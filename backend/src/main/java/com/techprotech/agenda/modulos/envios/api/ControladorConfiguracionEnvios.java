package com.techprotech.agenda.modulos.envios.api;

import com.techprotech.agenda.modulos.envios.api.dto.ConfiguracionEnvioRequest;
import com.techprotech.agenda.modulos.envios.api.dto.ConfiguracionEnvioResponse;
import com.techprotech.agenda.modulos.envios.aplicacion.ServicioConfiguracionEnvios;
import com.techprotech.agenda.modulos.envios.seguridad.PermisosEnvios;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/envios/configuracion")
@PreAuthorize(PermisosEnvios.EXPRESION_CONFIGURAR)
public class ControladorConfiguracionEnvios {
    private final ServicioConfiguracionEnvios servicio;

    public ControladorConfiguracionEnvios(ServicioConfiguracionEnvios servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ConfiguracionEnvioResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.obtener(usuario.empresaId());
    }

    @PutMapping
    public ConfiguracionEnvioResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                 @Valid @RequestBody ConfiguracionEnvioRequest request) {
        return servicio.actualizar(usuario.empresaId(), request);
    }
}
