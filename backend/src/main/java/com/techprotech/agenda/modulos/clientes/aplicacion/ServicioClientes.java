package com.techprotech.agenda.modulos.clientes.aplicacion;

import com.techprotech.agenda.modulos.clientes.dominio.Cliente;

import java.util.List;

public interface ServicioClientes {
    List<Cliente> listar(Long empresaId, String busqueda);
    Cliente crear(Long empresaId, DatosCliente datos);
    Cliente actualizar(Long empresaId, Long clienteId, DatosCliente datos);

    record DatosCliente(String nombreCompleto, String correo, String telefono, boolean aceptaWhatsapp, String notas) {
    }
}
