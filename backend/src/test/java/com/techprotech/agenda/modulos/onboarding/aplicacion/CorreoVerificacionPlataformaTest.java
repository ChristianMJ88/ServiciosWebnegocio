package com.techprotech.agenda.modulos.onboarding.aplicacion;
import com.techprotech.agenda.compartido.correo.*;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioProteccionAcceso;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.onboarding.infraestructura.repositorio.VerificacionCorreoOnboardingRepositorio;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CorreoVerificacionPlataformaTest {
 @Test void verificacionUsaTransportePlataformaYAliasTransaccional(){
  var repo=mock(VerificacionCorreoOnboardingRepositorio.class);
  var correo=mock(ClienteCorreoSuscripciones.class);
  var base=new ConfiguracionCorreoResolvida(null,true,ProveedorCorreo.SMTP,"no-reply@example.test","Plataforma","contacto@example.test","smtp.example.test",465,"user","secret",true,false,null,null,null,null,null,null,null);
  var cfg=base;
  var identidades=mock(ServicioIdentidadCorreoPlataforma.class);
  when(correo.configuracion()).thenReturn(base);
  when(identidades.resolver(base,PropositoCorreoPlataforma.TRANSACCIONAL)).thenReturn(cfg);
  var servicio=new ServicioVerificacionCorreoOnboarding(repo,mock(UsuarioRepositorio.class),mock(EmpresaRepositorio.class),correo,identidades,mock(ServicioOutboxCorreoBienvenida.class),mock(ServicioProteccionAcceso.class),"https://example.test/verificar",30, java.time.Clock.systemUTC());
  var empresa=new EmpresaEntidad();empresa.setId(8L);empresa.setNombre("Negocio de prueba");
  var usuario=new UsuarioEntidad();usuario.setId(45L);usuario.setCorreo("prueba@example.test");
  assertTrue(servicio.enviar(empresa,usuario,"Administrador"));
  verify(correo).enviar(eq(cfg),argThat(m->m.destinatario().equals(usuario.getCorreo())&&m.textoPlano().contains("https://example.test/verificar?token=")));
 }
}
