package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ReporteLiberacionPagoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReporteLiberacionPagoRepositorio extends JpaRepository<ReporteLiberacionPagoEntidad, Long> {
    Optional<ReporteLiberacionPagoEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
    List<ReporteLiberacionPagoEntidad> findTop50ByEmpresaIdOrderBySolicitadoEnDesc(Long empresaId);
}
