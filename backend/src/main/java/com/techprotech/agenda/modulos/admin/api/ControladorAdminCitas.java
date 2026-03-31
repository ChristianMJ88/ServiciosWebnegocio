package com.techprotech.agenda.modulos.admin.api;

import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionCorreoAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionCorreoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionWhatsappAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionWhatsappAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.DetectarChannelSenderWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.LogMensajeWhatsappAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.MigracionSecretosCorreoResponse;
import com.techprotech.agenda.modulos.admin.api.dto.AsociarChannelSenderWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.AsociarChannelSenderWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.AuditoriaConfiguracionAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.AuditoriaRolInternoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PlantillaWhatsappAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PrestadorAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.PrestadorAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PermisoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PlantillaRolInternoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarSubcuentaWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarMessagingServiceWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarMessagingServiceWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarSubcuentaWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PruebaPlantillaWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.PruebaPlantillaWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ReportePrestadorAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ReporteServicioAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.RolInternoAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.RolInternoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ResumenAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ServicioAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ServicioAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.SucursalAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.SucursalAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.UsuarioInternoAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.UsuarioInternoAdminResponse;
import com.techprotech.agenda.modulos.admin.aplicacion.ServicioAdminCitas;
import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import jakarta.validation.Valid;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('PANEL_ADMIN_ACCESO')")
public class ControladorAdminCitas {

    private final ServicioAdminCitas servicioAdminCitas;

    public ControladorAdminCitas(ServicioAdminCitas servicioAdminCitas) {
        this.servicioAdminCitas = servicioAdminCitas;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('PANEL_ADMIN_ACCESO')")
    public ResumenAdminResponse resumen(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.resumen(usuario.empresaId());
    }

    @GetMapping("/citas")
    @PreAuthorize("hasAuthority('CITAS_ADMIN_GESTIONAR')")
    public List<CitaClienteResponse> citas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listar(usuario.empresaId());
    }

    @PatchMapping("/citas/{citaId}/confirmar")
    @PreAuthorize("hasAuthority('CITAS_ADMIN_GESTIONAR')")
    public org.springframework.http.ResponseEntity<Void> confirmarCita(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        servicioAdminCitas.cambiarEstadoCita(usuario.empresaId(), usuario.usuarioId(), citaId, "CONFIRMADA");
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PatchMapping("/citas/{citaId}/finalizar")
    @PreAuthorize("hasAuthority('CITAS_ADMIN_GESTIONAR')")
    public org.springframework.http.ResponseEntity<Void> finalizarCita(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        servicioAdminCitas.cambiarEstadoCita(usuario.empresaId(), usuario.usuarioId(), citaId, "FINALIZADA");
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PatchMapping("/citas/{citaId}/no-asistio")
    @PreAuthorize("hasAuthority('CITAS_ADMIN_GESTIONAR')")
    public org.springframework.http.ResponseEntity<Void> marcarNoAsistio(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        servicioAdminCitas.cambiarEstadoCita(usuario.empresaId(), usuario.usuarioId(), citaId, "NO_ASISTIO");
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PatchMapping("/citas/{citaId}/cancelar")
    @PreAuthorize("hasAuthority('CITAS_ADMIN_GESTIONAR')")
    public org.springframework.http.ResponseEntity<Void> cancelarCita(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        servicioAdminCitas.cambiarEstadoCita(usuario.empresaId(), usuario.usuarioId(), citaId, "CANCELADA");
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @GetMapping("/reportes/servicios")
    @PreAuthorize("hasAuthority('REPORTES_ADMIN_VER')")
    public List<ReporteServicioAdminResponse> reporteServicios(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.reporteServicios(usuario.empresaId());
    }

    @GetMapping("/reportes/prestadores")
    @PreAuthorize("hasAuthority('REPORTES_ADMIN_VER')")
    public List<ReportePrestadorAdminResponse> reportePrestadores(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.reportePrestadores(usuario.empresaId());
    }

    @GetMapping("/roles-internos")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public List<RolInternoAdminResponse> rolesInternos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarRolesInternos(usuario.empresaId());
    }

    @GetMapping("/permisos")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public List<PermisoAdminResponse> permisos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarPermisos(usuario.empresaId());
    }

    @GetMapping("/roles-internos/plantillas")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public List<PlantillaRolInternoAdminResponse> plantillasRolesInternos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarPlantillasRolesInternos(usuario.empresaId());
    }

    @GetMapping("/roles-internos/auditoria")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public List<AuditoriaRolInternoAdminResponse> auditoriaRolesInternos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarAuditoriaRolesInternos(usuario.empresaId());
    }

