package com.techprotech.agenda.compartido.exception;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.MissingServletRequestParameterException;
import static org.junit.jupiter.api.Assertions.*;
class ManejadorParametrosTest {
 @Test void parametroObligatorioResponde400SinErrorInterno(){
  var r=new ManejadorGlobalExcepciones(java.time.Clock.systemUTC()).manejarParametroFaltante(new MissingServletRequestParameterException("desde","LocalDate"));
  assertEquals(400,r.getStatusCode().value());
  assertEquals("VALIDACION_INVALIDA",r.getBody().codigo());
  assertTrue(r.getBody().mensaje().contains("desde"));
 }
}
