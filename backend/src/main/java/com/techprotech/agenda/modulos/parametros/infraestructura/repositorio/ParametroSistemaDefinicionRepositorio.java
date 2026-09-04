package com.techprotech.agenda.modulos.parametros.infraestructura.repositorio;
import com.techprotech.agenda.modulos.parametros.infraestructura.entidad.ParametroSistemaDefinicionEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ParametroSistemaDefinicionRepositorio extends JpaRepository<ParametroSistemaDefinicionEntidad, String> {
    List<ParametroSistemaDefinicionEntidad> findAllByOrderByCategoriaAscOrdenAsc();
}
