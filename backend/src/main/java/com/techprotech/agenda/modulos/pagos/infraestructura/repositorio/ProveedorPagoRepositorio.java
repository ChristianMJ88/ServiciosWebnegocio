package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ProveedorPagoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProveedorPagoRepositorio extends JpaRepository<ProveedorPagoEntidad, Long> {
    Optional<ProveedorPagoEntidad> findByCodigoAndActivoTrue(String codigo);
}
