package com.techprotech.agenda.modulos.productos.api;

import com.techprotech.agenda.modulos.productos.api.dto.*;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioInventarioProductos;
import com.techprotech.agenda.modulos.productos.seguridad.PermisosProductos;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import com.techprotech.agenda.modulos.plataforma.seguridad.RequiereModulo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/inventario-productos")
@PreAuthorize(PermisosProductos.EXPRESION_INVENTARIO)
@RequiereModulo("INVENTARIO")
public class ControladorInventarioProductosAdmin {
    private final ServicioInventarioProductos inventario;

    public ControladorInventarioProductosAdmin(ServicioInventarioProductos inventario) { this.inventario = inventario; }

    @GetMapping
    public CatalogoInventarioResponse catalogo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return inventario.obtenerCatalogo(usuario.empresaId(), usuario.sucursalesPermitidas());
    }

    @PostMapping("/ajustes")
    public ExistenciaProductoResponse ajustar(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody AjusteInventarioRequest request) {
        return inventario.ajustar(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), request);
    }

    @PostMapping("/transferencias")
    public List<ExistenciaProductoResponse> transferir(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody TransferenciaInventarioRequest request) {
        return inventario.transferir(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), request);
    }
}
