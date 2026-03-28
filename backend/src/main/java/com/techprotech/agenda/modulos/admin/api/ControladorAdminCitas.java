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
import com.techprotech.agenda.modulos.admin.api.dto.PlantillaWhatsappAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PrestadorAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.PrestadorAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarSubcuentaWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarMessagingServiceWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarMessagingServiceWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarSubcuentaWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PruebaPlantillaWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.PruebaPlantillaWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ReporteServicioAdminResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class ControladorAdminCitas {

    private final ServicioAdminCitas servicioAdminCitas;

    public ControladorAdminCitas(ServicioAdminCitas servicioAdminCitas) {
        this.servicioAdminCitas = servicioAdminCitas;
    }

    @GetMapping("/resumen")
    public ResumenAdminResponse resumen(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.resumen(usuario.empresaId());
    }

    @GetMapping("/citas")
    public List<CitaClienteResponse> citas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listar(usuario.empresaId());
    }

    @PatchMapping("/citas/{citaId}/confirmar")
    public org.springframework.http.ResponseEntity<Void> confirmarCita(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        servicioAdminCitas.cambiarEstadoCita(usuario.empresaId(), usuario.usuarioId(), citaId, "CONFIRMADA");
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PatchMapping("/citas/{citaId}/finalizar")
    public org.springframework.http.ResponseEntity<Void> finalizarCita(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        servicioAdminCitas.cambiarEstadoCita(usuario.empresaId(), usuario.usuarioId(), citaId, "FINALIZADA");
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PatchMapping("/citas/{citaId}/no-asistio")
    public org.springframework.http.ResponseEntity<Void> marcarNoAsistio(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        servicioAdminCitas.cambiarEstadoCita(usuario.empresaId(), usuario.usuarioId(), citaId, "NO_ASISTIO");
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PatchMapping("/citas/{citaId}/cancelar")
    public org.springframework.http.ResponseEntity<Void> cancelarCita(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long citaId
    ) {
        servicioAdminCitas.cambiarEstadoCita(usuario.empresaId(), usuario.usuarioId(), citaId, "CANCELADA");
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @GetMapping("/reportes/servicios")
    public List<ReporteServicioAdminResponse> reporteServicios(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.reporteServicios(usuario.empresaId());
    }

    @GetMapping("/configuracion-correo")
    public ConfiguracionCorreoAdminResponse configuracionCorreo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.obtenerConfiguracionCorreo(usuario.empresaId());
    }

    @PatchMapping("/configuracion-correo")
    public ConfiguracionCorreoAdminResponse actualizarConfiguracionCorreo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ConfiguracionCorreoAdminRequest request
    ) {
        return servicioAdminCitas.actualizarConfiguracionCorreo(usuario.empresaId(), request);
    }

    @PostMapping("/configuracion-correo/migrar-secretos")
    public MigracionSecretosCorreoResponse migrarSecretosCorreo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.migrarSecretosCorreo(usuario.empresaId());
    }

    @GetMapping("/configuracion-whatsapp")
    public ConfiguracionWhatsappAdminResponse configuracionWhatsapp(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.obtenerConfiguracionWhatsapp(usuario.empresaId());
    }

    @PatchMapping("/configuracion-whatsapp")
    public ConfiguracionWhatsappAdminResponse actualizarConfiguracionWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ConfiguracionWhatsappAdminRequest request
    ) {
        return servicioAdminCitas.actualizarConfiguracionWhatsapp(usuario.empresaId(), request);
    }

    @GetMapping("/configuracion-whatsapp/plantillas")
    public List<PlantillaWhatsappAdminResponse> plantillasWhatsapp(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarPlantillasWhatsapp(usuario.empresaId());
    }

    @GetMapping("/configuracion-whatsapp/logs")
    public List<LogMensajeWhatsappAdminResponse> logsWhatsapp(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarLogsWhatsapp(usuario.empresaId());
    }

    @PostMapping("/configuracion-whatsapp/provisionar-subcuenta")
    public ProvisionarSubcuentaWhatsappResponse provisionarSubcuentaWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ProvisionarSubcuentaWhatsappRequest request
    ) {
        return servicioAdminCitas.provisionarSubcuentaWhatsapp(usuario.empresaId(), request);
    }

    @PostMapping("/configuracion-whatsapp/provisionar-messaging-service")
    public ProvisionarMessagingServiceWhatsappResponse provisionarMessagingServiceWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ProvisionarMessagingServiceWhatsappRequest request
    ) {
        return servicioAdminCitas.provisionarMessagingServiceWhatsapp(usuario.empresaId(), request);
    }

    @PostMapping("/configuracion-whatsapp/asociar-channel-sender")
    public AsociarChannelSenderWhatsappResponse asociarChannelSenderWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody AsociarChannelSenderWhatsappRequest request
    ) {
        return servicioAdminCitas.asociarChannelSenderWhatsapp(usuario.empresaId(), request);
    }

    @PostMapping("/configuracion-whatsapp/detectar-channel-sender")
    public DetectarChannelSenderWhatsappResponse detectarChannelSenderWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return servicioAdminCitas.detectarChannelSenderWhatsapp(usuario.empresaId());
    }

    @PostMapping("/configuracion-whatsapp/probar-plantilla")
    public PruebaPlantillaWhatsappResponse probarPlantillaWhatsapp(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody PruebaPlantillaWhatsappRequest request
    ) {
        return servicioAdminCitas.probarPlantillaWhatsapp(usuario.empresaId(), request);
    }

    @GetMapping("/sucursales")
    public List<SucursalAdminResponse> sucursales(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarSucursales(usuario.empresaId());
    }

    @PostMapping("/sucursales")
    public SucursalAdminResponse crearSucursal(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody SucursalAdminRequest request
    ) {
        return servicioAdminCitas.crearSucursal(usuario.empresaId(), request);
    }

    @PatchMapping("/sucursales/{id}")
    public SucursalAdminResponse actualizarSucursal(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody SucursalAdminRequest request
    ) {
        return servicioAdminCitas.actualizarSucursal(usuario.empresaId(), id, request);
    }

    @GetMapping("/servicios")
    public List<ServicioAdminResponse> servicios(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarServicios(usuario.empresaId());
    }

    @PostMapping("/servicios")
    public ServicioAdminResponse crearServicio(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ServicioAdminRequest request
    ) {
        return servicioAdminCitas.crearServicio(usuario.empresaId(), request);
    }

    @PatchMapping("/servicios/{id}")
    public ServicioAdminResponse actualizarServicio(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ServicioAdminRequest request
    ) {
        return servicioAdminCitas.actualizarServicio(usuario.empresaId(), id, request);
    }

    @GetMapping("/prestadores")
    public List<PrestadorAdminResponse> prestadores(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarPrestadores(usuario.empresaId());
    }

    @GetMapping("/usuarios-internos")
    public List<UsuarioInternoAdminResponse> usuariosInternos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioAdminCitas.listarUsuariosInternos(usuario.empresaId());
    }

    @PostMapping("/usuarios-internos")
    public UsuarioInternoAdminResponse crearUsuarioInterno(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody UsuarioInternoAdminRequest request
    ) {
        return servicioAdminCitas.crearUsuarioInterno(usuario.empresaId(), request);
    }

    @PatchMapping("/usuarios-internos/{id}")
    public UsuarioInternoAdminResponse actualizarUsuarioInterno(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody UsuarioInternoAdminRequest request
    ) {
        return servicioAdminCitas.actualizarUsuarioInterno(usuario.empresaId(), id, request);
    }

    @PostMapping("/prestadores")
    public PrestadorAdminResponse crearPrestador(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody PrestadorAdminRequest request
    ) {
        return servicioAdminCitas.crearPrestador(usuario.empresaId(), request);
    }

    @PatchMapping("/prestadores/{id}")
    public PrestadorAdminResponse actualizarPrestador(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody PrestadorAdminRequest request
    ) {
        return servicioAdminCitas.actualizarPrestador(usuario.empresaId(), id, request);
    }
}
