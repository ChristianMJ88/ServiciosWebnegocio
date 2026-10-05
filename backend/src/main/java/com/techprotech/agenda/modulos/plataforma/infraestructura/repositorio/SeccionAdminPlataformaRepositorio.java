package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;

import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.SeccionAdminPlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SeccionAdminPlataformaRepositorio extends JpaRepository<SeccionAdminPlataformaEntidad,Long> {
    List<SeccionAdminPlataformaEntidad> findByActivoTrueOrderByOrdenAscNombreAsc();

    @Query("select s.codigo from SeccionAdminPlataformaEntidad s where s.activo=true and s.requiereModulo=false order by s.orden")
    List<String> findCodigosSinModulo();

    @Query(value="""
            SELECT s.codigo
            FROM plataforma_seccion_admin s
            LEFT JOIN plataforma_modulo_seccion_admin ms ON ms.seccion_codigo=s.codigo
            LEFT JOIN plataforma_plan_modulo pm ON pm.modulo_id=ms.modulo_id AND pm.plan_id=:planId
            WHERE s.activo=TRUE AND (s.requiere_modulo=FALSE OR pm.plan_id IS NOT NULL)
            GROUP BY s.codigo,s.orden
            ORDER BY s.orden
            """,nativeQuery=true)
    List<String> findCodigosDisponiblesByPlanId(@Param("planId") Long planId);
}
