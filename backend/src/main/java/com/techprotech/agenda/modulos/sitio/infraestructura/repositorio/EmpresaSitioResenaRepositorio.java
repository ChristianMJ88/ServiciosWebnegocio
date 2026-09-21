package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioResenaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaSitioResenaRepositorio extends JpaRepository<EmpresaSitioResenaEntidad, Long> {
    List<EmpresaSitioResenaEntidad> findByEmpresaIdOrderByOrdenAsc(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
