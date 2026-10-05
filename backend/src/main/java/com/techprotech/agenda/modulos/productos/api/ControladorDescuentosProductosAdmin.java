package com.techprotech.agenda.modulos.productos.api;

import static com.techprotech.agenda.modulos.plataforma.dominio.CodigosModuloPlataforma.PRODUCTOS;
import com.techprotech.agenda.modulos.productos.api.dto.*;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioDescuentosProductos;
import com.techprotech.agenda.modulos.productos.seguridad.PermisosProductos;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import com.techprotech.agenda.modulos.plataforma.seguridad.RequiereModulo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/productos/descuentos")
@PreAuthorize(PermisosProductos.EXPRESION_GESTIONAR)
@RequiereModulo(PRODUCTOS)
public class ControladorDescuentosProductosAdmin {
    private final ServicioDescuentosProductos servicio;
    public ControladorDescuentosProductosAdmin(ServicioDescuentosProductos servicio){this.servicio=servicio;}
    @GetMapping public CatalogoDescuentosProductoResponse catalogo(@AuthenticationPrincipal UsuarioAutenticado u){return servicio.catalogo(u.empresaId());}
    @PostMapping public ReglaDescuentoProductoResponse crear(@AuthenticationPrincipal UsuarioAutenticado u,@Valid @RequestBody ReglaDescuentoProductoRequest r){return servicio.crear(u.empresaId(),r);}
    @PatchMapping("/{id}") public ReglaDescuentoProductoResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado u,@PathVariable Long id,@Valid @RequestBody ReglaDescuentoProductoRequest r){return servicio.actualizar(u.empresaId(),id,r);}
}
