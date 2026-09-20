package com.techprotech.agenda.modulos.servicios.api;

import com.techprotech.agenda.compartido.media.api.dto.ArchivoMediaAdminResponse;
import com.techprotech.agenda.compartido.media.api.dto.ConfiguracionMediaAdminResponse;
import com.techprotech.agenda.compartido.media.aplicacion.ServicioMediaEmpresa;
import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;
import com.techprotech.agenda.modulos.servicios.seguridad.PermisosServicios;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/catalogo/media")
@PreAuthorize(PermisosServicios.EXPRESION_GESTIONAR)
public class ControladorMediaServiciosAdmin {
    private final ServicioMediaEmpresa servicio;

    public ControladorMediaServiciosAdmin(ServicioMediaEmpresa servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/configuracion")
    public ConfiguracionMediaAdminResponse obtenerConfiguracion() {
        return servicio.obtenerConfiguracion();
    }

    @PostMapping(value = "/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArchivoMediaAdminResponse subirImagen(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                  @RequestPart("archivo") MultipartFile archivo) {
        return servicio.subirImagen(usuario.empresaId(), usuario.usuarioId(), CategoriaMedia.SERVICIOS, archivo);
    }
}
