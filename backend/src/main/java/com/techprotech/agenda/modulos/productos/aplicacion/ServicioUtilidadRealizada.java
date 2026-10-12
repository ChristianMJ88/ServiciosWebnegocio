package com.techprotech.agenda.modulos.productos.aplicacion;
import com.techprotech.agenda.modulos.productos.api.dto.UtilidadRealizadaResponse;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.RentabilidadVentasRepositorio;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ServicioUtilidadRealizada {
    private final RentabilidadVentasRepositorio repositorio;
    public ServicioUtilidadRealizada(RentabilidadVentasRepositorio repositorio) { this.repositorio=repositorio; }
    @Transactional(readOnly=true)
    public UtilidadRealizadaResponse consultar(Long empresaId, LocalDate desde, LocalDate hasta, Long sucursalId, String busqueda, int pagina) {
        if (desde.isAfter(hasta) || hasta.equals(LocalDate.MAX)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revisa el periodo del reporte");
        var resultado=repositorio.consultar(empresaId,desde.atStartOfDay(),hasta.plusDays(1).atStartOfDay(),sucursalId,
            busqueda.trim().toLowerCase(Locale.ROOT),PageRequest.of(Math.max(0,pagina),25));
        return new UtilidadRealizadaResponse(resultado.getContent().stream().map(ServicioUtilidadRealizada::mapear).toList(),
            resultado.getNumber(),resultado.getTotalPages(),resultado.getTotalElements(),
            "Ventas pagadas de productos, agrupadas por artículo y moneda, según fecha de venta. Ventas netas = total de línea menos impuestos registrados; los descuentos ya están incluidos. UB = ventas netas menos costo histórico. Sin costo en alguna línea, costo, UB y margen quedan sin calcular. No incluye comisiones ni ajustes de devoluciones no registrados. Los impuestos dependen de lo registrado en la venta.");
    }
    public static UtilidadRealizadaResponse.Fila mapear(RentabilidadVentasRepositorio.Fila f) {
        boolean completo=f.getLineasSinCosto()==0;
        return new UtilidadRealizadaResponse.Fila(f.getProductoId(),f.getNombre(),f.getSku(),f.getMoneda(),f.getCantidad(),
            f.getVentasNetas(),completo ? f.getCostoConocido():null,completo ? f.getVentasNetas().subtract(f.getCostoConocido()):null,
            completo ? ServicioRentabilidadProductos.margen(f.getVentasNetas(),f.getCostoConocido()):null,f.getLineasSinCosto());
    }
}
