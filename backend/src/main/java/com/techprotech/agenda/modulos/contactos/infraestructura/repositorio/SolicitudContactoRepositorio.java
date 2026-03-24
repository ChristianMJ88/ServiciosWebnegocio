package com.techprotech.agenda.modulos.contactos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.contactos.infraestructura.entidad.SolicitudContactoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolicitudContactoRepositorio extends JpaRepository<SolicitudContactoEntidad, Long> {
    List<SolicitudContactoEntidad> findByEmpresaIdOrderByCreadaEnDesc(Long empresaId);

    Optional<SolicitudContactoEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
}
