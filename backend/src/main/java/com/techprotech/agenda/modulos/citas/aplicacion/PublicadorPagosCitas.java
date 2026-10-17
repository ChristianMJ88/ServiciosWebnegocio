package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.citas.dominio.eventos.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** No cambia saldos: publica el cobro persistido, una vez por cita y sin importes de productos/envío. */
@Component
public class PublicadorPagosCitas {
    private final CitaRepositorio citas;
    private final PublicadorEventosCita eventos;
    public PublicadorPagosCitas(CitaRepositorio citas, PublicadorEventosCita eventos) {
        this.citas = citas; this.eventos = eventos;
    }
    public void pagoDirecto(CitaEntidad cita, PagoCitaEntidad pago) {
        eventos.publicar(new PagoCitaRegistrado(eventos.datos(cita, OrigenEventoCita.ADMIN, pago.getRegistradoPorUsuarioId()),
                pago.getMonto(), cita.getMoneda(), PagoCitaRegistrado.TipoRegistro.PAGO_CITA, pago.getId(), null, null));
    }
    public void pagoEnVenta(CitaEntidad cita, VentaEntidad venta, PagoVentaEntidad pago, BigDecimal importe,
            OrigenEventoCita origen, Long actorId) {
        eventos.publicar(new PagoCitaRegistrado(eventos.datos(cita, origen, actorId), importe, venta.getMoneda(),
                PagoCitaRegistrado.TipoRegistro.PAGO_VENTA, pago.getId(), venta.getId(), venta.getCheckoutOrdenId()));
    }
    public void venta(VentaEntidad venta, PagoVentaEntidad pago, List<VentaDetalleEntidad> filas,
            OrigenEventoCita origen, Long actorId) {
        var montos = new LinkedHashMap<Long, BigDecimal>();
        for (var fila : filas) if ("SERVICIO".equals(fila.getTipoConcepto()) && fila.getCitaId() != null)
            montos.merge(fila.getCitaId(), fila.getTotal(), BigDecimal::add);
        if (montos.isEmpty()) return;
        var porId = citas.findByEmpresaIdAndIdIn(venta.getEmpresaId(), montos.keySet()).stream()
                .collect(Collectors.toMap(CitaEntidad::getId, Function.identity()));
        montos.forEach((id, monto) -> {
            var cita = porId.get(id);
            if (cita == null || !Objects.equals(cita.getEmpresaId(), venta.getEmpresaId())
                    || !Objects.equals(cita.getSucursalId(), venta.getSucursalId()))
                throw new IllegalStateException("La cita cobrada no pertenece al alcance de la venta");
            pagoEnVenta(cita, venta, pago, monto, origen, actorId);
        });
    }
}
