package com.techprotech.agenda.modulos.disponibilidad.aplicacion;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.*;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
class ServicioSolicitudesHorarioTest {
 @Test void contrasenaIncorrectaNoAplicaCambio(){
  var repo=mock(SolicitudHorarioRepositorio.class);var reglas=mock(ReglaDisponibilidadRepositorio.class);var gestion=mock(ServicioGestionDisponibilidad.class);var semanal=mock(ServicioHorarioSemanal.class);var usuarios=mock(UsuarioRepositorio.class);var encoder=mock(PasswordEncoder.class);
  var admin=new UsuarioEntidad();admin.setHabilitado(true);admin.setContrasenaHash("hash");when(usuarios.findByIdAndEmpresaId(2L,1L)).thenReturn(Optional.of(admin));
  var servicio=new ServicioSolicitudesHorario(repo,reglas,gestion,semanal,usuarios,encoder,new ObjectMapper());
  assertThrows(ResponseStatusException.class,()->servicio.resolver(1L,2L,3L,new ResolverHorarioRequest(true,"incorrecta")));
  verifyNoInteractions(repo,gestion,semanal);
 }
 @Test void staffNoPuedeModificarHorarioExistentePorEndpointAntiguo(){
  var reglas=mock(ReglaDisponibilidadRepositorio.class);when(reglas.findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByDiaSemanaAscHoraInicioAsc(1L,"PRESTADOR",2L)).thenReturn(List.of(new ReglaDisponibilidadEntidad()));
  var gestion=new ServicioGestionDisponibilidad(reglas,mock(ExcepcionDisponibilidadRepositorio.class),mock(com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio.class),mock(com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio.class));
  assertThrows(ResponseStatusException.class,()->gestion.guardarReglaStaff(1L,2L,3L,null));verify(reglas,never()).save(any());
 }
}
