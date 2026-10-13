package com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio;

import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ReglaDisponibilidadEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReglaDisponibilidadRepositorio extends JpaRepository<ReglaDisponibilidadEntidad, Long> {
    @org.springframework.data.jpa.repository.Query("select r from ReglaDisponibilidadEntidad r where r.empresaId=:empresaId and ((r.tipoSujeto='SUCURSAL' and r.sujetoId=:sucursalId) or (r.tipoSujeto='PRESTADOR' and r.sujetoId in :prestadores))")
    List<ReglaDisponibilidadEntidad> buscarParaRango(Long empresaId, Long sucursalId, java.util.Collection<Long> prestadores);

    List<ReglaDisponibilidadEntidad> findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(Long empresaId, String tipoSujeto, Long sujetoId, int diaSemana);

    List<ReglaDisponibilidadEntidad> findByEmpresaIdOrderByTipoSujetoAscSujetoIdAscDiaSemanaAscHoraInicioAsc(Long empresaId);

    List<ReglaDisponibilidadEntidad> findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByDiaSemanaAscHoraInicioAsc(Long empresaId, String tipoSujeto, Long sujetoId);

    Optional<ReglaDisponibilidadEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    Optional<ReglaDisponibilidadEntidad> findByIdAndEmpresaIdAndTipoSujetoAndSujetoId(Long id, Long empresaId, String tipoSujeto, Long sujetoId);

    long countByEmpresaId(Long empresaId);
}
