package com.techprotech.agenda.modulos.envios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EnvioCotizacionEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnvioCotizacionRepositorio extends JpaRepository<EnvioCotizacionEntidad, String> {
    Optional<EnvioCotizacionEntidad> findByIdAndEmpresaId(String id, Long empresaId);
}
