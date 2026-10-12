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

    @Query("select c from ClienteEntidad c join UsuarioEntidad u on u.id=c.usuarioId where u.empresaId=:empresaId and c.usuarioId=:clienteId")
    java.util.Optional<ClienteEntidad> findByClienteIdAndEmpresaId(Long clienteId,Long empresaId);
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
            select c.usuario_id as id, c.nombre_completo as nombreCompleto, u.correo as correo,
                   c.telefono as telefono, c.acepta_whatsapp as aceptaWhatsapp, c.notas as notas,
                   coalesce(agenda.totalCitas,0) as totalCitas, agenda.ultimaCita as ultimaCita,
                   agenda.ultimaVisita as ultimaVisita, coalesce(agenda.citasAgendadas,0) as citasAgendadas

            from cliente c
            join usuario u on u.id = c.usuario_id and u.empresa_id = :empresaId
            left join (
                select cliente_id, count(*) as totalCitas, max(inicio) as ultimaCita,
                       max(case when estado = 'FINALIZADA' then inicio end) as ultimaVisita,
                       sum(case when estado in ('PENDIENTE', 'CONFIRMADA') and inicio > :ahora then 1 else 0 end) as citasAgendadas
                from cita where empresa_id = :empresaId group by cliente_id
            ) agenda on agenda.cliente_id = c.usuario_id
            where (:filtro = '' or lower(c.nombre_completo) like concat('%', :filtro, '%')
                or lower(u.correo) like concat('%', :filtro, '%') or c.telefono like concat('%', :filtro, '%'))
              and (:nombre = '' or lower(c.nombre_completo) like concat('%', :nombre, '%'))
              and (:telefono = '' or c.telefono like concat('%', :telefono, '%'))
              and (:visitaDesde is null or date(agenda.ultimaVisita) >= :visitaDesde)
              and (:visitaHasta is null or date(agenda.ultimaVisita) <= :visitaHasta)
              and (:citas = 'TODOS' or (:citas = 'CON_AGENDADAS' and coalesce(agenda.citasAgendadas,0) > 0)
                   or (:citas = 'SIN_AGENDADAS' and coalesce(agenda.citasAgendadas,0) = 0))

            order by
                case when :orden = 'NOMBRE_ASC' then c.nombre_completo end asc,
                case when :orden = 'NOMBRE_DESC' then c.nombre_completo end desc,
                case when :orden = 'TELEFONO_ASC' then c.telefono end asc,
                case when :orden = 'TELEFONO_DESC' then c.telefono end desc,
                case when :orden like 'ULTIMA_VISITA%' then agenda.ultimaVisita is null end asc,
                case when :orden = 'ULTIMA_VISITA_DESC' then agenda.ultimaVisita end desc,
                case when :orden = 'ULTIMA_VISITA_ASC' then agenda.ultimaVisita end asc,
                case when :orden = 'AGENDADAS_DESC' then coalesce(agenda.citasAgendadas,0) end desc,
                c.usuario_id asc
            """, countQuery = """
            select count(*)

            from cliente c
            join usuario u on u.id = c.usuario_id and u.empresa_id = :empresaId
            left join (
                select cliente_id, count(*) as totalCitas, max(inicio) as ultimaCita,
                       max(case when estado = 'FINALIZADA' then inicio end) as ultimaVisita,
                       sum(case when estado in ('PENDIENTE', 'CONFIRMADA') and inicio > :ahora then 1 else 0 end) as citasAgendadas
                from cita where empresa_id = :empresaId group by cliente_id
            ) agenda on agenda.cliente_id = c.usuario_id
            where (:filtro = '' or lower(c.nombre_completo) like concat('%', :filtro, '%')
                or lower(u.correo) like concat('%', :filtro, '%') or c.telefono like concat('%', :filtro, '%'))
              and (:nombre = '' or lower(c.nombre_completo) like concat('%', :nombre, '%'))
              and (:telefono = '' or c.telefono like concat('%', :telefono, '%'))
              and (:visitaDesde is null or date(agenda.ultimaVisita) >= :visitaDesde)
              and (:visitaHasta is null or date(agenda.ultimaVisita) <= :visitaHasta)
              and (:citas = 'TODOS' or (:citas = 'CON_AGENDADAS' and coalesce(agenda.citasAgendadas,0) > 0)
                   or (:citas = 'SIN_AGENDADAS' and coalesce(agenda.citasAgendadas,0) = 0))

            """, nativeQuery = true)
    Page<ClienteResumen> buscarResumenes(
            @Param("empresaId") Long empresaId, @Param("filtro") String filtro,
            @Param("nombre") String nombre, @Param("telefono") String telefono,
            @Param("visitaDesde") java.time.LocalDate visitaDesde, @Param("visitaHasta") java.time.LocalDate visitaHasta,
            @Param("citas") String citas, @Param("orden") String orden,
            @Param("ahora") LocalDateTime ahora, Pageable pageable
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
        LocalDateTime getUltimaVisita();
        long getCitasAgendadas();
    }
}
