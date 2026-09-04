package com.techprotech.agenda.modulos.clientes.api;

import com.techprotech.agenda.modulos.clientes.api.dto.ClienteResponse;
import com.techprotech.agenda.modulos.clientes.api.dto.GuardarClienteRequest;
import com.techprotech.agenda.modulos.clientes.aplicacion.ServicioClientes;
import com.techprotech.agenda.modulos.clientes.dominio.Cliente;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/clientes")
@PreAuthorize("hasAuthority('CLIENTES_GESTIONAR')")
public class ControladorClientes {
    private final ServicioClientes servicio;

    public ControladorClientes(ServicioClientes servicio) { this.servicio = servicio; }

    @GetMapping
    public List<ClienteResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                        @RequestParam(required = false) String busqueda) {
        return servicio.listar(usuario.empresaId(), busqueda).stream().map(this::respuesta).toList();
    }

    @PostMapping
    public ClienteResponse crear(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                  @Valid @RequestBody GuardarClienteRequest request) {
        return respuesta(servicio.crear(usuario.empresaId(), datos(request)));
    }

    @PatchMapping("/{clienteId}")
    public ClienteResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long clienteId,
                                       @Valid @RequestBody GuardarClienteRequest request) {
        return respuesta(servicio.actualizar(usuario.empresaId(), clienteId, datos(request)));
    }

    private ServicioClientes.DatosCliente datos(GuardarClienteRequest r) {
        return new ServicioClientes.DatosCliente(r.nombreCompleto(), r.correo(), r.telefono(), r.aceptaWhatsapp(), r.notas());
    }

    private ClienteResponse respuesta(Cliente c) {
        return new ClienteResponse(c.id(), c.nombreCompleto(), c.correo(), c.telefono(), c.aceptaWhatsapp(), c.notas(), c.totalCitas(), c.ultimaCita());
    }
}
