package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class CalculadorAnticipoServicio {
    private CalculadorAnticipoServicio() {}
    public static BigDecimal calcular(BigDecimal neto, ServicioEntidad servicio, String modalidad,
                                       BigDecimal porcentaje, BigDecimal importe) {
        if (servicio != null && servicio.isRequiereAnticipo()) {
            modalidad = "PORCENTAJE".equals(servicio.getAnticipoTipo()) ? "ANTICIPO_PORCENTAJE" : "ANTICIPO_IMPORTE";
            porcentaje = servicio.getAnticipoValor(); importe = servicio.getAnticipoValor();
        }
        BigDecimal resultado = switch (modalidad == null ? "TOTAL" : modalidad) {
            case "ANTICIPO_PORCENTAJE" -> neto.multiply(requerido(porcentaje)).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
            case "ANTICIPO_IMPORTE" -> requerido(importe);
            default -> neto;
        };
        return resultado.min(neto).max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);
    }
    private static BigDecimal requerido(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT,"El anticipo del servicio no está configurado correctamente");
        return valor;
    }
}
