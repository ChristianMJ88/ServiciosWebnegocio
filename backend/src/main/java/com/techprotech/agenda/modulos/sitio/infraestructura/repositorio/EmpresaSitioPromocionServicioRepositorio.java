package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioPromocionServicioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaSitioPromocionServicioRepositorio extends JpaRepository<EmpresaSitioPromocionServicioEntidad, Long> {
    List<EmpresaSitioPromocionServicioEntidad> findByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
