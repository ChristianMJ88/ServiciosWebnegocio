package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioServicioDestacadoItemEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaSitioServicioDestacadoItemRepositorio
        extends JpaRepository<EmpresaSitioServicioDestacadoItemEntidad, Long> {
    List<EmpresaSitioServicioDestacadoItemEntidad> findByEmpresaIdOrderByOrdenAsc(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
