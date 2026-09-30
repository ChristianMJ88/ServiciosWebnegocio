package com.techprotech.agenda.modulos.envios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EnvioPedidoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnvioPedidoRepositorio extends JpaRepository<EnvioPedidoEntidad, Long> {
    Optional<EnvioPedidoEntidad> findByEmpresaIdAndCheckoutOrdenId(Long empresaId, Long checkoutOrdenId);
    List<EnvioPedidoEntidad> findTop20ByEmpresaIdOrderByCreadoEnDesc(Long empresaId);
    List<EnvioPedidoEntidad> findTop50ByEstadoOrderByActualizadoEnAsc(String estado);
    List<EnvioPedidoEntidad> findTop50ByEstadoInAndNumeroGuiaIsNotNullOrderByRastreoActualizadoEnAsc(List<String> estados);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update EnvioPedidoEntidad e set e.estado = 'GENERANDO_GUIA', e.motivoBloqueo = null " +
            "where e.id = :id and e.empresaId = :empresaId and e.estado in ('LISTO_PARA_GUIA', 'ERROR_GUIA') " +
            "and e.numeroGuia is null")
    int reservarGeneracion(@Param("empresaId") Long empresaId, @Param("id") Long id);
}
