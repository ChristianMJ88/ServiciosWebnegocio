package com.techprotech.agenda.modulos.citas.aplicacion;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import java.math.BigDecimal;

/** La política del servicio prevalece; la modalidad global solo ayuda a configurar servicios nuevos. */
public record PoliticaCobroServicio(String politica,BigDecimal importeRequerido,BigDecimal saldoEnNegocio) {
    public static PoliticaCobroServicio resolver(BigDecimal neto,ServicioEntidad servicio) {
        if(servicio==null || !servicio.isRequiereAnticipo()) return new PoliticaCobroServicio("SIN_COBRO",BigDecimal.ZERO.setScale(2),neto);
        String politica="TOTAL".equals(servicio.getAnticipoTipo())?"TOTAL":
                "PORCENTAJE".equals(servicio.getAnticipoTipo())?"ANTICIPO_PORCENTAJE":"ANTICIPO_FIJO";
        BigDecimal requerido="TOTAL".equals(politica)?neto:CalculadorAnticipoServicio.calcular(neto,servicio,null,null,null);
        return new PoliticaCobroServicio(politica,requerido,neto.subtract(requerido));
    }
}
