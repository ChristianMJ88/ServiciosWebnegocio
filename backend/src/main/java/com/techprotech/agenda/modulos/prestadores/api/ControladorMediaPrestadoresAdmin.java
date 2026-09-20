package com.techprotech.agenda.modulos.prestadores.api;

import com.techprotech.agenda.compartido.media.api.dto.ArchivoMediaAdminResponse;
import com.techprotech.agenda.compartido.media.api.dto.ConfiguracionMediaAdminResponse;
import com.techprotech.agenda.compartido.media.aplicacion.ServicioMediaEmpresa;
import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;
import com.techprotech.agenda.modulos.prestadores.seguridad.PermisosPrestadores;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/prestadores/media")
@PreAuthorize(PermisosPrestadores.EXPRESION_GESTIONAR)
public class ControladorMediaPrestadoresAdmin {
    private final ServicioMediaEmpresa servicio;

    public ControladorMediaPrestadoresAdmin(ServicioMediaEmpresa servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/configuracion")
    public ConfiguracionMediaAdminResponse obtenerConfiguracion() {
        return servicio.obtenerConfiguracion();
    }

    @PostMapping(value = "/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArchivoMediaAdminResponse subirImagen(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                  @RequestPart("archivo") MultipartFile archivo) {
        return servicio.subirImagen(usuario.empresaId(), usuario.usuarioId(), CategoriaMedia.COLABORADORES, archivo);
    }
}
