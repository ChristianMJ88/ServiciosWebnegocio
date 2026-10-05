package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.PlanModuloPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.api.dto.CapacidadModuloEmpresaResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface PlanModuloPlataformaRepositorio extends JpaRepository<PlanModuloPlataformaEntidad,PlanModuloPlataformaEntidad.Clave>{
    List<PlanModuloPlataformaEntidad> findByPlanId(Long planId);
    @Query("""
            select new com.techprotech.agenda.modulos.plataforma.api.dto.CapacidadModuloEmpresaResponse(
                modulo.codigo, modulo.nombre, relacion.limiteUso)
            from PlanModuloPlataformaEntidad relacion
            join ModuloPlataformaEntidad modulo on modulo.id = relacion.moduloId
            where relacion.planId = :planId and modulo.activo = true
            order by modulo.orden
            """)
    List<CapacidadModuloEmpresaResponse> findCapacidadesByPlanId(@Param("planId") Long planId);
    void deleteByPlanId(Long planId);
}
