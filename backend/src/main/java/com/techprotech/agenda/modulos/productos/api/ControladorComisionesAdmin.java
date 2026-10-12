package com.techprotech.agenda.modulos.productos.api;

import static com.techprotech.agenda.modulos.plataforma.dominio.CodigosModuloPlataforma.COMISIONES;
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
@RequiereModulo(COMISIONES)
public class ControladorComisionesAdmin {
    private final ServicioComisiones servicio;
    private final com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisionesDevengadas devengadas;
    public ControladorComisionesAdmin(ServicioComisiones servicio,com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisionesDevengadas devengadas){this.servicio=servicio;this.devengadas=devengadas;}
    @GetMapping("/ganancias") public java.util.List<com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisionesDevengadas.Ganancia> ganancias(@AuthenticationPrincipal UsuarioAutenticado usuario){return devengadas.resumen(usuario.empresaId());}
    @GetMapping public CatalogoComisionesResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario){return servicio.catalogo(usuario.empresaId());}
    @PostMapping public ReglaComisionResponse crear(@AuthenticationPrincipal UsuarioAutenticado usuario,@Valid @RequestBody ReglaComisionRequest request){return servicio.crear(usuario.empresaId(),request);}
    @PatchMapping("/{id}") public ReglaComisionResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,@PathVariable Long id,@Valid @RequestBody ReglaComisionRequest request){return servicio.actualizar(usuario.empresaId(),id,request);}
}
