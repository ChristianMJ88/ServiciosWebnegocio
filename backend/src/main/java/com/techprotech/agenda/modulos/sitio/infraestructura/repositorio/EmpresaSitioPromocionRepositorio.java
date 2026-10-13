package com.techprotech.agenda.modulos.sitio.infraestructura.repositorio;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioPromocionEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaSitioPromocionRepositorio extends JpaRepository<EmpresaSitioPromocionEntidad, Long> {
    List<EmpresaSitioPromocionEntidad> findByEmpresaIdOrderByOrdenAsc(Long empresaId);
    java.util.Optional<EmpresaSitioPromocionEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
}
