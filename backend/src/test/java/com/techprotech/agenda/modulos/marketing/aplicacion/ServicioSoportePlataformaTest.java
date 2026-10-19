package com.techprotech.agenda.modulos.marketing.aplicacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.marketing.api.dto.*;
import com.techprotech.agenda.modulos.marketing.infraestructura.entidad.ContactoPlataformaEntidad;
import com.techprotech.agenda.modulos.marketing.infraestructura.repositorio.ContactoPlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioAuditoriaPlataforma;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
public class ServicioSoportePlataformaTest {
 final ContactoPlataformaRepositorio repo=mock(ContactoPlataformaRepositorio.class);
 final ServicioAuditoriaPlataforma auditoria=mock(ServicioAuditoriaPlataforma.class);
 final Clock clock=Clock.fixed(Instant.parse("2026-10-18T18:00:00Z"),ZoneOffset.UTC);
 final ServicioSoportePlataformaImpl s=new ServicioSoportePlataformaImpl(repo,new ObjectMapper(),auditoria,clock);
 public static SoportePlataformaSolicitud solicitud(Long version,String correo,String legal,String telefono,List<Integer> dias,LocalTime inicio,LocalTime fin,String zona){return new SoportePlataformaSolicitud(version,correo,legal,telefono,dias,inicio,fin,zona,true);}
 public static SoportePlataformaSolicitud valida(Long version){return solicitud(version," help@example.test "," LEGAL@example.test ","+1 (202) 555-0189",List.of(5,1,1),LocalTime.of(10,0),LocalTime.of(16,0),"Europe/Madrid");}
 ContactoPlataformaEntidad fila(){var e=new ContactoPlataformaEntidad();e.setId((byte)1);e.setVersion(4L);e.setCorreo("old@example.test");e.setCorreoLegal("old-legal@example.test");e.setTelefono("+12025550189");e.setDias("[2,4]");e.setHoraInicio(LocalTime.of(8,0));e.setHoraFin(LocalTime.of(15,0));e.setZonaHoraria("Europe/Madrid");e.setActualizadoEn(LocalDateTime.of(2026,1,1,0,0));return e;}
 @Test void guardaNormalizaAuditaYConservaTiempoUtc(){
  var e=fila();when(repo.findById((byte)1)).thenReturn(Optional.of(e));when(repo.saveAndFlush(e)).thenAnswer(i->{e.setVersion(5L);return e;});
  var r=s.guardar(valida(4L),12L);assertThat(r.correo()).isEqualTo("help@example.test");assertThat(r.correoLegal()).isEqualTo("legal@example.test");assertThat(r.telefono()).isEqualTo("+12025550189");assertThat(r.dias()).containsExactly(1,5);assertThat(r.actualizadoEn().toInstant()).isEqualTo(clock.instant());
  verify(auditoria).registrar(eq(12L),eq("SOPORTE_ACTUALIZAR"),eq("SOPORTE"),eq("1"),argThat(m->((SoportePlataformaResponse)m.get("anterior")).correoLegal().equals("old-legal@example.test")&&((SoportePlataformaResponse)m.get("nuevo")).correoLegal().equals("legal@example.test")));
 }
 @Test void sinFilaTodosLosGetsSon404(){when(repo.findById((byte)1)).thenReturn(Optional.empty());assertThatThrownBy(s::obtener).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(404));assertThatThrownBy(s::publico).isInstanceOf(ResponseStatusException.class);assertThatThrownBy(s::contactoLegal).isInstanceOf(ResponseStatusException.class);}
 @Test void ocultarSoporteNoOcultaLegal(){when(repo.findById((byte)1)).thenReturn(Optional.of(fila()));assertThatThrownBy(s::publico).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(404));assertThat(s.contactoLegal().correo()).isEqualTo("old-legal@example.test");}
 @Test void versionViejaNuncaGuardaNiAudita(){when(repo.findById((byte)1)).thenReturn(Optional.of(fila()));assertThatThrownBy(()->s.guardar(valida(3L),12L)).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(409));verify(repo,never()).saveAndFlush(any());verifyNoInteractions(auditoria);}
 @Test void carreraOptimistaEs409AntesDeAuditoria(){when(repo.findById((byte)1)).thenReturn(Optional.of(fila()));when(repo.saveAndFlush(any())).thenThrow(new ObjectOptimisticLockingFailureException(ContactoPlataformaEntidad.class,1));assertThatThrownBy(()->s.guardar(valida(4L),12L)).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(409));verifyNoInteractions(auditoria);}
 @Test void carreraCreacionEs409AntesDeAuditoria(){when(repo.findById((byte)1)).thenReturn(Optional.empty());when(repo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));assertThatThrownBy(()->s.guardar(valida(null),12L)).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(409));verifyNoInteractions(auditoria);}
 @ParameterizedTest @ValueSource(strings={"+05:00","UTC+3","no/zona"}) void soloZonasIana(String zona){var v=valida(null);rechaza(solicitud(null,v.correo(),v.correoLegal(),v.telefono(),v.dias(),v.horaInicio(),v.horaFin(),zona));}
 @ParameterizedTest @ValueSource(strings={"bad","a?bcc=x@dominio.mx","a&x=y@dominio.mx","a%3fbcc%3dx@dominio.mx",""}) void mailSinInyeccionEnAmbosCampos(String correo){var v=valida(null);rechaza(solicitud(null,correo,v.correoLegal(),v.telefono(),v.dias(),v.horaInicio(),v.horaFin(),v.zonaHoraria()));rechaza(solicitud(null,v.correo(),correo,v.telefono(),v.dias(),v.horaInicio(),v.horaFin(),v.zonaHoraria()));}
 @ParameterizedTest @ValueSource(strings={"2025550189","+012345678","+1234567","+1234567890123456"}) void exigeTelefonoInternacional(String telefono){var v=valida(null);rechaza(solicitud(null,v.correo(),v.correoLegal(),telefono,v.dias(),v.horaInicio(),v.horaFin(),v.zonaHoraria()));}
 @Test void validaDiasYHorario(){var v=valida(null);for(var dias:List.of(List.<Integer>of(),List.of(0),List.of(8)))rechaza(solicitud(null,v.correo(),v.correoLegal(),v.telefono(),dias,v.horaInicio(),v.horaFin(),v.zonaHoraria()));for(var fin:List.of(LocalTime.of(10,0),LocalTime.of(9,0),LocalTime.of(11,0,1)))rechaza(solicitud(null,v.correo(),v.correoLegal(),v.telefono(),v.dias(),v.horaInicio(),fin,v.zonaHoraria()));}
 void rechaza(SoportePlataformaSolicitud v){assertThatThrownBy(()->s.guardar(v,12L)).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(400));verifyNoInteractions(repo,auditoria);}
}
