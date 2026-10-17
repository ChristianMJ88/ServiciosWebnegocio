package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.citas.aplicacion.PublicadorPagosCitas;
import com.techprotech.agenda.modulos.citas.dominio.eventos.OrigenEventoCita;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.PagoVentaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaDetalleEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.PagoVentaRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaDetalleRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.IntentoPagoCheckoutEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.IntentoPagoCheckoutRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServicioVentasCheckout {
    private final PublicadorPagosCitas eventosPago;
    private final java.time.Clock reloj;
    private final CheckoutOrdenRepositorio ordenes;
    private final CheckoutOrdenDetalleRepositorio detallesCheckout;
    private final IntentoPagoCheckoutRepositorio intentos;
    private final VentaRepositorio ventas;
    private final VentaDetalleRepositorio detallesVenta;
    private final PagoVentaRepositorio pagos;
    private final com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisionesDevengadas comisiones;

    public ServicioVentasCheckout(CheckoutOrdenRepositorio ordenes, CheckoutOrdenDetalleRepositorio detallesCheckout,
                                  IntentoPagoCheckoutRepositorio intentos, VentaRepositorio ventas,
                                  VentaDetalleRepositorio detallesVenta, PagoVentaRepositorio pagos,
                                  com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisionesDevengadas comisiones, java.time.Clock reloj, PublicadorPagosCitas eventosPago) {
        this.eventosPago = eventosPago;
        this.reloj = reloj;
        this.ordenes = ordenes;
        this.detallesCheckout = detallesCheckout;
        this.intentos = intentos;
        this.ventas = ventas;
        this.detallesVenta = detallesVenta;
        this.pagos = pagos;
        this.comisiones = comisiones;
    }

    @Transactional
    public void proyectar(Long empresaId, Long ordenId) {
        CheckoutOrdenEntidad orden = ordenes.bloquearPorEmpresaEId(empresaId, ordenId)
                .orElseThrow(() -> new IllegalStateException("Orden de checkout no encontrada"));
        if (!java.util.Set.of("PAGADA", "PAGADA_CON_INCIDENCIA").contains(orden.getEstado()) || ventas.existsByEmpresaIdAndCheckoutOrdenId(empresaId, ordenId)) return;
        IntentoPagoCheckoutEntidad intento = intentos
                .findFirstByEmpresaIdAndCheckoutOrdenIdAndEstadoOrderByIdDesc(empresaId, ordenId, "APROBADO")
                .orElseThrow(() -> new IllegalStateException("La orden pagada no tiene un intento aprobado"));

        VentaEntidad venta = new VentaEntidad();
        venta.setEmpresaId(empresaId);
        venta.setSucursalId(orden.getSucursalId());
        venta.setCheckoutOrdenId(ordenId);
        venta.setFolio("WEB-" + orden.getPublicId().toUpperCase());
        venta.setEstado("PAGADA");
        venta.setCanal("ONLINE");
        venta.setMoneda(orden.getMoneda());
        venta.setSubtotal(orden.getSubtotal());
        venta.setDescuentoTotal(orden.getDescuentoTotal());
        venta.setImpuestosTotal(orden.getImpuestosTotal());
        venta.setTotal(orden.getTotal());
        venta.setObservaciones(orden.isRequiereConciliacion() ? "Pago con incidencia: " + orden.getMotivoIncidencia() : "Venta creada desde el checkout público");
        venta.setCreadaEn(orden.getCreadaEn() == null ? LocalDateTime.now(reloj) : orden.getCreadaEn());
        venta = ventas.save(venta);

        List<CheckoutOrdenDetalleEntidad> origen = detallesCheckout
                .findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(empresaId, ordenId);
        Long ventaId = venta.getId();
        var filas=detallesVenta.saveAll(origen.stream().map(item -> detalle(empresaId, ventaId, item)).toList());

        PagoVentaEntidad pago = new PagoVentaEntidad();
        pago.setEmpresaId(empresaId);
        pago.setVentaId(ventaId);
        pago.setIntentoPagoCheckoutId(intento.getId());
        pago.setMonto(intento.getImporte());
        pago.setMetodoPago("MERCADO_PAGO");
        pago.setReferencia(intento.getReferenciaExterna());
        pago.setRegistradoEn(LocalDateTime.now(reloj));
        pago = pagos.save(pago);
        comisiones.registrar(venta, filas);
        eventosPago.venta(venta, pago, filas, OrigenEventoCita.WEBHOOK, null);
    }

    private VentaDetalleEntidad detalle(Long empresaId, Long ventaId, CheckoutOrdenDetalleEntidad item) {
        VentaDetalleEntidad detalle = new VentaDetalleEntidad();
        detalle.setEmpresaId(empresaId);
        detalle.setVentaId(ventaId);
        detalle.setTipoConcepto(item.getTipoConcepto());
        detalle.setProductoId(item.getProductoId());
        detalle.setCostoUnitarioHistorico(item.getCostoUnitarioHistorico());
        detalle.setServicioId(item.getServicioId());
        detalle.setCitaId(item.getCitaId());
        detalle.setNombre(item.getNombre());
        detalle.setCantidad(item.getCantidad());
        detalle.setPrecioUnitario("SERVICIO".equals(item.getTipoConcepto()) ? item.getPrecioListaUnitario() : item.getPrecioUnitario());
        detalle.setDescuento(item.getDescuento());
        detalle.setImpuestos(item.getImpuestos());
        detalle.setTotal(item.getTotal());
        return detalle;
    }
}
