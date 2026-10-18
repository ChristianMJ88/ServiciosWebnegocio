package com.techprotech.agenda.modulos.pagos.api;

import com.techprotech.agenda.modulos.pagos.api.dto.*;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioMercadoPagoPoint;
import com.techprotech.agenda.modulos.pagos.seguridad.PermisosPagos;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ControladorMercadoPagoPoint {
    private final com.techprotech.agenda.modulos.pagos.aplicacion.ServicioResolucionPoint resolucion;
    private final java.time.Clock reloj;
    private final ServicioMercadoPagoPoint servicio;

    public ControladorMercadoPagoPoint(ServicioMercadoPagoPoint servicio,com.techprotech.agenda.modulos.pagos.aplicacion.ServicioResolucionPoint resolucion,java.time.Clock reloj) {
        this.reloj=reloj;
        this.resolucion=resolucion; this.servicio = servicio; }

    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public org.springframework.http.ResponseEntity<com.techprotech.agenda.compartido.exception.ErrorApi> conflictoConcurrente(Exception e) {
        return org.springframework.http.ResponseEntity.status(409).body(new com.techprotech.agenda.compartido.exception.ErrorApi("COBRO_CONCURRENTE","La operación coincidió con otro cobro. Intenta de nuevo.",java.time.OffsetDateTime.now(reloj)));
    }

    @GetMapping("/api/v1/admin/pagos/point/terminales")
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public List<TerminalPointResponse> terminales(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.listarTerminales(usuario.empresaId());
    }

    @PutMapping("/api/v1/admin/pagos/point/terminales")
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public TerminalPointResponse guardar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @Valid @RequestBody GuardarTerminalPointRequest request) {
        return servicio.guardarTerminal(usuario.empresaId(), request);
    }

    @PostMapping("/api/v1/caja/point/ordenes")
    @PreAuthorize("hasAuthority('CAJA_COBRAR')")
    public OrdenPointResponse crear(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                    @RequestHeader("Idempotency-Key") String idempotencia,
                                    @Valid @RequestBody CrearOrdenPointRequest request) {
        return servicio.crearOrden(usuario.empresaId(), usuario.usuarioId(), usuario.sucursalesPermitidas(), idempotencia, request).conAcciones(usuario);
    }

    @GetMapping("/api/v1/caja/point/terminales")
    @PreAuthorize("hasAuthority('CAJA_ACCESO')")
    public List<TerminalPointResponse> terminalesCaja(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                       @RequestParam Long sucursalId) {
        return servicio.listarTerminalesCaja(usuario.empresaId(), usuario.sucursalesPermitidas(), sucursalId);
    }

    @GetMapping("/api/v1/caja/point/ordenes")
    @PreAuthorize("hasAuthority('CAJA_ACCESO')")
    public List<OrdenPointResponse> ordenesCaja(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                @RequestParam Long sucursalId) {
        return servicio.listarOrdenesCaja(usuario.empresaId(), usuario.sucursalesPermitidas(), sucursalId);
    }

    @GetMapping("/api/v1/caja/point/ordenes/{id}")
    @PreAuthorize("hasAuthority('CAJA_ACCESO')")
    public OrdenPointResponse consultar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        return servicio.consultar(usuario.empresaId(),usuario.sucursalesPermitidas(), id).conAcciones(usuario);
    }

    @PostMapping("/api/v1/caja/point/ordenes/{id}/resolver")
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public OrdenPointResponse resolver(@AuthenticationPrincipal UsuarioAutenticado usuario,@PathVariable String id,@Valid @RequestBody ResolverOrdenPointRequest request) {
        return resolucion.resolver(usuario,id,request);
    }

    @PostMapping("/api/v1/caja/point/ordenes/{id}/reintentar")
    @PreAuthorize("hasAuthority('CAJA_COBRAR')")
    public OrdenPointResponse reintentar(@AuthenticationPrincipal UsuarioAutenticado usuario,@PathVariable String id) {
        return servicio.reintentar(usuario.empresaId(),usuario.sucursalesPermitidas(),id).conAcciones(usuario);
    }

    @PostMapping("/api/v1/caja/point/ordenes/{id}/cancelar")
    @PreAuthorize("hasAuthority('CAJA_COBRAR')")
    public OrdenPointResponse cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id,
                                       @RequestHeader("Idempotency-Key") String idempotencia) {
        return servicio.cancelar(usuario.empresaId(),usuario.sucursalesPermitidas(), id, idempotencia).conAcciones(usuario);
    }

    @PostMapping("/api/v1/caja/point/ordenes/{id}/reembolsar")
    @PreAuthorize("hasAuthority('CAJA_COBRAR')")
    public OrdenPointResponse reembolsar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id,
                                         @RequestHeader("Idempotency-Key") String idempotencia) {
        return servicio.reembolsar(usuario.empresaId(),usuario.sucursalesPermitidas(), id, idempotencia).conAcciones(usuario);
    }

    @PostMapping("/api/v1/caja/point/ordenes/{id}/simular-aprobacion")
    @PreAuthorize("hasAuthority('CAJA_COBRAR')")
    public OrdenPointResponse simularAprobacion(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                @PathVariable String id) {
        return servicio.simularAprobacion(usuario.empresaId(),usuario.sucursalesPermitidas(), id).conAcciones(usuario);
    }
}
