package com.techprotech.agenda.modulos.pagos.aplicacion;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class PointConcurrenciaRespuestaTest {
 @Test void cajaTraduceInterbloqueoA409SinReintentar() {
  var advice=new com.techprotech.agenda.modulos.caja.api.ManejadorConcurrenciaCaja(java.time.Clock.systemUTC());
  var respuesta=advice.conflicto(new org.springframework.dao.CannotAcquireLockException("deadlock"));
  assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
  assertThat(respuesta.getBody().mensaje()).isEqualTo("La operación coincidió con otro cobro. Intenta de nuevo.");
 }
}
