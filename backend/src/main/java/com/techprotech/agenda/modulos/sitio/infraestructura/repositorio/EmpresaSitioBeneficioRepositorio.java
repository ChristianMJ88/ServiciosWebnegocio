package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioBeneficioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaSitioBeneficioRepositorio extends JpaRepository<EmpresaSitioBeneficioEntidad, Long> {
    List<EmpresaSitioBeneficioEntidad> findByEmpresaIdOrderByOrdenAsc(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
