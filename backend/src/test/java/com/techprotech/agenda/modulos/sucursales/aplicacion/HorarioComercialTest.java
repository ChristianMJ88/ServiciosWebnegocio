package com.techprotech.agenda.modulos.sucursales.aplicacion;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
class HorarioComercialTest {
 @Test void normalizaEspaciosSinInventarUbicaciones(){
  assertEquals("San Andrés",NormalizadorUbicacion.texto("  San   Andre\u0301s  "));
  assertNull(NormalizadorUbicacion.texto("  "));assertNull(NormalizadorUbicacion.texto(null));
 }
 @Test void validaDiaFormatoYOrdenDelIntervalo(){
  try(var factory=Validation.buildDefaultValidatorFactory()){
   var validator=factory.getValidator();
   assertTrue(validator.validate(new HorarioComercial(1,"09:00","18:00")).isEmpty());
   assertFalse(validator.validate(new HorarioComercial(8,"09:00","18:00")).isEmpty());
   assertFalse(validator.validate(new HorarioComercial(1,"25:00","26:00")).isEmpty());
   assertFalse(validator.validate(new HorarioComercial(1,"18:00","09:00")).isEmpty());
  }
 }
 @Test void jsonPersisteSoloDatosSinPropiedadesDeValidacion()throws Exception{
  var mapper=new ObjectMapper();var horario=new HorarioComercial(2,"10:00","17:00");
  var json=mapper.writeValueAsString(horario);
  assertFalse(json.contains("intervaloValido"));assertEquals(horario,mapper.readValue(json,HorarioComercial.class));
 }
}
