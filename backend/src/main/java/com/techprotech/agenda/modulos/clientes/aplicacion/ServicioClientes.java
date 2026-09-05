package com.techprotech.agenda.modulos.clientes.aplicacion;

import com.techprotech.agenda.modulos.clientes.dominio.Cliente;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServicioClientes {
    Page<Cliente> listar(Long empresaId, String busqueda, Pageable pageable);
    Cliente crear(Long empresaId, DatosCliente datos);
    Cliente actualizar(Long empresaId, Long clienteId, DatosCliente datos);

    record DatosCliente(String nombreCompleto, String correo, String telefono, boolean aceptaWhatsapp, String notas) {
    }
}
