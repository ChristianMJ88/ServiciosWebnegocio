package com.techprotech.agenda.modulos.caja.api;

import static com.techprotech.agenda.modulos.plataforma.dominio.CodigosModuloPlataforma.CAJA;
import com.techprotech.agenda.modulos.caja.api.dto.AbrirCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.CatalogoCajaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.CajaSesionResponse;
import com.techprotech.agenda.modulos.caja.api.dto.CerrarCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.CitaPorCobrarResponse;
import com.techprotech.agenda.modulos.caja.api.dto.MovimientoCajaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.PagoCitaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarMovimientoCajaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarPagoRequest;
import com.techprotech.agenda.modulos.caja.api.dto.ResumenCajaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.CatalogoVentaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarVentaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.VentaResponse;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioCaja;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioVentasCaja;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import com.techprotech.agenda.modulos.plataforma.seguridad.RequiereModulo;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/caja")
@PreAuthorize("hasAuthority('CAJA_ACCESO')")
@RequiereModulo(CAJA)
public class ControladorCaja {

    private final com.techprotech.agenda.modulos.caja.aplicacion.ConfiguracionCobroCaja cobroConfig;
    private final ServicioCaja servicioCaja;
    private final ServicioVentasCaja servicioVentas;

    public ControladorCaja(ServicioCaja servicioCaja, ServicioVentasCaja servicioVentas,com.techprotech.agenda.modulos.caja.aplicacion.ConfiguracionCobroCaja cobroConfig) {
        this.cobroConfig=cobroConfig;
        this.servicioCaja = servicioCaja;
        this.servicioVentas = servicioVentas;
    }

    @GetMapping("/ventas/catalogo")
    @PreAuthorize("hasAuthority('CAJA_COBRAR')")
    public CatalogoVentaResponse catalogoVenta(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                               @RequestParam Long sucursalId) {
        return servicioVentas.catalogo(usuario.empresaId(), usuario.sucursalesPermitidas(), sucursalId);
    }

