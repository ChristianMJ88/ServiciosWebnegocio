package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VentaRepositorio extends JpaRepository<VentaEntidad, Long> {
    List<VentaEntidad> findTop50ByEmpresaIdAndSucursalIdOrderByCreadaEnDesc(Long empresaId, Long sucursalId);
    boolean existsByEmpresaIdAndCheckoutOrdenId(Long empresaId, Long checkoutOrdenId);
}
