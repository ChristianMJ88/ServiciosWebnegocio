package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioPreguntaFrecuenteEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmpresaSitioPreguntaFrecuenteRepositorio extends JpaRepository<EmpresaSitioPreguntaFrecuenteEntidad, Long> {
    List<EmpresaSitioPreguntaFrecuenteEntidad> findByEmpresaIdOrderByOrdenAsc(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
