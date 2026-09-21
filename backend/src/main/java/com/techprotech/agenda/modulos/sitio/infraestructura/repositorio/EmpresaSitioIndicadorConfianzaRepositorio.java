package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioIndicadorConfianzaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaSitioIndicadorConfianzaRepositorio extends JpaRepository<EmpresaSitioIndicadorConfianzaEntidad, Long> {
    List<EmpresaSitioIndicadorConfianzaEntidad> findByEmpresaIdOrderByOrdenAsc(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
