package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ConciliacionPagoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConciliacionPagoRepositorio extends JpaRepository<ConciliacionPagoEntidad, Long> {
    Optional<ConciliacionPagoEntidad> findByProveedorPagoIdAndReferenciaOrdenExterna(Long proveedorPagoId,
                                                                                      String referenciaOrdenExterna);
    List<ConciliacionPagoEntidad> findTop100ByEmpresaIdOrderByActualizadoEnDesc(Long empresaId);
}
