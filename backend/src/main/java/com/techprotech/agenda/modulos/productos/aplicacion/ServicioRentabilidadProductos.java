package com.techprotech.agenda.modulos.productos.aplicacion;
import com.techprotech.agenda.modulos.productos.api.dto.RentabilidadProductosResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ServicioRentabilidadProductos {
    private final ServicioCatalogoProductos catalogo;
    public ServicioRentabilidadProductos(ServicioCatalogoProductos catalogo) { this.catalogo = catalogo; }
    @Transactional(readOnly = true)
    public RentabilidadProductosResponse consultar(Long empresaId, int pagina, String busqueda) {
        var datos = catalogo.obtenerCatalogo(empresaId, PageRequest.of(Math.max(0, pagina), 25), busqueda);
        var filas = datos.productos().stream().map(p -> {
            var diferencia = p.costo() == null ? null : p.precioBase().subtract(p.costo());
            return new RentabilidadProductosResponse.Fila(p.id(), p.sku(), p.nombre(), p.categoriaNombre(),
                p.moneda(), p.precioBase(), p.costo(), diferencia, margen(p.precioBase(), p.costo()));
        }).toList();
        return new RentabilidadProductosResponse(filas, datos.pagina(), datos.totalPaginas(), datos.totalElementos(),
            "Estimación sobre precio base y costo actual. No incluye descuentos, precios por zona, ajustes fiscales ni comisiones. No representa utilidad realizada: las ventas anteriores pueden no tener costo histórico del artículo.");
    }
    public static BigDecimal margen(BigDecimal precio, BigDecimal costo) {
        if (costo == null || precio.signum() == 0) return null;
        return precio.subtract(costo).multiply(BigDecimal.valueOf(100)).divide(precio, 2, RoundingMode.HALF_UP);
    }
}
