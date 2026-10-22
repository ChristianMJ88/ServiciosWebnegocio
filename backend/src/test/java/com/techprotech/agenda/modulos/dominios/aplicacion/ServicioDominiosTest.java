package com.techprotech.agenda.modulos.dominios.aplicacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.dominios.api.dto.*;
import com.techprotech.agenda.modulos.dominios.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.dominios.infraestructura.repositorio.EmpresaDominioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import com.techprotech.agenda.modulos.plataforma.api.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;import java.util.*;import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
import static com.techprotech.agenda.modulos.dominios.infraestructura.entidad.EstadoDominio.*;
class ServicioDominiosTest {
 EmpresaDominioRepositorio repo;EmpresaRepositorio empresas;EmpresaSitioConfigRepositorio sitios;ServicioCapacidadesEmpresa capacidades;ServicioConsultaDominios consulta;ServicioAuditoriaDominios audit;ResolverTxtDominio dns;ComprobadorHostingDominio hosting;ServicioDominios servicio;
 List<EmpresaDominioEntidad> rows;Clock clock=Clock.fixed(Instant.parse("2026-10-20T15:00:00Z"),ZoneOffset.UTC);AtomicInteger transactions;long limite=2;
 @BeforeEach void setup(){
  repo=mock(EmpresaDominioRepositorio.class);empresas=mock(EmpresaRepositorio.class);sitios=mock(EmpresaSitioConfigRepositorio.class);capacidades=mock(ServicioCapacidadesEmpresa.class);consulta=mock(ServicioConsultaDominios.class);audit=mock(ServicioAuditoriaDominios.class);dns=mock(ResolverTxtDominio.class);hosting=mock(ComprobadorHostingDominio.class);rows=new ArrayList<>();transactions=new AtomicInteger();
  when(empresas.bloquearPorId(anyLong())).thenReturn(Optional.of(new EmpresaEntidad()));when(consulta.incluido(anyLong())).thenReturn(true);
  when(capacidades.obtener(anyLong())).thenAnswer(i->new CapacidadesEmpresaResponse(true,1L,4L,"FIXTURE_NEUTRO","Fixture", "ACTIVA",null,null,List.of(new CapacidadModuloEmpresaResponse("DOMINIO_PROPIO","Dominio",null)),List.of(new CapacidadLimiteEmpresaResponse("DOMINIOS_PROPIOS_MAX","Dominios","dominios",limite)),List.of()));
  doAnswer(i->{if((long)i.getArgument(2)>=limite)throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,"Límite");return null;}).when(capacidades).exigirLimite(anyLong(),eq("DOMINIOS_PROPIOS_MAX"),anyLong());
  when(repo.saveAndFlush(any())).thenAnswer(i->{EmpresaDominioEntidad d=i.getArgument(0);if(d.getId()==null){d.setId((long)rows.size()+1);d.setVersion(0L);rows.add(d);}else d.setVersion(d.getVersion()+1);return d;});
  when(repo.findByEmpresaIdOrderByIdDesc(anyLong())).thenAnswer(i->rows.stream().filter(d->d.getEmpresaId().equals(i.getArgument(0))).toList());
  when(repo.countByEmpresaIdAndEstadoNot(anyLong(),any())).thenAnswer(i->rows.stream().filter(d->d.getEmpresaId().equals(i.getArgument(0))&&d.getEstado()!=i.getArgument(1)).count());
  when(repo.findByIdAndEmpresaId(anyLong(),anyLong())).thenAnswer(i->rows.stream().filter(d->d.getId().equals(i.getArgument(0))&&d.getEmpresaId().equals(i.getArgument(1))).findFirst());
  when(repo.findById(anyLong())).thenAnswer(i->rows.stream().filter(d->d.getId().equals(i.getArgument(0))).findFirst());
  when(repo.bloquearHostname(anyString())).thenAnswer(i->rows.stream().filter(d->d.getHostname().equals(i.getArgument(0))).toList());
  when(repo.findByHostnameAndEstado(anyString(),any())).thenAnswer(i->rows.stream().filter(d->d.getHostname().equals(i.getArgument(0))&&d.getEstado()==i.getArgument(1)).findFirst());
  when(repo.ultimoChequeoEmpresa(anyLong())).thenAnswer(i->rows.stream().filter(d->d.getEmpresaId().equals(i.getArgument(0))).map(EmpresaDominioEntidad::getUltimoChequeoEn).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));
  var tm=mock(PlatformTransactionManager.class);when(tm.getTransaction(any())).thenAnswer(i->{transactions.incrementAndGet();return new SimpleTransactionStatus();});doAnswer(i->{transactions.decrementAndGet();return null;}).when(tm).commit(any());doAnswer(i->{transactions.decrementAndGet();return null;}).when(tm).rollback(any());
  servicio=new ServicioDominios(repo,empresas,sitios,capacidades,consulta,audit,new ValidadorHostname("https://platform.example","https://console.example"),dns,hosting,new ObjectMapper().findAndRegisterModules(),clock,tm,60,7,"fixture-clientes");
 }
 @Test void solicitudTokenSeguroYLimiteDelBackend(){var d=servicio.solicitar(1L,10L,"marca.mx");assertThat(d.registros()).hasSize(1);assertThat(rows.getFirst().getTokenVerificacion()).matches("[A-Za-z0-9_-]{43}");servicio.solicitar(1L,10L,"reservas.marca.mx");assertThat(rows.get(1).getTokenVerificacion()).isNotEqualTo(rows.getFirst().getTokenVerificacion());assertStatus(402,()->servicio.solicitar(1L,10L,"www.marca.mx"));verify(capacidades,times(3)).exigirModulo(1L,"DOMINIO_PROPIO");}
 @Test void sinModuloNoCrea(){doThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED)).when(capacidades).exigirModulo(1L,"DOMINIO_PROPIO");assertStatus(402,()->servicio.solicitar(1L,10L,"marca.mx"));assertThat(rows).isEmpty();}
 @Test void aislamientoYVersionAntesDelDns(){var d=servicio.solicitar(1L,10L,"marca.mx");assertStatus(404,()->servicio.verificar(2L,20L,d.id(),d.version()));assertStatus(409,()->servicio.verificar(1L,10L,d.id(),99L));verifyNoInteractions(dns);assertThat(servicio.panel(2L).dominios()).isEmpty();}
 @Test void dnsFueraDeTransaccionYLiberaPendienteAjeno()throws Exception{servicio.solicitar(1L,10L,"marca.mx");var b=servicio.solicitar(2L,20L,"marca.mx");String token=rows.get(1).getTokenVerificacion();when(dns.consultar("_fluora.marca.mx")).thenAnswer(i->{assertThat(transactions).hasValue(0);return List.of("un TXT ajeno","fluora-verificacion="+token);});var result=servicio.verificar(2L,20L,b.id(),b.version());assertThat(result.estado()).isEqualTo("VERIFICADO");assertThat(result.registros()).isEmpty();assertThat(rows.getFirst().getEstado()).isEqualTo(RETIRADO);assertThat(servicio.panel(1L).dominios().getFirst().registros()).isEmpty();verify(audit).registrar(rows.getFirst(),null,false,"DOMINIO_LIBERADO");}
 @Test void txtIncorrectoNoVerificaYAplicaFrecuencia()throws Exception{var d=servicio.solicitar(1L,10L,"marca.mx");when(dns.consultar(anyString())).thenReturn(List.of("incorrecto"));var r=servicio.verificar(1L,10L,d.id(),d.version());assertThat(r.estado()).isEqualTo("PENDIENTE_VERIFICACION");assertThat(r.ultimoError()).isEqualTo("TXT_NO_ENCONTRADO");assertStatus(429,()->servicio.verificar(1L,10L,r.id(),r.version()));}
 @Test void errorDnsNoFiltraElMensajeExterno()throws Exception{var d=servicio.solicitar(1L,10L,"marca.mx");when(dns.consultar(anyString())).thenThrow(new IllegalStateException("token que no debe salir"));var r=servicio.verificar(1L,10L,d.id(),d.version());assertThat(r.ultimoError()).isEqualTo("COMPROBACION_NO_DISPONIBLE");assertThat(r.ultimoError()).doesNotContain("token");}
 @Test void solicitudContraReservadoDaConflicto(){var a=servicio.solicitar(1L,10L,"marca.mx");rows.getFirst().setEstado(ACTIVO);assertStatus(409,()->servicio.solicitar(2L,20L,"marca.mx"));assertThat(rows).hasSize(1);}
 @Test void noTomaDominioVerificadoDuranteCarrera()throws Exception{var a=servicio.solicitar(1L,10L,"marca.mx");var b=servicio.solicitar(2L,20L,"marca.mx");rows.getFirst().setEstado(VERIFICADO);when(dns.consultar(anyString())).thenReturn(List.of("fluora-verificacion="+rows.get(1).getTokenVerificacion()));var r=servicio.verificar(2L,20L,b.id(),b.version());assertThat(r.estado()).isEqualTo("PENDIENTE_VERIFICACION");assertThat(r.ultimoError()).isEqualTo("HOST_EN_USO");assertThat(rows.getFirst().getVersion()).isEqualTo(a.version());}
 @Test void conectarActivarRetirarDerivaSitio()throws Exception{var d=servicio.solicitar(1L,10L,"marca.mx");assertStatus(409,()->servicio.conectar(80L,d.id(),new ConexionDominioSolicitud(d.version(),List.of(new RegistroDns("A","marca.mx","203.0.113.1")))));rows.getFirst().setEstado(VERIFICADO);var conectado=servicio.conectar(80L,d.id(),new ConexionDominioSolicitud(d.version(),List.of(new RegistroDns("A","marca.mx","203.0.113.1"))));assertThat(conectado.estado()).isEqualTo("CONECTANDO");var site=new EmpresaSitioConfigEntidad();when(sitios.findById(1L)).thenReturn(Optional.of(site));when(hosting.comprobar(anyString(),anyString())).thenAnswer(i->{assertThat(transactions).hasValue(0);return true;});var activo=servicio.verificar(1L,10L,d.id(),conectado.version());assertThat(activo.estado()).isEqualTo("ACTIVO");assertThat(site.getDominioPrincipal()).isEqualTo("marca.mx");servicio.retirar(1L,10L,d.id(),activo.version(),false);assertThat(site.getDominioPrincipal()).isNull();assertThat(rows.getFirst().getEstado()).isEqualTo(RETIRADO);}
 @Test void cambioDuranteDnsNoResucitaSolicitud()throws Exception{var d=servicio.solicitar(1L,10L,"marca.mx");when(dns.consultar(anyString())).thenAnswer(i->{var row=rows.getFirst();servicio.retirar(1L,10L,row.getId(),row.getVersion(),false);return List.of("fluora-verificacion="+row.getTokenVerificacion());});assertStatus(409,()->servicio.verificar(1L,10L,d.id(),d.version()));assertThat(rows.getFirst().getEstado()).isEqualTo(RETIRADO);}
 @Test void conectarYVerificarReinicianBackoff()throws Exception{
  var d=servicio.solicitar(1L,10L,"marca.mx");var row=rows.getFirst();row.setIntentosChequeo(10);row.setProximoChequeoEn(LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC).plusDays(1));
  when(dns.consultar(anyString())).thenReturn(List.of("fluora-verificacion="+row.getTokenVerificacion()));
  var verificado=servicio.verificar(1L,10L,d.id(),d.version());assertThat(row.getIntentosChequeo()).isZero();assertThat(row.getProximoChequeoEn()).isNull();
  row.setIntentosChequeo(10);row.setProximoChequeoEn(LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC).plusDays(1));
  servicio.conectar(80L,d.id(),new ConexionDominioSolicitud(verificado.version(),List.of(new RegistroDns("A","marca.mx","203.0.113.1"))));
  assertThat(row.getEstado()).isEqualTo(CONECTANDO);assertThat(row.getIntentosChequeo()).isZero();assertThat(row.getProximoChequeoEn()).isNull();
 }
 private void assertStatus(int status,org.assertj.core.api.ThrowableAssert.ThrowingCallable call){assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(status));}
}
