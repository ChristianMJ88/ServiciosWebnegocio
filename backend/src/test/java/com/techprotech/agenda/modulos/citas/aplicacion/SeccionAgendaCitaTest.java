package com.techprotech.agenda.modulos.citas.aplicacion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDateTime;import java.math.BigDecimal;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.caja.aplicacion.*;
import static org.assertj.core.api.Assertions.*;
class SeccionAgendaCitaTest {
 @ParameterizedTest @CsvSource({"CONFIRMADA,true,0,FINALIZADA","CONFIRMADA,true,10,ACTIVA","NO_ASISTIO,false,10,FINALIZADA","FINALIZADA,false,10,FINALIZADA","CANCELADA,true,0,CANCELADA","PENDIENTE,false,0,ACTIVA"})
 void clasificaSinRequerirPermisoDeCobro(String estado,boolean llegada,String pendiente,String esperada) {
  var cita=new CitaEntidad();cita.setEstado(estado);if(llegada)cita.setCheckInEn(LocalDateTime.parse("2026-10-17T12:00"));
  var saldo=new SaldoCita(BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,new BigDecimal(pendiente),EstadoCobroCita.PENDIENTE);
  assertThat(SeccionAgendaCita.de(cita,saldo).name()).isEqualTo(esperada);
 }
}
