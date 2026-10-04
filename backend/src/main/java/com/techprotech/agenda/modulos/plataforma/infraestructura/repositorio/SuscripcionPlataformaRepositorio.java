package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.SuscripcionPlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SuscripcionPlataformaRepositorio extends JpaRepository<SuscripcionPlataformaEntidad,Long>{
    List<SuscripcionPlataformaEntidad> findAllByOrderByCreadaEnDesc();
    List<SuscripcionPlataformaEntidad> findByEmpresaIdAndEstadoIn(Long empresaId, List<String> estados);
}