    @PostMapping("/roles-internos")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public RolInternoAdminResponse crearRolInterno(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody RolInternoAdminRequest request
    ) {
        return servicioAdminCitas.crearRolInterno(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @PatchMapping("/roles-internos/{rolEmpresaId}")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public RolInternoAdminResponse actualizarRolInterno(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long rolEmpresaId,
            @Valid @RequestBody RolInternoAdminRequest request
    ) {
        return servicioAdminCitas.actualizarRolInterno(usuario.empresaId(), usuario.usuarioId(), rolEmpresaId, request);
    }

    @PostMapping("/roles-internos/{rolEmpresaId}/clonar")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public RolInternoAdminResponse clonarRolInterno(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long rolEmpresaId,
            @Valid @RequestBody RolInternoAdminRequest request
    ) {
        return servicioAdminCitas.clonarRolInterno(usuario.empresaId(), usuario.usuarioId(), rolEmpresaId, request);
    }

    @DeleteMapping("/roles-internos/{rolEmpresaId}")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public ResponseEntity<Void> eliminarRolInterno(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long rolEmpresaId
    ) {
        servicioAdminCitas.eliminarRolInterno(usuario.empresaId(), usuario.usuarioId(), rolEmpresaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/configuracion-correo")
    @PreAuthorize("hasAuthority('CONFIGURACION_EMPRESA_GESTIONAR')")
    public ConfiguracionCorreoAdminResponse configuracionCorreo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.obtenerConfiguracionCorreo(usuario.empresaId());
    }

    @PatchMapping("/configuracion-correo")
    @PreAuthorize("hasAuthority('CONFIGURACION_EMPRESA_GESTIONAR')")
    public ConfiguracionCorreoAdminResponse actualizarConfiguracionCorreo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ConfiguracionCorreoAdminRequest request
    ) {
        return servicioAdminCitas.actualizarConfiguracionCorreo(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @PostMapping("/configuracion-correo/migrar-secretos")
    @PreAuthorize("hasAuthority('CONFIGURACION_EMPRESA_GESTIONAR')")
    public MigracionSecretosCorreoResponse migrarSecretosCorreo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.migrarSecretosCorreo(usuario.empresaId(), usuario.usuarioId());
    }

    @GetMapping("/configuracion-whatsapp")
    @PreAuthorize("hasAuthority('WHATSAPP_CONFIGURAR')")
    public ConfiguracionWhatsappAdminResponse configuracionWhatsapp(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.obtenerConfiguracionWhatsapp(usuario.empresaId());
    }

    @GetMapping("/configuracion/auditoria")
    @PreAuthorize("hasAuthority('CONFIGURACION_EMPRESA_GESTIONAR') or hasAuthority('WHATSAPP_CONFIGURAR')")
    public List<AuditoriaConfiguracionAdminResponse> auditoriaConfiguracion(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarAuditoriaConfiguracion(usuario.empresaId());
    }

    @PatchMapping("/configuracion-whatsapp")
    @PreAuthorize("hasAuthority('WHATSAPP_CONFIGURAR')")
    public ConfiguracionWhatsappAdminResponse actualizarConfiguracionWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ConfiguracionWhatsappAdminRequest request
    ) {
        return servicioAdminCitas.actualizarConfiguracionWhatsapp(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @GetMapping("/configuracion-whatsapp/plantillas")
    @PreAuthorize("hasAuthority('WHATSAPP_CONFIGURAR')")
    public List<PlantillaWhatsappAdminResponse> plantillasWhatsapp(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarPlantillasWhatsapp(usuario.empresaId());
    }

    @GetMapping("/configuracion-whatsapp/logs")
    @PreAuthorize("hasAuthority('WHATSAPP_CONFIGURAR')")
    public List<LogMensajeWhatsappAdminResponse> logsWhatsapp(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarLogsWhatsapp(usuario.empresaId());
    }

    @PostMapping("/configuracion-whatsapp/provisionar-subcuenta")
    @PreAuthorize("hasAuthority('WHATSAPP_CONFIGURAR')")
    public ProvisionarSubcuentaWhatsappResponse provisionarSubcuentaWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ProvisionarSubcuentaWhatsappRequest request
    ) {
        return servicioAdminCitas.provisionarSubcuentaWhatsapp(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @PostMapping("/configuracion-whatsapp/provisionar-messaging-service")
    @PreAuthorize("hasAuthority('WHATSAPP_CONFIGURAR')")
    public ProvisionarMessagingServiceWhatsappResponse provisionarMessagingServiceWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ProvisionarMessagingServiceWhatsappRequest request
    ) {
        return servicioAdminCitas.provisionarMessagingServiceWhatsapp(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @PostMapping("/configuracion-whatsapp/asociar-channel-sender")
    @PreAuthorize("hasAuthority('WHATSAPP_CONFIGURAR')")
    public AsociarChannelSenderWhatsappResponse asociarChannelSenderWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody AsociarChannelSenderWhatsappRequest request
    ) {
        return servicioAdminCitas.asociarChannelSenderWhatsapp(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @PostMapping("/configuracion-whatsapp/detectar-channel-sender")
    @PreAuthorize("hasAuthority('WHATSAPP_CONFIGURAR')")
    public DetectarChannelSenderWhatsappResponse detectarChannelSenderWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return servicioAdminCitas.detectarChannelSenderWhatsapp(usuario.empresaId(), usuario.usuarioId());
    }

    @PostMapping("/configuracion-whatsapp/probar-plantilla")
    @PreAuthorize("hasAuthority('WHATSAPP_CONFIGURAR')")
    public PruebaPlantillaWhatsappResponse probarPlantillaWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody PruebaPlantillaWhatsappRequest request
    ) {
        return servicioAdminCitas.probarPlantillaWhatsapp(usuario.empresaId(), request);
    }

    @GetMapping("/sucursales")
    @PreAuthorize("hasAuthority('SUCURSALES_GESTIONAR')")
    public List<SucursalAdminResponse> sucursales(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarSucursales(usuario.empresaId());
    }

    @PostMapping("/sucursales")
    @PreAuthorize("hasAuthority('SUCURSALES_GESTIONAR')")
    public SucursalAdminResponse crearSucursal(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody SucursalAdminRequest request
    ) {
        return servicioAdminCitas.crearSucursal(usuario.empresaId(), request);
    }

    @PatchMapping("/sucursales/{id}")
    @PreAuthorize("hasAuthority('SUCURSALES_GESTIONAR')")
    public SucursalAdminResponse actualizarSucursal(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody SucursalAdminRequest request
    ) {
        return servicioAdminCitas.actualizarSucursal(usuario.empresaId(), id, request);
    }

    @GetMapping("/servicios")
    @PreAuthorize("hasAuthority('SERVICIOS_GESTIONAR')")
    public List<ServicioAdminResponse> servicios(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarServicios(usuario.empresaId());
    }

    @PostMapping("/servicios")
    @PreAuthorize("hasAuthority('SERVICIOS_GESTIONAR')")
    public ServicioAdminResponse crearServicio(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ServicioAdminRequest request
    ) {
        return servicioAdminCitas.crearServicio(usuario.empresaId(), request);
    }

    @PatchMapping("/servicios/{id}")
    @PreAuthorize("hasAuthority('SERVICIOS_GESTIONAR')")
    public ServicioAdminResponse actualizarServicio(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ServicioAdminRequest request
    ) {
        return servicioAdminCitas.actualizarServicio(usuario.empresaId(), id, request);
    }

    @GetMapping("/prestadores")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public List<PrestadorAdminResponse> prestadores(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarPrestadores(usuario.empresaId());
    }

    @GetMapping("/usuarios-internos")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public List<UsuarioInternoAdminResponse> usuariosInternos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarUsuariosInternos(usuario.empresaId());
    }

    @PostMapping("/usuarios-internos")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public UsuarioInternoAdminResponse crearUsuarioInterno(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody UsuarioInternoAdminRequest request
    ) {
        return servicioAdminCitas.crearUsuarioInterno(usuario.empresaId(), usuario.usuarioId(), request);
    }

    @PatchMapping("/usuarios-internos/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_INTERNOS_GESTIONAR')")
    public UsuarioInternoAdminResponse actualizarUsuarioInterno(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody UsuarioInternoAdminRequest request
    ) {
        return servicioAdminCitas.actualizarUsuarioInterno(usuario.empresaId(), usuario.usuarioId(), id, request);
    }

    @PostMapping("/prestadores")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public PrestadorAdminResponse crearPrestador(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody PrestadorAdminRequest request
    ) {
        return servicioAdminCitas.crearPrestador(usuario.empresaId(), request);
    }

    @PatchMapping("/prestadores/{id}")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public PrestadorAdminResponse actualizarPrestador(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody PrestadorAdminRequest request
    ) {
        return servicioAdminCitas.actualizarPrestador(usuario.empresaId(), id, request);
    }
}
