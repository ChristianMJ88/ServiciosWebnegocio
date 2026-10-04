package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.LimitePlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LimitePlataformaRepositorio extends JpaRepository<LimitePlataformaEntidad,Long> {
    List<LimitePlataformaEntidad> findByActivoTrueOrderByOrdenAscNombreAsc();
}
