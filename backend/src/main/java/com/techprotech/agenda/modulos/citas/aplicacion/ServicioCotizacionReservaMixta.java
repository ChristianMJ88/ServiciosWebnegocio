package com.techprotech.agenda.modulos.citas.aplicacion;
import com.techprotech.agenda.modulos.citas.api.dto.*;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioCotizacionProductos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ServicioCotizacionReservaMixta {
    private final ServicioCotizacionCitas citas;
    private final ServicioCotizacionProductos productos;
    private final ServicioPoliticaReservaPago politica;
    public ServicioCotizacionReservaMixta(ServicioCotizacionCitas citas,ServicioCotizacionProductos productos,ServicioPoliticaReservaPago politica) { this.citas=citas;this.productos=productos;this.politica=politica; }
    @Transactional(readOnly=true)
    public ResultadoCotizacionCitas cotizar(CotizarCitasRequest request) {
        var cotizacion=citas.cotizar(request.empresaId(),request.sucursalId(),request.promocionId(),request.items());
        if(request.productos()==null || request.productos().isEmpty()) return cotizacion;
        Map<Long,BigDecimal> cantidades=new LinkedHashMap<>();
        request.productos().forEach(p -> cantidades.merge(p.productoId(),p.cantidad(),BigDecimal::add));
        var lineas=productos.cotizar(request.empresaId(),request.sucursalId(),null,cantidades).productos();
        if(lineas.stream().anyMatch(p -> !cotizacion.moneda().equalsIgnoreCase(p.moneda()))) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Productos y servicios deben usar la misma moneda");
        BigDecimal lista=lineas.stream().map(p -> p.precioListaUnitario().multiply(p.cantidad())).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
        BigDecimal neto=lineas.stream().map(ServicioCotizacionProductos.CotizacionProducto::total).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
        boolean online=politica.puedeCobrarProductosOnline(request.empresaId());
        var requerido=(cotizacion.requierePago()?cotizacion.importeRequeridoTotal():BigDecimal.ZERO).add(online?neto:BigDecimal.ZERO);
        return new ResultadoCotizacionCitas(cotizacion.moneda(),cotizacion.subtotalLista().add(lista),cotizacion.descuentoTotal().add(lista.subtract(neto)),cotizacion.total().add(neto),requerido,
                cotizacion.promocion(),cotizacion.motivoNoAplicada(),cotizacion.lineas(),cotizacion.requierePago() || online && neto.signum()>0,requerido,List.copyOf(lineas),online && neto.signum()>0,
                online?null:"El negocio no tiene habilitada la compra de productos en línea. Tu carrito se conserva.",
                cotizacion.reservableEnLinea(), cotizacion.motivoNoReservable());
    }
}
