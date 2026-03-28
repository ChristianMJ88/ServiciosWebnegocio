package com.techprotech.agenda.compartido.correo;

import java.time.LocalDateTime;
import java.util.List;

public interface BandejaSalidaNotificacionRepositorioCustom {

    List<BandejaSalidaNotificacionEntidad> reclamarPendientesEmail(int limite, LocalDateTime ahora);

    List<BandejaSalidaNotificacionEntidad> reclamarPendientesPorCanal(String canal, int limite, LocalDateTime ahora);
}
