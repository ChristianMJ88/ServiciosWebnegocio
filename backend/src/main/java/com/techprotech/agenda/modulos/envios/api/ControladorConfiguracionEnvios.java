package com.techprotech.agenda.modulos.envios.api;

import static com.techprotech.agenda.modulos.plataforma.dominio.CodigosModuloPlataforma.ENVIOS;
import com.techprotech.agenda.modulos.envios.api.dto.ConfiguracionEnvioRequest;
import com.techprotech.agenda.modulos.envios.api.dto.ConfiguracionEnvioResponse;
import com.techprotech.agenda.modulos.envios.api.dto.PruebaConexionEnvioResponse;
import com.techprotech.agenda.modulos.envios.aplicacion.ServicioConfiguracionEnvios;
import com.techprotech.agenda.modulos.envios.aplicacion.ServicioEnviosCheckout;
import com.techprotech.agenda.modulos.envios.api.dto.EnvioPedidoResponse;
import com.techprotech.agenda.modulos.envios.api.dto.ActualizarEnvioManualRequest;
import com.techprotech.agenda.modulos.envios.seguridad.PermisosEnvios;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import com.techprotech.agenda.modulos.plataforma.seguridad.RequiereModulo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/envios/configuracion")
@PreAuthorize(PermisosEnvios.EXPRESION_CONFIGURAR)
@RequiereModulo(ENVIOS)
public class ControladorConfiguracionEnvios {
    private final ServicioConfiguracionEnvios servicio;
    private final ServicioEnviosCheckout enviosCheckout;
    private final com.techprotech.agenda.modulos.envios.aplicacion.ServicioGuiasEnvio guias;

    public ControladorConfiguracionEnvios(ServicioConfiguracionEnvios servicio, ServicioEnviosCheckout enviosCheckout,
                                          com.techprotech.agenda.modulos.envios.aplicacion.ServicioGuiasEnvio guias) {
        this.servicio = servicio;
        this.enviosCheckout = enviosCheckout;
        this.guias = guias;
    }

    @GetMapping
    public ConfiguracionEnvioResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.obtener(usuario.empresaId());
    }

    @PutMapping
    public ConfiguracionEnvioResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                 @Valid @RequestBody ConfiguracionEnvioRequest request) {
        return servicio.actualizar(usuario.empresaId(), request);
    }

    @PostMapping("/probar-conexion")
    public PruebaConexionEnvioResponse probarConexion(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.probarConexion(usuario.empresaId());
    }

    @GetMapping("/pedidos")
    public List<EnvioPedidoResponse> listarPedidos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return enviosCheckout.listar(usuario.empresaId());
    }

    @PatchMapping("/pedidos/{envioId}/gestion-manual")
    public EnvioPedidoResponse actualizarManual(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                 @PathVariable Long envioId,
                                                 @Valid @RequestBody ActualizarEnvioManualRequest request) {
        return enviosCheckout.actualizarManual(usuario.empresaId(), envioId, request);
    }

    @PostMapping("/pedidos/{envioId}/generar-guia")
    public EnvioPedidoResponse generarGuia(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                           @PathVariable Long envioId) {
        guias.generar(usuario.empresaId(), envioId);
        return enviosCheckout.obtener(usuario.empresaId(), envioId);
    }
}
