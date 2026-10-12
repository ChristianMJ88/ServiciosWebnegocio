package com.techprotech.agenda.modulos.ventasstaff.api;

import com.techprotech.agenda.modulos.caja.api.dto.*;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioVentasCaja;
import com.techprotech.agenda.modulos.clientes.aplicacion.ServicioClientes;
import com.techprotech.agenda.modulos.clientes.aplicacion.FiltroClientes;
import com.techprotech.agenda.modulos.clientes.dominio.Cliente;
import com.techprotech.agenda.modulos.prestadores.aplicacion.ServicioAgendaStaff;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@com.techprotech.agenda.modulos.plataforma.seguridad.RequiereModulo(com.techprotech.agenda.modulos.plataforma.dominio.CodigosModuloPlataforma.PRODUCTOS)
@RestController
@RequestMapping("/api/v1/staff/ventas")
public class ControladorRegistroVentaStaff {
    private final ServicioVentasCaja ventas;
    private final ServicioAgendaStaff perfiles;
    private final ServicioClientes clientes;
    public ControladorRegistroVentaStaff(ServicioVentasCaja ventas,ServicioAgendaStaff perfiles,ServicioClientes clientes) {
        this.ventas=ventas;this.perfiles=perfiles;this.clientes=clientes;
    }
    public record VentaRegistrada(Long id,String folio,String estado,java.math.BigDecimal total,String moneda) {}
    private Long sucursal(UsuarioAutenticado u) {
        var perfil=perfiles.obtenerPerfil(u.empresaId(),u.usuarioId());
        if(!perfil.activo() || !u.puedeAccederSucursal(perfil.sucursalId())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"El prestador no está habilitado en esta sucursal");
        return perfil.sucursalId();
    }
    @GetMapping("/productos") @PreAuthorize("hasAuthority('STAFF_VENTAS_REGISTRAR')")
    public CatalogoVentaResponse productos(@AuthenticationPrincipal UsuarioAutenticado u) { return ventas.catalogo(u.empresaId(),u.sucursalesPermitidas(),sucursal(u)); }
    @PostMapping("/cotizacion") @PreAuthorize("hasAuthority('STAFF_VENTAS_REGISTRAR')")
    public CotizacionCajaResponse cotizar(@AuthenticationPrincipal UsuarioAutenticado u,@Valid @RequestBody RegistrarVentaStaffRequest r) {
        return ventas.cotizarProductos(u.empresaId(),u.sucursalesPermitidas(),new CotizacionCajaRequest(sucursal(u),r.productos()));
    }
    @PostMapping @PreAuthorize("hasAuthority('STAFF_VENTAS_REGISTRAR')")
    public VentaRegistrada registrar(@AuthenticationPrincipal UsuarioAutenticado u,@Valid @RequestBody RegistrarVentaStaffRequest r) {
        var venta=ventas.registrarStaff(u.empresaId(),u.usuarioId(),sucursal(u),r.clienteId(),r.productos());
        return new VentaRegistrada(venta.id(),venta.folio(),venta.estado(),venta.total(),venta.moneda());
    }
    @GetMapping("/clientes") @PreAuthorize("hasAuthority('STAFF_VENTAS_CLIENTES_GESTIONAR')")
    public Page<Cliente> clientes(@AuthenticationPrincipal UsuarioAutenticado u,@RequestParam(defaultValue="") String busqueda) {
        sucursal(u);
        return clientes.listar(u.empresaId(),new FiltroClientes(busqueda,null,null,null,null,null,null),PageRequest.of(0,20));
    }
    public record NuevoCliente(@NotBlank @Size(max=150) String nombreCompleto,@NotBlank @Size(max=30) String telefono,@Email @Size(max=150) String correo) {}
    @PostMapping("/clientes") @PreAuthorize("hasAuthority('STAFF_VENTAS_CLIENTES_GESTIONAR')")
    public Cliente nuevoCliente(@AuthenticationPrincipal UsuarioAutenticado u,@Valid @RequestBody NuevoCliente r) {
        sucursal(u);
        return clientes.crear(u.empresaId(),new ServicioClientes.DatosCliente(r.nombreCompleto(),r.correo(),r.telefono(),false,null));
    }
}
