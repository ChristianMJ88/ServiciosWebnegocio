package com.techprotech.agenda.modulos.clientes.api;

import com.techprotech.agenda.modulos.clientes.api.dto.ClienteResponse;
import com.techprotech.agenda.modulos.clientes.api.dto.GuardarClienteRequest;
import com.techprotech.agenda.modulos.clientes.api.dto.PaginaClientesResponse;
import com.techprotech.agenda.modulos.clientes.aplicacion.ServicioClientes;
import com.techprotech.agenda.modulos.clientes.aplicacion.FiltroClientes;
import com.techprotech.agenda.modulos.clientes.api.dto.FiltrosClientesResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.techprotech.agenda.modulos.clientes.dominio.Cliente;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/v1/admin/clientes")
@PreAuthorize("hasAuthority('CLIENTES_GESTIONAR')")
public class ControladorClientes {
    private final ServicioClientes servicio;

    public ControladorClientes(ServicioClientes servicio) { this.servicio = servicio; }

    @GetMapping
    public PaginaClientesResponse listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitaHasta,
            @RequestParam(required = false) String citas,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano
    ) {
        int paginaSegura = Math.max(0, pagina);
        int tamanoSeguro = Math.max(1, Math.min(tamano, 100));
        var resultado = servicio.listar(
                usuario.empresaId(),
                new FiltroClientes(busqueda, nombre, telefono, visitaDesde, visitaHasta,
                        FiltroClientes.opcion(FiltroClientes.Citas.class, citas), FiltroClientes.opcion(FiltroClientes.Orden.class, orden)),
                PageRequest.of(paginaSegura, tamanoSeguro)
        );
        return new PaginaClientesResponse(
                resultado.getContent().stream().map(this::respuesta).toList(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages()
        );
    }

    @GetMapping("/metadatos")
    public FiltrosClientesResponse metadatos() {
        return new FiltrosClientesResponse(
                java.util.Arrays.stream(FiltroClientes.Orden.values()).map(o -> new FiltrosClientesResponse.Opcion(o.name(), o.etiqueta)).toList(),
                java.util.Arrays.stream(FiltroClientes.Citas.values()).map(o -> new FiltrosClientesResponse.Opcion(o.name(), o.etiqueta)).toList(),
                FiltroClientes.Orden.NOMBRE_ASC.name(), FiltroClientes.Citas.TODOS.name());
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
        return new ClienteResponse(c.id(), c.nombreCompleto(), c.correo(), c.telefono(), c.aceptaWhatsapp(), c.notas(), c.totalCitas(), c.ultimaCita(), c.ultimaVisita(), c.citasAgendadas());
    }
}
