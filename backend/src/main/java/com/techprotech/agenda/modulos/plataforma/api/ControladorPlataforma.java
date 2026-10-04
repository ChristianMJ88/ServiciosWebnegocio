package com.techprotech.agenda.modulos.plataforma.api;

import com.techprotech.agenda.modulos.plataforma.api.dto.CambiarEstadoReglaRequest;
import com.techprotech.agenda.modulos.plataforma.api.dto.GuardarReglaComisionPlataformaRequest;
import com.techprotech.agenda.modulos.plataforma.api.dto.ProveedorPagoPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.ReglaComisionPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.ResumenPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.SimulacionComisionPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.SimularComisionPlataformaRequest;
import com.techprotech.agenda.modulos.plataforma.api.dto.TenantPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.DetalleTenantPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.ModuloPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.GuardarPlanPlataformaRequest;
import com.techprotech.agenda.modulos.plataforma.api.dto.PlanPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.AsignarSuscripcionPlataformaRequest;
import com.techprotech.agenda.modulos.plataforma.api.dto.SuscripcionPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioConsultaPlataforma;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioGestionComisionesPlataforma;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioSuscripcionesPlataforma;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plataforma")
public class ControladorPlataforma {
    private final ServicioConsultaPlataforma servicio;
    private final ServicioGestionComisionesPlataforma comisiones;
    private final ServicioSuscripcionesPlataforma suscripciones;

    public ControladorPlataforma(ServicioConsultaPlataforma servicio, ServicioGestionComisionesPlataforma comisiones,
                                 ServicioSuscripcionesPlataforma suscripciones) {
        this.servicio = servicio; this.comisiones = comisiones; this.suscripciones = suscripciones;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('PLATAFORMA_FINANZAS_VER')")
    public ResumenPlataformaResponse resumen() { return servicio.resumen(); }

    @GetMapping("/tenants")
    @PreAuthorize("hasAuthority('PLATAFORMA_TENANTS_VER')")
    public List<TenantPlataformaResponse> tenants() { return servicio.tenants(); }

    @GetMapping("/tenants/{empresaId}")
    @PreAuthorize("hasAuthority('PLATAFORMA_TENANTS_VER')")
    public DetalleTenantPlataformaResponse tenant(@PathVariable Long empresaId) {
        return servicio.tenant(empresaId);
    }

    @GetMapping("/comisiones/proveedores")
    @PreAuthorize("hasAuthority('PLATAFORMA_COMISIONES_GESTIONAR')")
    public List<ProveedorPagoPlataformaResponse> proveedores() { return comisiones.proveedores(); }

    @GetMapping("/comisiones/reglas")
    @PreAuthorize("hasAuthority('PLATAFORMA_COMISIONES_GESTIONAR')")
    public List<ReglaComisionPlataformaResponse> reglas() { return comisiones.reglas(); }

    @PostMapping("/comisiones/reglas")
    @PreAuthorize("hasAuthority('PLATAFORMA_COMISIONES_GESTIONAR')")
    public ReglaComisionPlataformaResponse crearRegla(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody GuardarReglaComisionPlataformaRequest solicitud) {
        return comisiones.crear(usuario.usuarioId(), solicitud);
    }

    @PutMapping("/comisiones/reglas/{id}")
    @PreAuthorize("hasAuthority('PLATAFORMA_COMISIONES_GESTIONAR')")
    public ReglaComisionPlataformaResponse actualizarRegla(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id, @Valid @RequestBody GuardarReglaComisionPlataformaRequest solicitud) {
        return comisiones.actualizar(usuario.usuarioId(), id, solicitud);
    }

    @PatchMapping("/comisiones/reglas/{id}/estado")
    @PreAuthorize("hasAuthority('PLATAFORMA_COMISIONES_GESTIONAR')")
    public ReglaComisionPlataformaResponse estadoRegla(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id, @RequestBody CambiarEstadoReglaRequest solicitud) {
        return comisiones.cambiarEstado(usuario.usuarioId(), id, solicitud.activa());
    }

    @PostMapping("/comisiones/simular")
    @PreAuthorize("hasAuthority('PLATAFORMA_COMISIONES_GESTIONAR')")
    public SimulacionComisionPlataformaResponse simular(@Valid @RequestBody SimularComisionPlataformaRequest solicitud) {
        return comisiones.simular(solicitud);
    }

    @GetMapping("/suscripciones/modulos")
    @PreAuthorize("hasAuthority('PLATAFORMA_SUSCRIPCIONES_GESTIONAR')")
    public List<ModuloPlataformaResponse> modulosSuscripcion() { return suscripciones.modulos(); }

    @GetMapping("/suscripciones/planes")
    @PreAuthorize("hasAuthority('PLATAFORMA_SUSCRIPCIONES_GESTIONAR')")
    public List<PlanPlataformaResponse> planes() { return suscripciones.planes(); }

    @PostMapping("/suscripciones/planes")
    @PreAuthorize("hasAuthority('PLATAFORMA_SUSCRIPCIONES_GESTIONAR')")
    public PlanPlataformaResponse crearPlan(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody GuardarPlanPlataformaRequest solicitud) {
        return suscripciones.crearPlan(usuario.usuarioId(),solicitud);
    }

    @PutMapping("/suscripciones/planes/{id}")
    @PreAuthorize("hasAuthority('PLATAFORMA_SUSCRIPCIONES_GESTIONAR')")
    public PlanPlataformaResponse actualizarPlan(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,@Valid @RequestBody GuardarPlanPlataformaRequest solicitud) {
        return suscripciones.actualizarPlan(usuario.usuarioId(),id,solicitud);
    }

    @GetMapping("/suscripciones")
    @PreAuthorize("hasAuthority('PLATAFORMA_SUSCRIPCIONES_GESTIONAR')")
    public List<SuscripcionPlataformaResponse> suscripciones() { return suscripciones.suscripciones(); }

    @PostMapping("/suscripciones")
    @PreAuthorize("hasAuthority('PLATAFORMA_SUSCRIPCIONES_GESTIONAR')")
    public SuscripcionPlataformaResponse asignarSuscripcion(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody AsignarSuscripcionPlataformaRequest solicitud) {
        return suscripciones.asignar(usuario.usuarioId(),solicitud);
    }
}
