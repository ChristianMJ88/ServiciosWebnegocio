package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioBloqueEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaSitioBloqueRepositorio extends JpaRepository<EmpresaSitioBloqueEntidad, Long> {
    List<EmpresaSitioBloqueEntidad> findByEmpresaIdOrderByOrdenAsc(Long empresaId);
}
