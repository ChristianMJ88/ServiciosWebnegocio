package com.techprotech.agenda.modulos.citas.aplicacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import com.techprotech.agenda.modulos.sitio.dominio.TipoDescuentoPromocion;

class CalculadorPromocionServiciosTest {
    @ParameterizedTest @CsvSource({"250,PORCENTAJE,20,200.00,50.00", "250,PRECIO,180,180.00,70.00",
        "250,PRECIO,300,250.00,0.00", "250,PORCENTAJE,100,0.00,250.00", "100.05,PORCENTAJE,10,90.05,10.00"})
    void calculaNetoYDescuentoSinSalirDelPrecioLista(String lista,String tipo,String valor,String neto,String descuento) {
        var resultado = CalculadorPromocionServicios.calcular(new BigDecimal(lista),TipoDescuentoPromocion.valueOf(tipo),new BigDecimal(valor));
        assertThat(resultado.precioNeto()).isEqualByComparingTo(neto); assertThat(resultado.descuento()).isEqualByComparingTo(descuento);
        assertThat(resultado.precioNeto().scale()).isEqualTo(2);
    }
    @Test void anticipoSeCalculaSobreNetoYDescuentoSoloSeReportaUnaVez() {
        var servicio = new com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad();
        servicio.setRequiereAnticipo(true); servicio.setAnticipoTipo("PORCENTAJE"); servicio.setAnticipoValor(new BigDecimal("50"));
        assertThat(CalculadorAnticipoServicio.calcular(new BigDecimal("200"),servicio,"PAGO_COMPLETO",null,null)).isEqualByComparingTo("100.00");
        var cita = new com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad();
        cita.setPrecio(new BigDecimal("200")); cita.setPrecioLista(new BigDecimal("250")); cita.setDescuento(new BigDecimal("50"));
        var primero = DesglosePrecioCita.paraCobro(cita,new BigDecimal("100"),BigDecimal.ZERO);
        var saldo = DesglosePrecioCita.paraCobro(cita,new BigDecimal("100"),new BigDecimal("100"));
        assertThat(primero.precioLista().add(saldo.precioLista())).isEqualByComparingTo("250");
        assertThat(primero.descuento()).isEqualByComparingTo("50"); assertThat(saldo.descuento()).isZero();
    }
}
