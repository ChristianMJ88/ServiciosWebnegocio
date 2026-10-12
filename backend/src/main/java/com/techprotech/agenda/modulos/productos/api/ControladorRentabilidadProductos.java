package com.techprotech.agenda.modulos.productos.api;
import com.techprotech.agenda.modulos.productos.api.dto.RentabilidadProductosResponse;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioRentabilidadProductos;
import com.techprotech.agenda.modulos.productos.seguridad.PermisosProductos;
import com.techprotech.agenda.modulos.plataforma.seguridad.RequiereModulo;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import static com.techprotech.agenda.modulos.plataforma.dominio.CodigosModuloPlataforma.PRODUCTOS;
@RestController
@RequestMapping("/api/v1/admin/productos/rentabilidad")
@PreAuthorize(PermisosProductos.EXPRESION_GESTIONAR)
@RequiereModulo(PRODUCTOS)
public class ControladorRentabilidadProductos {
    private final ServicioRentabilidadProductos servicio;
    private final com.techprotech.agenda.modulos.productos.aplicacion.ServicioUtilidadRealizada realizadas;
    public ControladorRentabilidadProductos(ServicioRentabilidadProductos servicio, com.techprotech.agenda.modulos.productos.aplicacion.ServicioUtilidadRealizada realizadas) { this.servicio = servicio; this.realizadas=realizadas; }
    @GetMapping
    public RentabilidadProductosResponse consultar(@AuthenticationPrincipal UsuarioAutenticado usuario,
        @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="") String busqueda) {
        return servicio.consultar(usuario.empresaId(), pagina, busqueda);
    }
    @GetMapping("/realizada")
    public com.techprotech.agenda.modulos.productos.api.dto.UtilidadRealizadaResponse realizada(
        @AuthenticationPrincipal UsuarioAutenticado usuario,
        @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
        @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta,
        @RequestParam(required=false) Long sucursalId, @RequestParam(defaultValue="") String busqueda,
        @RequestParam(defaultValue="0") int pagina) {
        return realizadas.consultar(usuario.empresaId(),desde,hasta,sucursalId,busqueda,pagina);
    }
}
