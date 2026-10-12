package com.techprotech.agenda.modulos.productos.api;
import com.techprotech.agenda.modulos.productos.api.dto.RevisionImportacionProductos;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioImportacionProductos;
import com.techprotech.agenda.modulos.productos.seguridad.PermisosProductos;
import com.techprotech.agenda.modulos.plataforma.seguridad.RequiereModulo;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import static com.techprotech.agenda.modulos.plataforma.dominio.CodigosModuloPlataforma.PRODUCTOS;
@RestController
@RequestMapping("/api/v1/admin/productos/importacion")
@PreAuthorize(PermisosProductos.EXPRESION_GESTIONAR)
@RequiereModulo(PRODUCTOS)
public class ControladorImportacionProductos {
    private final ServicioImportacionProductos servicio;
    public ControladorImportacionProductos(ServicioImportacionProductos servicio){this.servicio=servicio;}
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla(@AuthenticationPrincipal UsuarioAutenticado usuario){
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=productos.xlsx")
          .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .body(servicio.plantilla(usuario.empresaId()));
    }
    @PostMapping(value="/revision",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public RevisionImportacionProductos revisar(@AuthenticationPrincipal UsuarioAutenticado usuario,@RequestPart MultipartFile archivo){return servicio.revisar(usuario.empresaId(),archivo);}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Resultado importar(@AuthenticationPrincipal UsuarioAutenticado usuario,@RequestPart MultipartFile archivo){return new Resultado(servicio.importar(usuario.empresaId(),archivo));}
    public record Resultado(int creados){}
}
