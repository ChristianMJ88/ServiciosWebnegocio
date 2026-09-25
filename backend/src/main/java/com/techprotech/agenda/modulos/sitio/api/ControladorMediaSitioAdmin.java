package com.techprotech.agenda.modulos.sitio.api;

import com.techprotech.agenda.compartido.media.api.dto.ArchivoMediaAdminResponse;
import com.techprotech.agenda.compartido.media.api.dto.ConfiguracionMediaAdminResponse;
import com.techprotech.agenda.compartido.media.aplicacion.ServicioMediaEmpresa;
import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;
import com.techprotech.agenda.modulos.sitio.seguridad.PermisosSitio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/configuracion-sitio/media")
@PreAuthorize(PermisosSitio.EXPRESION_PANEL_ADMIN)
public class ControladorMediaSitioAdmin {
    private final ServicioMediaEmpresa servicio;

    public ControladorMediaSitioAdmin(ServicioMediaEmpresa servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/configuracion")
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public ConfiguracionMediaAdminResponse obtenerConfiguracion() {
        return servicio.obtenerConfiguracion();
    }

    @PostMapping(value = "/imagenes/galeria", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public ArchivoMediaAdminResponse subirImagen(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestPart("archivo") MultipartFile archivo
    ) {
        return servicio.subirImagen(usuario.empresaId(), usuario.usuarioId(), CategoriaMedia.GALERIA, archivo);
    }

    @PostMapping(value = "/imagenes/branding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public ArchivoMediaAdminResponse subirImagenBranding(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestPart("archivo") MultipartFile archivo
    ) {
        return servicio.subirImagen(usuario.empresaId(), usuario.usuarioId(), CategoriaMedia.BRANDING, archivo);
    }

    @PostMapping(value = "/imagenes/resenas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public ArchivoMediaAdminResponse subirImagenResena(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestPart("archivo") MultipartFile archivo
    ) {
        return servicio.subirImagen(usuario.empresaId(), usuario.usuarioId(), CategoriaMedia.RESENAS, archivo);
    }

    @PostMapping(value = "/imagenes/promociones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(PermisosSitio.EXPRESION_GESTIONAR)
    public ArchivoMediaAdminResponse subirImagenPromocion(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestPart("archivo") MultipartFile archivo
    ) {
        return servicio.subirImagen(usuario.empresaId(), usuario.usuarioId(), CategoriaMedia.PROMOCIONES, archivo);
    }
}
