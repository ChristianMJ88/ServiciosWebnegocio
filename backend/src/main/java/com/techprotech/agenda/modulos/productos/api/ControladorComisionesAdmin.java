package com.techprotech.agenda.modulos.productos.api;

import com.techprotech.agenda.modulos.plataforma.seguridad.RequiereModulo;
import com.techprotech.agenda.modulos.productos.api.dto.*;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisiones;
import com.techprotech.agenda.modulos.productos.seguridad.PermisosComisiones;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/comisiones")
@PreAuthorize(PermisosComisiones.EXPRESION)
@RequiereModulo("COMISIONES")
public class ControladorComisionesAdmin {
    private final ServicioComisiones servicio;
    public ControladorComisionesAdmin(ServicioComisiones servicio){this.servicio=servicio;}
    @GetMapping public CatalogoComisionesResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario){return servicio.catalogo(usuario.empresaId());}
    @PostMapping public ReglaComisionResponse crear(@AuthenticationPrincipal UsuarioAutenticado usuario,@Valid @RequestBody ReglaComisionRequest request){return servicio.crear(usuario.empresaId(),request);}
    @PatchMapping("/{id}") public ReglaComisionResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,@PathVariable Long id,@Valid @RequestBody ReglaComisionRequest request){return servicio.actualizar(usuario.empresaId(),id,request);}
}
