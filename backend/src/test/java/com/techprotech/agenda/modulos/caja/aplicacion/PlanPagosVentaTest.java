package com.techprotech.agenda.modulos.caja.aplicacion;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarVentaRequest;
import com.techprotech.agenda.modulos.pagos.aplicacion.PagosPrevistosPoint;
import java.util.*;import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class PlanPagosVentaTest {
 static RegistrarVentaRequest solicitud(RegistrarVentaRequest.PagoVentaRequest... partes){return new RegistrarVentaRequest(3L,null,null,List.of(),null,null,null,null,null,List.of(partes));}
 static RegistrarVentaRequest.PagoVentaRequest parte(String metodo,String monto,String recibido){return new RegistrarVentaRequest.PagoVentaRequest(metodo,monto==null?null:new BigDecimal(monto),recibido==null?null:new BigDecimal(recibido),null);}
 @Test void repartoEfectivoTarjetaYCambioUsanTotalDefinitivo(){
  var plan=PlanPagosVenta.resolver(new BigDecimal("700"),solicitud(parte("EFECTIVO","300","500"),parte("TARJETA","400",null)),false);
  assertThat(plan.partes()).hasSize(2);assertThat(plan.partes().getFirst().cambio()).isEqualByComparingTo("200");assertThat(plan.importePoint()).isZero();
 }
 @Test void pointCalculaRestoYDocumentoEsVersionadoConDecimalesComoTexto(){
  var plan=PlanPagosVenta.resolver(new BigDecimal("700"),solicitud(parte("EFECTIVO","300","500"),parte("MERCADO_PAGO_POINT",null,null)),true);
  assertThat(plan.importePoint()).isEqualByComparingTo("400");
  var codec=new PagosPrevistosPoint(new com.fasterxml.jackson.databind.ObjectMapper());var json=codec.escribir(plan);
  assertThat(json).contains("\"v\":1","\"monto\":\"300.00\"","\"montoRecibido\":\"500.00\"");
  assertThat(codec.leer(json)).isEqualTo(plan.previstas());assertThat(codec.leer(null)).isEmpty();
  assertThatThrownBy(()->codec.leer("{\"v\":2,\"partes\":[]}")).isInstanceOf(IllegalStateException.class);
 }
 @Test void rechazaRepartosInvalidosAntesDeEscribir(){
  for(var invalido:List.of(
    solicitud(parte("MERCADO_PAGO_POINT",null,null),parte("MERCADO_PAGO_POINT",null,null)),
    solicitud(parte("EFECTIVO","700",null),parte("MERCADO_PAGO_POINT",null,null)),
    solicitud(parte("EFECTIVO","701",null),parte("MERCADO_PAGO_POINT",null,null)),
    solicitud(parte("EFECTIVO","-1",null),parte("MERCADO_PAGO_POINT",null,null)),
    solicitud(parte("EFECTIVO","300","200"),parte("MERCADO_PAGO_POINT",null,null)),
    solicitud(parte("TARJETA","300","500"),parte("MERCADO_PAGO_POINT",null,null)),
    solicitud(parte("EFECTIVO","300.001",null),parte("MERCADO_PAGO_POINT",null,null))
  )) assertThatThrownBy(()->PlanPagosVenta.resolver(new BigDecimal("700"),invalido,true)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  var ambos=new RegistrarVentaRequest(3L,null,null,List.of(),"EFECTIVO",null,null,null,null,List.of(parte("EFECTIVO","700",null)));
  assertThatThrownBy(()->PlanPagosVenta.resolver(new BigDecimal("700"),ambos,false)).hasMessageContaining("no ambos");
  assertThatThrownBy(()->PlanPagosVenta.resolver(new BigDecimal("700"),solicitud(parte("EFECTIVO","300",null),parte("TARJETA","300",null)),false)).hasMessageContaining("coincidir");
 }
}
