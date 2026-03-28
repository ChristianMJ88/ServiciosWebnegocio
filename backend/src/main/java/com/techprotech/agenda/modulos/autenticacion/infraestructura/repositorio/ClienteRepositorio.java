package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClienteRepositorio extends JpaRepository<ClienteEntidad, Long> {

    List<ClienteEntidad> findByAceptaWhatsappTrue();

    @Query("""
            select c from ClienteEntidad c
            where lower(c.nombreCompleto) like lower(concat('%', :texto, '%'))
               or c.telefono like concat('%', :texto, '%')
            order by c.nombreCompleto asc
            """)
    List<ClienteEntidad> buscarPorNombreOTelefono(String texto);
}
