package com.techprotech.agenda.modulos.sucursales.api;
import com.techprotech.agenda.modulos.sucursales.aplicacion.ServicioZonaPrecioSucursal;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
@RequestMapping("/api/v1/admin/sucursales/{id}/zona-precio")
@PreAuthorize("hasAuthority('SUCURSALES_GESTIONAR')")
public class ControladorZonaPrecioSucursal {
    private final ServicioZonaPrecioSucursal servicio;
    public ControladorZonaPrecioSucursal(ServicioZonaPrecioSucursal servicio){this.servicio=servicio;}
    public record Solicitud(Long zonaPrecioId) {}
    @GetMapping public ServicioZonaPrecioSucursal.Configuracion consultar(@AuthenticationPrincipal UsuarioAutenticado usuario,@PathVariable Long id){return servicio.consultar(usuario.empresaId(),id);}
    @PatchMapping public ServicioZonaPrecioSucursal.Configuracion guardar(@AuthenticationPrincipal UsuarioAutenticado usuario,@PathVariable Long id,@RequestBody Solicitud request){return servicio.guardar(usuario.empresaId(),id,request.zonaPrecioId());}
}
