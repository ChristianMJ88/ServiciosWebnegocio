package com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio;

import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ExcepcionDisponibilidadEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExcepcionDisponibilidadRepositorio extends JpaRepository<ExcepcionDisponibilidadEntidad, Long> {
    @org.springframework.data.jpa.repository.Query("select e from ExcepcionDisponibilidadEntidad e where e.empresaId=:empresaId and e.fechaExcepcion between :desde and :hasta and ((e.tipoSujeto='SUCURSAL' and e.sujetoId=:sucursalId) or (e.tipoSujeto='PRESTADOR' and e.sujetoId in :prestadores))")
    List<ExcepcionDisponibilidadEntidad> buscarParaRango(Long empresaId, Long sucursalId, java.util.Collection<Long> prestadores, LocalDate desde, LocalDate hasta);

    List<ExcepcionDisponibilidadEntidad> findByEmpresaIdAndTipoSujetoAndSujetoIdAndFechaExcepcion(
            Long empresaId,
            String tipoSujeto,
            Long sujetoId,
            LocalDate fechaExcepcion
    );

    List<ExcepcionDisponibilidadEntidad> findByEmpresaIdOrderByFechaExcepcionDescTipoSujetoAsc(Long empresaId);

    List<ExcepcionDisponibilidadEntidad> findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByFechaExcepcionDescHoraInicioAsc(
            Long empresaId,
            String tipoSujeto,
            Long sujetoId
    );

    Optional<ExcepcionDisponibilidadEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    Optional<ExcepcionDisponibilidadEntidad> findByIdAndEmpresaIdAndTipoSujetoAndSujetoId(Long id, Long empresaId, String tipoSujeto, Long sujetoId);
}
