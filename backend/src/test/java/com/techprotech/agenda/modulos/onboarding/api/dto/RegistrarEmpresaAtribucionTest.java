package com.techprotech.agenda.modulos.onboarding.api.dto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RegistrarEmpresaAtribucionTest {
 private final ObjectMapper mapper=new ObjectMapper();
 private final String datos="\"nombreEmpresa\":\"Negocio prueba\",\"giro\":\"Servicios\",\"tamanoEquipo\":\"1\",\"nombreAdministrador\":\"Administrador prueba\",\"correoAdministrador\":\"prueba@example.test\",\"telefonoAdministrador\":\"5512345678\"";
 @Test void contratoAnteriorSigueValidoSinAtribucion()throws Exception{
  var request=mapper.readValue("{"+datos+"}",RegistrarEmpresaRequest.class);
  try(var factory=Validation.buildDefaultValidatorFactory()){
   assertNull(request.atribucion());assertTrue(factory.getValidator().validate(request).isEmpty());
  }
 }
 @Test void validacionBackendRechazaAtribucionConDatosAjenoAlContrato()throws Exception{
  var request=mapper.readValue("{"+datos+",\"atribucion\":{\"source\":\"persona@example.test\"}}",RegistrarEmpresaRequest.class);
  try(var factory=Validation.buildDefaultValidatorFactory()){
   assertTrue(factory.getValidator().validate(request).stream().anyMatch(v->v.getPropertyPath().toString().equals("atribucion.source")));
  }
 }
}
