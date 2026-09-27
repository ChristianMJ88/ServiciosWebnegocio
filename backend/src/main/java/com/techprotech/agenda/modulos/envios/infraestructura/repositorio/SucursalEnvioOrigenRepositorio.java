package com.techprotech.agenda.modulos.envios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.envios.infraestructura.entidad.SucursalEnvioOrigenEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SucursalEnvioOrigenRepositorio extends JpaRepository<SucursalEnvioOrigenEntidad, Long> {
    List<SucursalEnvioOrigenEntidad> findByEmpresaIdOrderBySucursalIdAsc(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
