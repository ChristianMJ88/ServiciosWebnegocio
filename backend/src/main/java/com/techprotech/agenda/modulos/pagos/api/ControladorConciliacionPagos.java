package com.techprotech.agenda.modulos.pagos.api;

import static com.techprotech.agenda.modulos.plataforma.dominio.CodigosModuloPlataforma.PAGOS_ONLINE;
import com.techprotech.agenda.modulos.pagos.api.dto.ConciliacionPagoResponse;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioConciliacionPagos;
import com.techprotech.agenda.modulos.pagos.seguridad.PermisosPagos;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import com.techprotech.agenda.modulos.plataforma.seguridad.RequiereModulo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/pagos/conciliaciones")
@RequiereModulo(PAGOS_ONLINE)
public class ControladorConciliacionPagos {
    private final ServicioConciliacionPagos servicio;
    private final com.techprotech.agenda.modulos.pagos.aplicacion.ServicioIncidenciasCheckout incidencias;

    private final com.techprotech.agenda.modulos.pagos.aplicacion.ServicioResolucionIncidenciasCheckout resolucion;

    public ControladorConciliacionPagos(ServicioConciliacionPagos servicio, com.techprotech.agenda.modulos.pagos.aplicacion.ServicioIncidenciasCheckout incidencias, com.techprotech.agenda.modulos.pagos.aplicacion.ServicioResolucionIncidenciasCheckout resolucion) { this.resolucion=resolucion; this.servicio = servicio; this.incidencias=incidencias; }

    @org.springframework.web.bind.annotation.PostMapping("/incidencias/{ordenId}/resolver")
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public void resolver(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @org.springframework.web.bind.annotation.PathVariable String ordenId,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.techprotech.agenda.modulos.pagos.api.dto.ResolverIncidenciaRequest request) {
        resolucion.resolver(usuario, ordenId, request);
    }

    @GetMapping("/incidencias")
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public List<com.techprotech.agenda.modulos.pagos.api.dto.IncidenciaCheckoutResponse> incidencias(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return incidencias.listar(usuario.empresaId());
    }

    @GetMapping
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public List<ConciliacionPagoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.listar(usuario.empresaId());
    }
}
