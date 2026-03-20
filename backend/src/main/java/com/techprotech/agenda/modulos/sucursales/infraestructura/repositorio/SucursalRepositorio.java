package com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SucursalRepositorio extends JpaRepository<SucursalEntidad, Long> {

    List<SucursalEntidad> findByEmpresaIdAndActivaTrue(Long empresaId);

    Optional<SucursalEntidad> findByIdAndEmpresaIdAndActivaTrue(Long id, Long empresaId);

    List<SucursalEntidad> findByEmpresaIdOrderByNombreAsc(Long empresaId);

    Optional<SucursalEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
}
