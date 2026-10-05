package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.PlanLimitePlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.api.dto.CapacidadLimiteEmpresaResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface PlanLimitePlataformaRepositorio extends JpaRepository<PlanLimitePlataformaEntidad,PlanLimitePlataformaEntidad.Clave> {
    List<PlanLimitePlataformaEntidad> findByPlanId(Long planId);
    @Query("""
            select new com.techprotech.agenda.modulos.plataforma.api.dto.CapacidadLimiteEmpresaResponse(
                limite.codigo, limite.nombre, limite.unidad, relacion.valor)
            from PlanLimitePlataformaEntidad relacion
            join LimitePlataformaEntidad limite on limite.id = relacion.limiteId
            where relacion.planId = :planId and limite.activo = true
            order by limite.orden
            """)
    List<CapacidadLimiteEmpresaResponse> findCapacidadesByPlanId(@Param("planId") Long planId);
    void deleteByPlanId(Long planId);
}
