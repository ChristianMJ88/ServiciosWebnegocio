package com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio;

import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ReglaDisponibilidadEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReglaDisponibilidadRepositorio extends JpaRepository<ReglaDisponibilidadEntidad, Long> {

    List<ReglaDisponibilidadEntidad> findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(Long empresaId, String tipoSujeto, Long sujetoId, int diaSemana);

    List<ReglaDisponibilidadEntidad> findByEmpresaIdOrderByTipoSujetoAscSujetoIdAscDiaSemanaAscHoraInicioAsc(Long empresaId);

    List<ReglaDisponibilidadEntidad> findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByDiaSemanaAscHoraInicioAsc(Long empresaId, String tipoSujeto, Long sujetoId);

    Optional<ReglaDisponibilidadEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    Optional<ReglaDisponibilidadEntidad> findByIdAndEmpresaIdAndTipoSujetoAndSujetoId(Long id, Long empresaId, String tipoSujeto, Long sujetoId);
}
