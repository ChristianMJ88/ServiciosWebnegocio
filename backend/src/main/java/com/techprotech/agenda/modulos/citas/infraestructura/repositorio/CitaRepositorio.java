package com.techprotech.agenda.modulos.citas.infraestructura.repositorio;

import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaRepositorio extends JpaRepository<CitaEntidad, Long> {

    @Query("""
            select c from CitaEntidad c
            where c.prestadorId = :prestadorId
              and c.estado in :estados
              and c.inicio < :fin
              and c.fin > :inicio
            """)
    List<CitaEntidad> buscarConflictos(Long prestadorId, LocalDateTime inicio, LocalDateTime fin, List<String> estados);

    List<CitaEntidad> findByEmpresaIdAndClienteIdOrderByInicioDesc(Long empresaId, Long clienteId);

    Optional<CitaEntidad> findByIdAndEmpresaIdAndClienteId(Long id, Long empresaId, Long clienteId);

    List<CitaEntidad> findByEmpresaIdAndPrestadorIdAndInicioBetweenOrderByInicioAsc(
            Long empresaId,
            Long prestadorId,
            LocalDateTime inicio,
            LocalDateTime fin
    );

    Optional<CitaEntidad> findByIdAndEmpresaIdAndPrestadorId(Long id, Long empresaId, Long prestadorId);

    Optional<CitaEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    List<CitaEntidad> findByEmpresaIdOrderByInicioDesc(Long empresaId);

    List<CitaEntidad> findByEmpresaIdAndEstadoInOrderByInicioDesc(Long empresaId, Collection<String> estados);

    List<CitaEntidad> findByEmpresaIdAndInicioBetweenOrderByInicioAsc(
            Long empresaId,
            LocalDateTime inicio,
            LocalDateTime fin
    );

    List<CitaEntidad> findByEmpresaIdAndInicioBetweenAndEstadoInOrderByInicioAsc(
            Long empresaId,
            LocalDateTime inicio,
            LocalDateTime fin,
            List<String> estados
    );

    List<CitaEntidad> findByEmpresaIdAndInicioBeforeAndEstadoOrderByInicioAsc(
            Long empresaId,
            LocalDateTime inicio,
            String estado
    );

    long countByEmpresaId(Long empresaId);

    long countByEmpresaIdAndEstado(Long empresaId, String estado);
}
