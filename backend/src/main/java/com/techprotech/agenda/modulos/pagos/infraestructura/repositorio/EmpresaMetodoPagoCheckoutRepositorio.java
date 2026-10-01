package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EmpresaMetodoPagoCheckoutEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaMetodoPagoCheckoutRepositorio extends JpaRepository<EmpresaMetodoPagoCheckoutEntidad, Long> {
    List<EmpresaMetodoPagoCheckoutEntidad> findByEmpresaIdAndProveedorPagoIdOrderByNombreAsc(Long empresaId, Long proveedorPagoId);
    Optional<EmpresaMetodoPagoCheckoutEntidad> findByEmpresaIdAndProveedorPagoIdAndMetodoExternoId(
            Long empresaId, Long proveedorPagoId, String metodoExternoId);
}
