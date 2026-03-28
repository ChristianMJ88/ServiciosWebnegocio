package com.techprotech.agenda.compartido.correo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BandejaSalidaNotificacionRepositorio extends JpaRepository<BandejaSalidaNotificacionEntidad, Long>, BandejaSalidaNotificacionRepositorioCustom {

    boolean existsByAgregadoIdAndCanalAndTipoEventoAndEstadoIn(Long agregadoId, String canal, String tipoEvento, Collection<String> estados);

    Optional<BandejaSalidaNotificacionEntidad> findFirstByCanalAndProveedorMensajeId(String canal, String proveedorMensajeId);

    List<BandejaSalidaNotificacionEntidad> findTop50ByEmpresaIdAndCanalOrderByIdDesc(Long empresaId, String canal);
}
