package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.TerminalMercadoPagoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerminalMercadoPagoRepositorio extends JpaRepository<TerminalMercadoPagoEntidad, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from TerminalMercadoPagoEntidad t where t.id=:id and t.empresaId=:empresaId")
    Optional<TerminalMercadoPagoEntidad> bloquear(Long id, Long empresaId);
    List<TerminalMercadoPagoEntidad> findByEmpresaIdOrderByNombreAsc(Long empresaId);
    Optional<TerminalMercadoPagoEntidad> findByIdAndEmpresaId(Long id, Long empresaId);
    Optional<TerminalMercadoPagoEntidad> findByEmpresaIdAndTerminalId(Long empresaId, String terminalId);
}
