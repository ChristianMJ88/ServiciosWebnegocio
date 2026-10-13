package com.techprotech.agenda.modulos.citas.aplicacion;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record DesglosePrecioCita(BigDecimal precioLista, BigDecimal descuento) {
    public static DesglosePrecioCita paraCobro(CitaEntidad cita, BigDecimal cobrar, BigDecimal pagado) {
        // Recognize the whole agreed discount on the first payment (including a deposit).
        // Following payments contain only net balance. Gross payments therefore sum to
        // the original list price, and the discount is never counted twice.
        BigDecimal descuento = pagado.signum()==0 && cita.getDescuento()!=null ? cita.getDescuento() : BigDecimal.ZERO;
        descuento = descuento.max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);
        return new DesglosePrecioCita(cobrar.add(descuento).setScale(2,RoundingMode.HALF_UP),descuento);
    }
}
