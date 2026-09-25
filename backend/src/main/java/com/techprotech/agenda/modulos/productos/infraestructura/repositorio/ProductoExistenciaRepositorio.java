package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoExistenciaEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoExistenciaId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoExistenciaRepositorio extends JpaRepository<ProductoExistenciaEntidad, ProductoExistenciaId> {
    List<ProductoExistenciaEntidad> findByIdEmpresaIdAndIdSucursalIdIn(Long empresaId, List<Long> sucursalIds);

    @Modifying
    @Query(value = "INSERT IGNORE INTO producto_existencia (empresa_id, producto_id, sucursal_id, existencia, reservado) VALUES (:empresaId, :productoId, :sucursalId, 0, 0)", nativeQuery = true)
    void asegurarFila(@Param("empresaId") Long empresaId, @Param("productoId") Long productoId, @Param("sucursalId") Long sucursalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ProductoExistenciaEntidad e where e.id.empresaId = :empresaId and e.id.productoId = :productoId and e.id.sucursalId = :sucursalId")
    Optional<ProductoExistenciaEntidad> bloquear(@Param("empresaId") Long empresaId, @Param("productoId") Long productoId, @Param("sucursalId") Long sucursalId);
}
