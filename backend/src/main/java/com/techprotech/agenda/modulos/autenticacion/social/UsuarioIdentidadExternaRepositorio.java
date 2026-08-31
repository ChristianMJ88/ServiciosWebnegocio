package com.techprotech.agenda.modulos.autenticacion.social;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioIdentidadExternaRepositorio extends JpaRepository<UsuarioIdentidadExternaEntidad, Long> {
    boolean existsByUsuarioIdAndProveedor(Long usuarioId, String proveedor);
}
