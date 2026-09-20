package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioGaleriaImagenEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaSitioGaleriaImagenRepositorio extends JpaRepository<EmpresaSitioGaleriaImagenEntidad, Long> {
    List<EmpresaSitioGaleriaImagenEntidad> findByEmpresaIdOrderByOrdenAsc(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
