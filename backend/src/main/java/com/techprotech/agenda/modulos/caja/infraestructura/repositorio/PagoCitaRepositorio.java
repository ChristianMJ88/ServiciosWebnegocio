package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.PagoCitaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface PagoCitaRepositorio extends JpaRepository<PagoCitaEntidad, Long> {


    List<PagoCitaEntidad> findByEmpresaIdAndCitaIdOrderByRegistradoEnDesc(Long empresaId, Long citaId);

    List<PagoCitaEntidad> findByEmpresaIdAndCajaSesionIdOrderByRegistradoEnDesc(Long empresaId, Long cajaSesionId);

    @Query("select coalesce(sum(p.monto), 0) from PagoCitaEntidad p where p.empresaId = :empresaId and p.citaId = :citaId")
    BigDecimal sumarPagadoPorCita(Long empresaId, Long citaId);

    /**
     * Suma de pagos agrupada por cita — reemplaza N llamadas a sumarPagadoPorCita() en un loop.
     * Retorna filas [citaId, total].
     */
    @Query("select p.citaId, coalesce(sum(p.monto), 0) from PagoCitaEntidad p " +
           "where p.empresaId = :empresaId and p.citaId in :citaIds group by p.citaId")
    List<Object[]> sumarPagadoPorCitas(@Param("empresaId") Long empresaId,
                                       @Param("citaIds") Collection<Long> citaIds);
}
