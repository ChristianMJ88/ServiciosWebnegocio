package com.techprotech.agenda.modulos.clientes.infraestructura.repositorio;

import com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClienteRepositorio extends JpaRepository<ClienteEntidad, Long> {

    List<ClienteEntidad> findByAceptaWhatsappTrue();

    @Query("""
            select c from ClienteEntidad c
            join UsuarioEntidad u on u.id = c.usuarioId
            where u.empresaId = :empresaId
            order by c.nombreCompleto asc
            """)
    List<ClienteEntidad> findByEmpresaId(Long empresaId);

    @Query("""
            select c from ClienteEntidad c
            where lower(c.nombreCompleto) like lower(concat('%', :texto, '%'))
               or c.telefono like concat('%', :texto, '%')
            order by c.nombreCompleto asc
            """)
    List<ClienteEntidad> buscarPorNombreOTelefono(String texto);

    @Query(value = """
            select c.usuario_id as id,
                   c.nombre_completo as nombreCompleto,
                   u.correo as correo,
                   c.telefono as telefono,
                   c.acepta_whatsapp as aceptaWhatsapp,
                   c.notas as notas,
                   count(ci.id) as totalCitas,
                   max(ci.inicio) as ultimaCita
            from cliente c
            join usuario u on u.id = c.usuario_id and u.empresa_id = :empresaId
            left join cita ci on ci.cliente_id = c.usuario_id and ci.empresa_id = :empresaId
            where (:filtro = ''
                or lower(c.nombre_completo) like concat('%', :filtro, '%')
                or lower(u.correo) like concat('%', :filtro, '%')
                or c.telefono like concat('%', :filtro, '%'))
            group by c.usuario_id, c.nombre_completo, u.correo, c.telefono, c.acepta_whatsapp, c.notas
            order by c.nombre_completo asc, c.usuario_id asc
            """, countQuery = """
            select count(*)
            from cliente c
            join usuario u on u.id = c.usuario_id and u.empresa_id = :empresaId
            where (:filtro = ''
                or lower(c.nombre_completo) like concat('%', :filtro, '%')
                or lower(u.correo) like concat('%', :filtro, '%')
                or c.telefono like concat('%', :filtro, '%'))
            """, nativeQuery = true)
    Page<ClienteResumen> buscarResumenes(
            @Param("empresaId") Long empresaId,
            @Param("filtro") String filtro,
            Pageable pageable
    );

    interface ClienteResumen {
        Long getId();
        String getNombreCompleto();
        String getCorreo();
        String getTelefono();
        boolean getAceptaWhatsapp();
        String getNotas();
        long getTotalCitas();
        LocalDateTime getUltimaCita();
    }
}
