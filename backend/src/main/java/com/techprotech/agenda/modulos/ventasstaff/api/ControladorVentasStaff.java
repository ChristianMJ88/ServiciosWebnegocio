package com.techprotech.agenda.modulos.ventasstaff.api;

import com.techprotech.agenda.modulos.ventasstaff.aplicacion.ServicioVentasStaff;
import com.techprotech.agenda.modulos.ventasstaff.infraestructura.VentasStaffRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/ventas")
public class ControladorVentasStaff {
    private final ServicioVentasStaff servicio;
    private final com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa capacidades;
    public ControladorVentasStaff(ServicioVentasStaff servicio,com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa capacidades) { this.servicio=servicio;this.capacidades=capacidades; }
    public record Modulo(String id,String titulo,String descripcion,String icono) {}
    @GetMapping("/modulos")
    @PreAuthorize("hasAuthority('STAFF_PANEL_ACCESO')")
    public java.util.List<Modulo> modulos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        var disponibles=new java.util.ArrayList<Modulo>();
        if(usuario.tienePermiso("STAFF_VENTAS_REGISTRAR") && capacidades.obtener(usuario.empresaId()).modulos().stream().anyMatch(m->com.techprotech.agenda.modulos.plataforma.dominio.CodigosModuloPlataforma.PRODUCTOS.equals(m.codigo())))
            disponibles.add(new Modulo("registrarVenta","Registrar venta","Prepara una cuenta de productos para cobrar en Caja.","ventas"));
        if(usuario.tienePermiso("STAFF_VENTAS_VER"))
            disponibles.add(new Modulo("ventas","Mis ventas","Ventas atribuidas a ti.","agenda"));
        if(usuario.tienePermiso("STAFF_VENTAS_REPORTES_VER"))
            disponibles.add(new Modulo("reporteVentas","Reporte de ventas","Consulta por periodo y concepto.","agenda"));
        return disponibles;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STAFF_VENTAS_VER')")
    public Page<VentaStaffResponse> ventas(@AuthenticationPrincipal UsuarioAutenticado usuario,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(defaultValue="") String concepto,@RequestParam(defaultValue="") String busqueda,
        @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamano) {
        return servicio.consultar(usuario.empresaId(),usuario.usuarioId(),desde,hasta,concepto,busqueda,pagina,tamano);
    }
    @GetMapping("/reporte")
    @PreAuthorize("hasAuthority('STAFF_VENTAS_REPORTES_VER')")
    public Page<VentaStaffResponse> reporte(@AuthenticationPrincipal UsuarioAutenticado usuario,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(defaultValue="") String concepto,@RequestParam(defaultValue="") String busqueda,
        @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamano) {
        return servicio.consultar(usuario.empresaId(),usuario.usuarioId(),desde,hasta,concepto,busqueda,pagina,tamano);
    }
}
