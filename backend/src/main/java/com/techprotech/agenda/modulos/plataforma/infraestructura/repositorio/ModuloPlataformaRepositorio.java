package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.ModuloPlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ModuloPlataformaRepositorio extends JpaRepository<ModuloPlataformaEntidad,Long>{
    List<ModuloPlataformaEntidad> findByActivoTrueOrderByOrdenAscNombreAsc();
}
