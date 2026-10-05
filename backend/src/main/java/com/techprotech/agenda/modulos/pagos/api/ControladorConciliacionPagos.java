package com.techprotech.agenda.modulos.pagos.api;

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
@RequiereModulo("PAGOS_ONLINE")
public class ControladorConciliacionPagos {
    private final ServicioConciliacionPagos servicio;

    public ControladorConciliacionPagos(ServicioConciliacionPagos servicio) { this.servicio = servicio; }

    @GetMapping
    @PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
    public List<ConciliacionPagoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.listar(usuario.empresaId());
    }
}
