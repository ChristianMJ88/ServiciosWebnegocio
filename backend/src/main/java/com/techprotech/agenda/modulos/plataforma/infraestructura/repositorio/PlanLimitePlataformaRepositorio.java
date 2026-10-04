package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.PlanLimitePlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PlanLimitePlataformaRepositorio extends JpaRepository<PlanLimitePlataformaEntidad,PlanLimitePlataformaEntidad.Clave> {
    List<PlanLimitePlataformaEntidad> findByPlanId(Long planId);
    void deleteByPlanId(Long planId);
}
