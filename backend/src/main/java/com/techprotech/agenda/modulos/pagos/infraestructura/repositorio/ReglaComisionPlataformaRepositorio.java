package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ReglaComisionPlataformaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReglaComisionPlataformaRepositorio extends JpaRepository<ReglaComisionPlataformaEntidad, Long> {
    List<ReglaComisionPlataformaEntidad> findAllByOrderByActivaDescPrioridadDescIdDesc();
    @Query("""
            select regla from ReglaComisionPlataformaEntidad regla
            where regla.proveedorPagoId = :proveedorId
              and regla.activa = true
              and (regla.empresaId = :empresaId or regla.empresaId is null)
              and regla.vigenteDesde <= :momento
              and (regla.vigenteHasta is null or regla.vigenteHasta >= :momento)
              and (regla.moneda is null or upper(regla.moneda) = upper(:moneda))
            order by case when regla.empresaId = :empresaId then 0 else 1 end, regla.prioridad desc, regla.id desc
            """)
    List<ReglaComisionPlataformaEntidad> resolver(@Param("proveedorId") Long proveedorId,
                                                   @Param("empresaId") Long empresaId,
                                                   @Param("moneda") String moneda,
                                                   @Param("momento") LocalDateTime momento);
}
