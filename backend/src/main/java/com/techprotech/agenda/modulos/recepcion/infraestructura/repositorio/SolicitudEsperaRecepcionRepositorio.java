package com.techprotech.agenda.modulos.recepcion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.recepcion.infraestructura.entidad.SolicitudEsperaRecepcionEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SolicitudEsperaRecepcionRepositorio extends JpaRepository<SolicitudEsperaRecepcionEntidad, Long> {

    List<SolicitudEsperaRecepcionEntidad> findByEmpresaIdAndFechaDeseadaOrderByCreadaEnAsc(Long empresaId, LocalDate fechaDeseada);
}
