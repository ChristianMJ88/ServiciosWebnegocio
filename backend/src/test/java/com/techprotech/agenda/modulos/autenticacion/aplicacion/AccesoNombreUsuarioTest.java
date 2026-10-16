package com.techprotech.agenda.modulos.autenticacion.aplicacion;
import com.techprotech.agenda.modulos.autenticacion.api.dto.IniciarSesionAppRequest;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.seguridad.jwt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class AccesoNombreUsuarioTest {
 @Mock ServicioTokenJwt tokens; PropiedadesJwt propiedades = new PropiedadesJwt("secreto-de-prueba", "prueba", 15, 7); @Mock EmpresaRepositorio empresas; @Mock UsuarioRepositorio usuarios;
 @Mock ClienteRepositorio clientes; @Mock ServicioRolesEmpresa roles; @Mock TokenActualizacionRepositorio refresh;
 @Mock PasswordEncoder encoder; @Mock ServicioProteccionAcceso proteccion;
 ServicioAutenticacionImpl servicio;
 @BeforeEach void configurar(){ servicio = new ServicioAutenticacionImpl(tokens, propiedades, empresas, usuarios, clientes, roles, refresh, encoder, proteccion, java.time.Clock.systemUTC()); }
 private UsuarioEntidad preparar(){var e=new EmpresaEntidad();e.setId(10L);when(empresas.findBySlug("negocio-prueba")).thenReturn(Optional.of(e));var u=new UsuarioEntidad();u.setId(20L);u.setEmpresaId(10L);u.setNombreUsuario("operador");u.setContrasenaHash("hash");u.setHabilitado(true);u.setRequiereCambioContrasena(true);when(usuarios.findByEmpresaIdAndNombreUsuario(10L,"operador")).thenReturn(Optional.of(u));return u;}
 @Test void temporalNoEmiteSesion(){preparar();when(encoder.matches("temporal","hash")).thenReturn(true);var r=servicio.iniciarSesionApp(new IniciarSesionAppRequest("negocio-prueba/operador","temporal",null));assertEquals("CAMBIO_CONTRASENA",r.estado());assertNull(r.sesion());verifyNoInteractions(tokens,refresh);}
 @Test void noPermiteSeleccionarOtroTenant(){preparar();when(encoder.matches("temporal","hash")).thenReturn(true);assertThrows(ResponseStatusException.class,()->servicio.iniciarSesionApp(new IniciarSesionAppRequest("negocio-prueba/operador","temporal",99L)));verifyNoInteractions(tokens,refresh);}
 @Test void rechazaContrasenaIncorrecta(){preparar();assertThrows(ResponseStatusException.class,()->servicio.iniciarSesionApp(new IniciarSesionAppRequest("negocio-prueba/operador","incorrecta",null)));verify(proteccion).registrarIntentoFallido(20L);verifyNoInteractions(tokens,refresh);}
 @Test void rechazaReutilizarTemporal(){preparar();when(encoder.matches("temporal","hash")).thenReturn(true);assertThrows(ResponseStatusException.class,()->servicio.iniciarSesionApp(new IniciarSesionAppRequest("negocio-prueba/operador","temporal",null,"temporal")));verifyNoInteractions(tokens,refresh);}
 @Test void cambioPersonalPermiteSesion(){
  var usuario=preparar();
  var empresa=new EmpresaEntidad();empresa.setId(10L);empresa.setSlug("negocio-prueba");
  when(empresas.findById(10L)).thenReturn(Optional.of(empresa));
  when(encoder.matches("temporal","hash")).thenReturn(true);
  when(encoder.encode("personal-segura")).thenReturn("hash-personal");
  when(tokens.generarTokenActualizacion("operador",20L,10L,0L)).thenReturn("refresh-prueba");
  var respuesta=servicio.iniciarSesionApp(new IniciarSesionAppRequest("negocio-prueba/operador","temporal",null,"personal-segura"));
  assertEquals("AUTENTICADO",respuesta.estado());assertNotNull(respuesta.sesion());
  assertFalse(usuario.isRequiereCambioContrasena());assertEquals("hash-personal",usuario.getContrasenaHash());
  verify(refresh).save(any());
 }
}
