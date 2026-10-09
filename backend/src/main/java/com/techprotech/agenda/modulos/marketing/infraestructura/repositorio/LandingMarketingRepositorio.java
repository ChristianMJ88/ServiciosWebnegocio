package com.techprotech.agenda.modulos.marketing.infraestructura.repositorio;
import com.techprotech.agenda.modulos.marketing.infraestructura.entidad.LandingMarketingEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LandingMarketingRepositorio extends JpaRepository<LandingMarketingEntidad,Long> {
 Optional<LandingMarketingEntidad> findBySlug(String slug);
 Optional<LandingMarketingEntidad> findBySlugAndPublicadoTrue(String slug);
 List<LandingMarketingEntidad> findAllByPublicadoTrueOrderBySlugAsc();
}
