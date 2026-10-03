package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.api.dto.ConciliacionPagoResponse;
import com.techprotech.agenda.modulos.pagos.dominio.DesgloseLiquidacionPago;
import com.techprotech.agenda.modulos.pagos.dominio.EstadoPago;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoCobro;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ConciliacionPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.IntentoPagoCheckoutEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ClienteMercadoPagoPoint;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ConciliacionPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioConciliacionPagos {
    private static final BigDecimal CERO = new BigDecimal("0.00");

    private final ConciliacionPagoRepositorio conciliaciones;
    private final ProveedorPagoRepositorio proveedores;

    public ServicioConciliacionPagos(ConciliacionPagoRepositorio conciliaciones,
                                     ProveedorPagoRepositorio proveedores) {
        this.conciliaciones = conciliaciones;
        this.proveedores = proveedores;
    }

    @Transactional
    public void registrarCheckout(IntentoPagoCheckoutEntidad intento, ResultadoCobro resultado) {
        if (!registrable(resultado.estado(), resultado.liquidacion()) || resultado.referenciaExterna() == null) return;
        ConciliacionPagoEntidad entidad = obtener(intento.getProveedorPagoId(), resultado.referenciaExterna());
        entidad.setEmpresaId(intento.getEmpresaId());
        entidad.setProveedorPagoId(intento.getProveedorPagoId());
        entidad.setCanal("CHECKOUT");
        entidad.setCheckoutOrdenId(intento.getCheckoutOrdenId());
        entidad.setIntentoPagoCheckoutId(intento.getId());
        entidad.setReferenciaOrdenExterna(resultado.referenciaExterna());
        entidad.setReferenciaPagoExterna(resultado.referenciaPagoExterna());
        aplicar(entidad, resultado.estado(), resultado.liquidacion(), intento.getImporte(), intento.getMoneda());
        conciliaciones.save(entidad);
    }

    @Transactional
    public void registrarPoint(OrdenPointCajaEntidad orden, ClienteMercadoPagoPoint.OrdenRemota remota) {
        String estado = orden.getEstado();
        if (!("APROBADO".equals(estado) || "REEMBOLSADO".equals(estado)) || remota.ordenId() == null) return;
        ProveedorPagoEntidad proveedor = proveedores.findByCodigoAndActivoTrue("MERCADO_PAGO")
                .orElseThrow(() -> new IllegalStateException("Proveedor Mercado Pago no disponible"));
        ConciliacionPagoEntidad entidad = obtener(proveedor.getId(), remota.ordenId());
        entidad.setEmpresaId(orden.getEmpresaId());
        entidad.setProveedorPagoId(proveedor.getId());
        entidad.setCanal("POINT");
        entidad.setOrdenPointCajaId(orden.getId());
        entidad.setVentaId(orden.getVentaId());
        entidad.setReferenciaOrdenExterna(remota.ordenId());
        entidad.setReferenciaPagoExterna(remota.pagoId());
        EstadoPago estadoPago = "REEMBOLSADO".equals(estado) ? EstadoPago.REEMBOLSADO : EstadoPago.APROBADO;
        aplicar(entidad, estadoPago, remota.liquidacion(), orden.getImporte(), orden.getMoneda());
        conciliaciones.save(entidad);
    }

    @Transactional(readOnly = true)
    public List<ConciliacionPagoResponse> listar(Long empresaId) {
        List<ConciliacionPagoEntidad> registros = conciliaciones.findTop100ByEmpresaIdOrderByActualizadoEnDesc(empresaId);
        Map<Long, ProveedorPagoEntidad> proveedoresPorId = proveedores.findAllById(
                        registros.stream().map(ConciliacionPagoEntidad::getProveedorPagoId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(ProveedorPagoEntidad::getId, Function.identity()));
        return registros.stream().map(item -> respuesta(item, proveedoresPorId)).toList();
    }

    private ConciliacionPagoEntidad obtener(Long proveedorId, String referenciaOrden) {
        return conciliaciones.findByProveedorPagoIdAndReferenciaOrdenExterna(proveedorId, referenciaOrden)
                .orElseGet(ConciliacionPagoEntidad::new);
    }

    private void aplicar(ConciliacionPagoEntidad entidad, EstadoPago estado, DesgloseLiquidacionPago desglose,
                         BigDecimal brutoPredeterminado, String monedaPredeterminada) {
        entidad.setImporteBruto(dinero(valor(desglose == null ? null : desglose.importeBruto(), brutoPredeterminado)));
        entidad.setMoneda(valorTexto(desglose == null ? null : desglose.moneda(), monedaPredeterminada));
        entidad.setImporteReembolsado(dinero(valor(desglose == null ? null : desglose.importeReembolsado(), CERO)));
        entidad.setFuente("API_ORDEN");
        if (desglose != null) {
            entidad.setComisionProveedorTotal(dinero(desglose.comisionProveedorTotal()));
            entidad.setImpuestoComisionProveedor(dinero(desglose.impuestoComisionProveedor()));
            entidad.setComisionPlataforma(dinero(desglose.comisionPlataforma()));
            entidad.setImpuestoComisionPlataforma(dinero(desglose.impuestoComisionPlataforma()));
            entidad.setRetenciones(dinero(desglose.retenciones()));
            entidad.setImporteNeto(dinero(desglose.importeNeto()));
            entidad.setLiquidacionDefinitiva(desglose.definitiva());
            entidad.setLiquidadoEn(desglose.liquidadoEn() == null ? null
                    : desglose.liquidadoEn().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        }
        entidad.setEstado(estado == EstadoPago.REEMBOLSADO ? "REEMBOLSADA"
                : entidad.isLiquidacionDefinitiva() ? "CONCILIADA" : "PENDIENTE_CONCILIACION");
    }

    private boolean registrable(EstadoPago estado, DesgloseLiquidacionPago desglose) {
        return estado == EstadoPago.APROBADO || estado == EstadoPago.REEMBOLSADO || desglose != null;
    }

    private ConciliacionPagoResponse respuesta(ConciliacionPagoEntidad item,
                                               Map<Long, ProveedorPagoEntidad> proveedoresPorId) {
        ProveedorPagoEntidad proveedorEntidad = proveedoresPorId.get(item.getProveedorPagoId());
        String proveedor = proveedorEntidad == null ? "DESCONOCIDO" : proveedorEntidad.getCodigo();
        return new ConciliacionPagoResponse(item.getId(), item.getCanal(), item.getEstado(), proveedor,
                item.getReferenciaOrdenExterna(), item.getReferenciaPagoExterna(), item.getMoneda(), item.getImporteBruto(),
                item.getComisionProveedorTotal(), item.getImpuestoComisionProveedor(), item.getComisionPlataforma(),
                item.getImpuestoComisionPlataforma(), item.getRetenciones(), item.getImporteReembolsado(),
                item.getImporteNeto(), item.isLiquidacionDefinitiva(), item.getLiquidadoEn(), item.getActualizadoEn());
    }

    private BigDecimal dinero(BigDecimal valor) { return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal valor(BigDecimal preferido, BigDecimal respaldo) { return preferido == null ? respaldo : preferido; }
    private String valorTexto(String preferido, String respaldo) { return preferido == null || preferido.isBlank() ? respaldo : preferido; }
}
