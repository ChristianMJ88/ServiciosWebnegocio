package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EmpresaProveedorPagoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpresaProveedorPagoRepositorio extends JpaRepository<EmpresaProveedorPagoEntidad, Long> {
    Optional<EmpresaProveedorPagoEntidad> findByEmpresaIdAndProveedorPagoIdAndAmbiente(Long empresaId, Long proveedorPagoId, String ambiente);
    Optional<EmpresaProveedorPagoEntidad> findByProveedorPagoIdAndCuentaExternaId(Long proveedorPagoId, String cuentaExternaId);
}
