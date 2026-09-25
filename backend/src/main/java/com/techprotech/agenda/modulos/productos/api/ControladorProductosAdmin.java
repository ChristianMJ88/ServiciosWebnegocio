package com.techprotech.agenda.modulos.productos.api;

import com.techprotech.agenda.compartido.media.api.dto.ArchivoMediaAdminResponse;
import com.techprotech.agenda.compartido.media.api.dto.ConfiguracionMediaAdminResponse;
import com.techprotech.agenda.compartido.media.aplicacion.ServicioMediaEmpresa;
import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;
import com.techprotech.agenda.modulos.productos.api.dto.*;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioCatalogoProductos;
import com.techprotech.agenda.modulos.productos.seguridad.PermisosProductos;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/productos")
@PreAuthorize(PermisosProductos.EXPRESION_GESTIONAR)
public class ControladorProductosAdmin {
    private final ServicioCatalogoProductos catalogo;
    private final ServicioMediaEmpresa media;

    public ControladorProductosAdmin(ServicioCatalogoProductos catalogo, ServicioMediaEmpresa media) {
        this.catalogo = catalogo;
        this.media = media;
    }

    @GetMapping("/catalogo")
    public CatalogoProductosAdminResponse catalogo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return catalogo.obtenerCatalogo(usuario.empresaId());
    }

    @PostMapping("/categorias")
    public CategoriaProductoResponse crearCategoria(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody CategoriaProductoRequest request) {
        return catalogo.crearCategoria(usuario.empresaId(), request);
    }

    @PatchMapping("/categorias/{id}")
    public CategoriaProductoResponse actualizarCategoria(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id, @Valid @RequestBody CategoriaProductoRequest request) {
        return catalogo.actualizarCategoria(usuario.empresaId(), id, request);
    }

    @PostMapping("/proveedores")
    public ProveedorProductoResponse crearProveedor(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody ProveedorProductoRequest request) {
        return catalogo.crearProveedor(usuario.empresaId(), request);
    }

    @PatchMapping("/proveedores/{id}")
    public ProveedorProductoResponse actualizarProveedor(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id, @Valid @RequestBody ProveedorProductoRequest request) {
        return catalogo.actualizarProveedor(usuario.empresaId(), id, request);
    }

    @PostMapping("/zonas-precio")
    public ZonaPrecioProductoResponse crearZona(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody ZonaPrecioProductoRequest request) {
        return catalogo.crearZona(usuario.empresaId(), request);
    }

    @PatchMapping("/zonas-precio/{id}")
    public ZonaPrecioProductoResponse actualizarZona(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id, @Valid @RequestBody ZonaPrecioProductoRequest request) {
        return catalogo.actualizarZona(usuario.empresaId(), id, request);
    }

    @PostMapping
    public ProductoResponse crearProducto(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody ProductoRequest request) {
        return catalogo.crearProducto(usuario.empresaId(), request);
    }

    @PatchMapping("/{id}")
    public ProductoResponse actualizarProducto(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return catalogo.actualizarProducto(usuario.empresaId(), id, request);
    }

    @GetMapping("/media/configuracion")
    public ConfiguracionMediaAdminResponse configuracionMedia() {
        return media.obtenerConfiguracion();
    }

    @PostMapping(value = "/media/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArchivoMediaAdminResponse subirImagen(@AuthenticationPrincipal UsuarioAutenticado usuario, @RequestPart("archivo") MultipartFile archivo) {
        return media.subirImagen(usuario.empresaId(), usuario.usuarioId(), CategoriaMedia.PRODUCTOS, archivo);
    }
}
