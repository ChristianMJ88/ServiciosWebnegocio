package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.CajaSesionEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CajaSesionRepositorio extends JpaRepository<CajaSesionEntidad, Long> {

    Optional<CajaSesionEntidad> findByEmpresaIdAndSucursalIdAndEstado(Long empresaId, Long sucursalId, String estado);

    Optional<CajaSesionEntidad> findFirstByEmpresaIdAndEstadoOrderByAbiertaEnDesc(Long empresaId, String estado);

    Optional<CajaSesionEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
}
