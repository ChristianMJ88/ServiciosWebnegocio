package com.techprotech.agenda.modulos.servicios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AsignacionServicioPrestadorRepositorio extends JpaRepository<AsignacionServicioPrestadorEntidad, AsignacionServicioPrestadorId> {
    @org.springframework.data.jpa.repository.Query("select a from AsignacionServicioPrestadorEntidad a " +
            "join ServicioEntidad s on s.id=a.id.servicioId join UsuarioEntidad u on u.id=a.id.prestadorId " +
            "where s.empresaId=:empresaId and u.empresaId=:empresaId and u.habilitado=true and a.id.servicioId=:servicioId and a.activa=true")
    List<AsignacionServicioPrestadorEntidad> findActivasPorEmpresaYServicio(
            @org.springframework.data.repository.query.Param("empresaId") Long empresaId,
            @org.springframework.data.repository.query.Param("servicioId") Long servicioId);

    List<AsignacionServicioPrestadorEntidad> findByIdServicioIdAndActivaTrue(Long servicioId);

    Optional<AsignacionServicioPrestadorEntidad> findByIdPrestadorIdAndIdServicioIdAndActivaTrue(Long prestadorId, Long servicioId);

    List<AsignacionServicioPrestadorEntidad> findByIdPrestadorId(Long prestadorId);

    List<AsignacionServicioPrestadorEntidad> findByIdPrestadorIdIn(Collection<Long> prestadorIds);
}
