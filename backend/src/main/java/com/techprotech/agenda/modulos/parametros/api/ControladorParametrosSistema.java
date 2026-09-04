package com.techprotech.agenda.modulos.parametros.api;
import com.techprotech.agenda.modulos.parametros.api.dto.*;
import com.techprotech.agenda.modulos.parametros.aplicacion.*;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/parametros-sistema")
@PreAuthorize("hasAuthority('PARAMETROS_SISTEMA_GESTIONAR')")
public class ControladorParametrosSistema {
    private final ServicioParametrosSistema servicio;
    public ControladorParametrosSistema(ServicioParametrosSistema servicio) { this.servicio = servicio; }
    @GetMapping public List<ParametroSistemaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u) { return servicio.listar(u.empresaId()).stream().map(this::respuesta).toList(); }
    @PutMapping("/{clave}") public ParametroSistemaResponse guardar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable String clave, @Valid @RequestBody GuardarParametroSistemaRequest r) { return respuesta(servicio.guardar(u.empresaId(), u.usuarioId(), clave, r.valor())); }
    @DeleteMapping("/{clave}") public ParametroSistemaResponse restablecer(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable String clave) { return respuesta(servicio.restablecer(u.empresaId(), clave)); }
    private ParametroSistemaResponse respuesta(ParametroSistema p) { return new ParametroSistemaResponse(p.clave(), p.nombre(), p.descripcion(), p.tipo(), p.valor(), p.valorPredeterminado(), p.categoria(), p.opciones(), p.personalizado(), p.editable(), p.actualizadoEn()); }
}
