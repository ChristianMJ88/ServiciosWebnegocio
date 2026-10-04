package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.PlanModuloPlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PlanModuloPlataformaRepositorio extends JpaRepository<PlanModuloPlataformaEntidad,PlanModuloPlataformaEntidad.Clave>{
    List<PlanModuloPlataformaEntidad> findByPlanId(Long planId);
    void deleteByPlanId(Long planId);
}
