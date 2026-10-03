package com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio;

import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.UsuarioPlataformaEntidad;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccesoPlataformaRepositorio extends Repository<UsuarioPlataformaEntidad, Long> {
    @Query(value = """
            SELECT DISTINCT r.codigo
            FROM plataforma_usuario_rol ur
            JOIN plataforma_rol r ON r.id = ur.rol_id AND r.activo = TRUE
            WHERE ur.usuario_id = :usuarioId
            ORDER BY r.codigo
            """, nativeQuery = true)
    List<String> roles(@Param("usuarioId") Long usuarioId);

    @Query(value = """
            SELECT DISTINCT p.codigo
            FROM plataforma_usuario_rol ur
            JOIN plataforma_rol r ON r.id = ur.rol_id AND r.activo = TRUE
            JOIN plataforma_rol_permiso rp ON rp.rol_id = r.id
            JOIN plataforma_permiso p ON p.id = rp.permiso_id
            WHERE ur.usuario_id = :usuarioId
            ORDER BY p.codigo
            """, nativeQuery = true)
    List<String> permisos(@Param("usuarioId") Long usuarioId);

    @Query(value = """
            INSERT INTO plataforma_usuario_rol (usuario_id, rol_id)
            SELECT :usuarioId, id FROM plataforma_rol WHERE codigo = :codigoRol
            """, nativeQuery = true)
    @org.springframework.data.jpa.repository.Modifying
    void asignarRol(@Param("usuarioId") Long usuarioId, @Param("codigoRol") String codigoRol);
}
