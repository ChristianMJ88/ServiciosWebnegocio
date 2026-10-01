package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.TerminalMercadoPagoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerminalMercadoPagoRepositorio extends JpaRepository<TerminalMercadoPagoEntidad, Long> {
    List<TerminalMercadoPagoEntidad> findByEmpresaIdOrderByNombreAsc(Long empresaId);
    Optional<TerminalMercadoPagoEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
    Optional<TerminalMercadoPagoEntidad> findByEmpresaIdAndTerminalId(Long empresaId, String terminalId);
}
