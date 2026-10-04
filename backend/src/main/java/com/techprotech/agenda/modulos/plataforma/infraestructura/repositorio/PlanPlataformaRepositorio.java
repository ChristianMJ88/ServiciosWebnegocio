package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.PlanPlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PlanPlataformaRepositorio extends JpaRepository<PlanPlataformaEntidad,Long>{
    boolean existsByCodigoIgnoreCase(String codigo);
    Optional<PlanPlataformaEntidad> findByCodigoIgnoreCase(String codigo);
    List<PlanPlataformaEntidad> findAllByOrderByActivoDescNombreAsc();
}
