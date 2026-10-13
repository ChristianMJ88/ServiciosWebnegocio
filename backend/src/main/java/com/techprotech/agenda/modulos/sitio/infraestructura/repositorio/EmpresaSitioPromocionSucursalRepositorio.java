package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioPromocionSucursalEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaSitioPromocionSucursalRepositorio extends JpaRepository<EmpresaSitioPromocionSucursalEntidad, Long> {
    List<EmpresaSitioPromocionSucursalEntidad> findByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
    void deleteByEmpresaIdAndPromocionId(Long empresaId, Long promocionId);
    List<EmpresaSitioPromocionSucursalEntidad> findByEmpresaIdAndPromocionId(Long empresaId, Long promocionId);
}
