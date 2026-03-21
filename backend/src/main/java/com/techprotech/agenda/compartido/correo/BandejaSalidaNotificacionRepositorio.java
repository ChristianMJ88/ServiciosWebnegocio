package com.techprotech.agenda.compartido.correo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BandejaSalidaNotificacionRepositorio extends JpaRepository<BandejaSalidaNotificacionEntidad, Long>, BandejaSalidaNotificacionRepositorioCustom {
}
