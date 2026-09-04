package com.techprotech.agenda.modulos.clientes.infraestructura;

import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioRolesEmpresa;
import com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.clientes.aplicacion.ServicioClientes;
import com.techprotech.agenda.modulos.clientes.dominio.Cliente;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioClientesJpa implements ServicioClientes {
    private final ClienteRepositorio clientes;
    private final UsuarioRepositorio usuarios;
    private final CitaRepositorio citas;
    private final ServicioRolesEmpresa roles;
    private final PasswordEncoder passwordEncoder;

    public ServicioClientesJpa(ClienteRepositorio clientes, UsuarioRepositorio usuarios, CitaRepositorio citas,
                               ServicioRolesEmpresa roles, PasswordEncoder passwordEncoder) {
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.citas = citas;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listar(Long empresaId, String busqueda) {
        String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        return clientes.findByEmpresaId(empresaId).stream()
                .map(cliente -> mapear(empresaId, cliente))
                .filter(cliente -> filtro.isBlank()
                        || cliente.nombreCompleto().toLowerCase(Locale.ROOT).contains(filtro)
                        || cliente.correo().toLowerCase(Locale.ROOT).contains(filtro)
                        || cliente.telefono().contains(filtro))
                .toList();
    }

    @Override
    @Transactional
    public Cliente crear(Long empresaId, DatosCliente datos) {
        String correo = normalizarCorreo(datos.correo());
        if (usuarios.existsByEmpresaIdAndCorreo(empresaId, correo)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un usuario con ese correo en el negocio");
        }
        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setEmpresaId(empresaId);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(passwordEncoder.encode("cliente-pendiente-" + UUID.randomUUID()));
        usuario.setHabilitado(false);
        usuario.setBloqueado(false);
        usuario = usuarios.save(usuario);

        ClienteEntidad cliente = new ClienteEntidad();
        cliente.setUsuarioId(usuario.getId());
        aplicar(cliente, datos);
        clientes.save(cliente);
        roles.asignarRolEmpresa(usuario, empresaId, "CLIENTE");
        return mapear(empresaId, cliente);
    }

    @Override
    @Transactional
    public Cliente actualizar(Long empresaId, Long clienteId, DatosCliente datos) {
        UsuarioEntidad usuario = usuarios.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El cliente no pertenece al negocio"));
        ClienteEntidad cliente = clientes.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El cliente no existe"));
        String correo = normalizarCorreo(datos.correo());
        usuarios.findByEmpresaIdAndCorreo(empresaId, correo)
                .filter(existente -> !existente.getId().equals(clienteId))
                .ifPresent(existente -> { throw new ResponseStatusException(CONFLICT, "Ya existe otro usuario con ese correo"); });
        usuario.setCorreo(correo);
        usuarios.save(usuario);
        aplicar(cliente, datos);
        clientes.save(cliente);
        return mapear(empresaId, cliente);
    }

    private Cliente mapear(Long empresaId, ClienteEntidad cliente) {
        UsuarioEntidad usuario = usuarios.findByIdAndEmpresaId(cliente.getUsuarioId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No se encontró la identidad del cliente"));
        return new Cliente(cliente.getUsuarioId(), cliente.getNombreCompleto(), usuario.getCorreo(), cliente.getTelefono(),
                cliente.isAceptaWhatsapp(), cliente.getNotas(), citas.countByEmpresaIdAndClienteId(empresaId, cliente.getUsuarioId()),
                citas.findFirstByEmpresaIdAndClienteIdOrderByInicioDesc(empresaId, cliente.getUsuarioId()).map(c -> c.getInicio()).orElse(null));
    }

    private void aplicar(ClienteEntidad cliente, DatosCliente datos) {
        cliente.setNombreCompleto(datos.nombreCompleto().trim());
        cliente.setTelefono(datos.telefono().trim());
        cliente.setAceptaWhatsapp(datos.aceptaWhatsapp());
        cliente.setNotas(datos.notas() == null || datos.notas().isBlank() ? null : datos.notas().trim());
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
