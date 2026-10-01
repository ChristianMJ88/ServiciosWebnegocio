package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenPointCajaRepositorio extends JpaRepository<OrdenPointCajaEntidad, Long> {
    Optional<OrdenPointCajaEntidad> findByEmpresaIdAndPublicId(Long empresaId, String publicId);
    Optional<OrdenPointCajaEntidad> findByProveedorOrdenId(String proveedorOrdenId);
    Optional<OrdenPointCajaEntidad> findByEmpresaIdAndIdempotencia(Long empresaId, String idempotencia);
    List<OrdenPointCajaEntidad> findTop50ByEmpresaIdOrderByCreadoEnDesc(Long empresaId);
}