    @PostMapping("/ventas/cotizacion")
    @PreAuthorize("hasAuthority('CAJA_COBRAR')")
    public com.techprotech.agenda.modulos.caja.api.dto.CotizacionCajaResponse cotizarVenta(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody com.techprotech.agenda.modulos.caja.api.dto.CotizacionCajaRequest request) {
        return servicioVentas.cotizarProductos(usuario.empresaId(), usuario.sucursalesPermitidas(), request);
    }
    @GetMapping("/ventas/{ventaId}")
    public VentaResponse comprobante(@AuthenticationPrincipal UsuarioAutenticado usuario,@PathVariable Long ventaId) {
        return servicioVentas.comprobante(usuario.empresaId(),usuario.sucursalesPermitidas(),ventaId);
    }
    @PostMapping("/ventas")
    @PreAuthorize("hasAuthority('CAJA_COBRAR')")
    public VentaResponse registrarVenta(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                        @Valid @RequestBody RegistrarVentaRequest request) {
        return servicioVentas.registrar(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), request);
    }

    @GetMapping("/ventas/pendientes-staff")
    @PreAuthorize("hasAuthority('CAJA_ACCESO')")
    public com.techprotech.agenda.modulos.caja.api.dto.PaginaCajaResponse<com.techprotech.agenda.modulos.caja.aplicacion.ServicioVentasCaja.PendienteStaff> pendientesStaff(@AuthenticationPrincipal UsuarioAutenticado u,@RequestParam Long sucursalId,@RequestParam(defaultValue="0") int pagina) {
        return com.techprotech.agenda.modulos.caja.api.dto.PaginaCajaResponse.de(servicioVentas.pendientesStaff(u.empresaId(),u.sucursalesPermitidas(),sucursalId,pagina));
    }
    public record CobrarVentaStaffRequest(@jakarta.validation.constraints.NotBlank String metodoPago,@jakarta.validation.constraints.Size(max=120) String referencia) {}
    @PostMapping("/ventas/{ventaId}/cobrar")
    @PreAuthorize("hasAuthority('CAJA_COBRAR')")
    public VentaResponse cobrarStaff(@AuthenticationPrincipal UsuarioAutenticado u,@PathVariable Long ventaId,@Valid @RequestBody CobrarVentaStaffRequest r) {
        return servicioVentas.cobrarPendiente(u.empresaId(),u.usuarioId(),u.sucursalesPermitidas(),ventaId,r.metodoPago(),r.referencia());
    }

    @GetMapping("/ventas")
    @PreAuthorize("hasAuthority('CAJA_ACCESO')")
    public List<VentaResponse> listarVentas(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @RequestParam Long sucursalId) {
        return servicioVentas.listar(usuario.empresaId(), usuario.sucursalesPermitidas(), sucursalId);
    }

    @GetMapping("/catalogo")
    @PreAuthorize("hasAuthority('CAJA_ACCESO')")
    public CatalogoCajaResponse catalogo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long sucursalId
    ) {
        var c=servicioCaja.catalogo(usuario.empresaId(),usuario.sucursalesPermitidas(),sucursalId);
        var metodos=c.metodosPago().stream().map(m -> new CatalogoCajaResponse.OpcionCajaResponse(m.codigo(),m.etiqueta(),m.esEfectivo(),m.esPoint(),m.requiereReferencia(),m.ayuda(),m.esEfectivo()?cobroConfig.getDenominaciones():java.util.Map.<String,java.util.List<java.math.BigDecimal>>of())).toList();
        return new CatalogoCajaResponse(c.sucursalActivaId(),c.sucursales(),metodos,c.tiposMovimiento(),c.estadosSesion(),c.estadoSesionAbierta(),c.estadoSesionCerrada(),c.metodoPagoEfectivo());
    }

    @PostMapping("/sesiones/abrir")
    @PreAuthorize("hasAuthority('CAJA_SESION_GESTIONAR')")
    public CajaSesionResponse abrirCaja(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody AbrirCajaRequest request
    ) {
        return servicioCaja.abrirSesion(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), request);
    }

    @PostMapping("/sesiones/{id}/cerrar")
    @PreAuthorize("hasAuthority('CAJA_SESION_GESTIONAR')")
    public CajaSesionResponse cerrarCaja(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody CerrarCajaRequest request
    ) {
        return servicioCaja.cerrarSesion(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), id, request);
    }

    @GetMapping("/sesiones/actual")
    @PreAuthorize("hasAuthority('CAJA_ACCESO')")
    public CajaSesionResponse sesionActual(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long sucursalId
    ) {
        return servicioCaja.obtenerSesionActual(usuario.empresaId(), usuario.sucursalesPermitidas(), sucursalId);
    }

    @GetMapping("/citas-por-cobrar")
    @PreAuthorize("hasAuthority('CAJA_ACCESO')")
    public List<CitaPorCobrarResponse> citasPorCobrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long sucursalId
    ) {
        return servicioCaja.listarCitasPorCobrar(usuario.empresaId(), usuario.sucursalesPermitidas(), sucursalId);
    }

    @PostMapping("/citas/{citaId}/pagos")
    @PreAuthorize("hasAuthority('CAJA_COBRAR')")
    public PagoCitaResponse registrarPago(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId,
            @Valid @RequestBody RegistrarPagoRequest request
    ) {
        return servicioCaja.registrarPago(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), citaId, request);
    }

    @GetMapping("/citas/{citaId}/pagos")
    @PreAuthorize("hasAuthority('CAJA_ACCESO')")
    public List<PagoCitaResponse> pagosCita(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        return servicioCaja.listarPagosCita(usuario.empresaId(), usuario.sucursalesPermitidas(), citaId);
    }

    @PostMapping("/movimientos")
    @PreAuthorize("hasAuthority('CAJA_MOVIMIENTOS_GESTIONAR')")
    public MovimientoCajaResponse registrarMovimiento(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody RegistrarMovimientoCajaRequest request
    ) {
        return servicioCaja.registrarMovimiento(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), request);
    }

    @GetMapping("/movimientos")
    @PreAuthorize("hasAuthority('CAJA_ACCESO')")
    public List<MovimientoCajaResponse> movimientos(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long sucursalId
    ) {
        return servicioCaja.listarMovimientos(usuario.empresaId(), usuario.sucursalesPermitidas(), sucursalId);
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('CAJA_ACCESO')")
    public ResumenCajaResponse resumen(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long sucursalId
    ) {
        return servicioCaja.resumen(usuario.empresaId(), usuario.sucursalesPermitidas(), sucursalId);
    }
}
