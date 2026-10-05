package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;

import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.ModuloSeccionAdminPlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface ModuloSeccionAdminPlataformaRepositorio extends JpaRepository<ModuloSeccionAdminPlataformaEntidad,ModuloSeccionAdminPlataformaEntidad.Clave> {
    List<ModuloSeccionAdminPlataformaEntidad> findByModuloIdIn(Collection<Long> moduloIds);
    void deleteByModuloId(Long moduloId);
}
