package com.techprotech.agenda.modulos.pagos.api;

import com.techprotech.agenda.modulos.pagos.api.dto.*;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioReportesLiberacion;
import com.techprotech.agenda.modulos.pagos.seguridad.PermisosPagos;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/pagos/reportes-liberacion")
@PreAuthorize(PermisosPagos.EXPRESION_CONFIGURAR)
public class ControladorReportesLiberacion {
    private final ServicioReportesLiberacion servicio;
    public ControladorReportesLiberacion(ServicioReportesLiberacion servicio){this.servicio=servicio;}
    @GetMapping public List<ReporteLiberacionResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u){return servicio.listar(u.empresaId());}
    @PostMapping public ReporteLiberacionResponse solicitar(@AuthenticationPrincipal UsuarioAutenticado u,@Valid @RequestBody SolicitarReporteLiberacionRequest r){return servicio.solicitar(u.empresaId(),r.desde(),r.hasta());}
    @PostMapping("/{id}/sincronizar") public ReporteLiberacionResponse sincronizar(@AuthenticationPrincipal UsuarioAutenticado u,@PathVariable Long id){return servicio.sincronizar(u.empresaId(),id);}
}
