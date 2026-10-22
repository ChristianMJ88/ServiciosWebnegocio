package com.techprotech.agenda.modulos.dominios.aplicacion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.dominios.api.dto.*;
import com.techprotech.agenda.modulos.dominios.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.dominios.infraestructura.repositorio.EmpresaDominioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static com.techprotech.agenda.modulos.dominios.infraestructura.entidad.EstadoDominio.*;

@Service
public class ServicioDominios {
 private final EmpresaDominioRepositorio repo;private final EmpresaRepositorio empresas;private final EmpresaSitioConfigRepositorio sitios;
 private final ServicioCapacidadesEmpresa capacidades;private final ServicioConsultaDominios consulta;private final ServicioAuditoriaDominios auditoria;
 private final ValidadorHostname hosts;private final ResolverTxtDominio dns;private final ComprobadorHostingDominio hosting;private final ObjectMapper json;private final Clock reloj;private final TransactionTemplate tx;
 private final long frecuenciaSegundos,avisoDias;private final String sitioHosting;
 public ServicioDominios(EmpresaDominioRepositorio r,EmpresaRepositorio e,EmpresaSitioConfigRepositorio s,ServicioCapacidadesEmpresa c,ServicioConsultaDominios q,ServicioAuditoriaDominios a,ValidadorHostname h,ResolverTxtDominio d,ComprobadorHostingDominio p,ObjectMapper j,Clock clock,PlatformTransactionManager tm,
  @Value("${aplicacion.dominios.frecuencia-segundos:60}") long frecuencia,@Value("${aplicacion.dominios.aviso-pendiente-dias:7}") long dias,@Value("${aplicacion.dominios.sitio-hosting:}") String sitio){sitioHosting=sitio;repo=r;empresas=e;sitios=s;capacidades=c;consulta=q;auditoria=a;hosts=h;dns=d;hosting=p;json=j;reloj=clock;tx=new TransactionTemplate(tm);frecuenciaSegundos=Math.max(1,frecuencia);avisoDias=Math.max(1,dias);}
 private LocalDateTime ahora(){return LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC);}
 public PanelDominiosResponse panel(Long empresa){
  return tx.execute(status->{var rows=repo.findByEmpresaIdOrderByIdDesc(empresa);boolean incluido=consulta.incluido(empresa);long uso=rows.stream().filter(d->d.getEstado()!=RETIRADO).count();Long limite=limite(empresa);
   return new PanelDominiosResponse(incluido,incluido&&(limite==null||uso<limite),limite,uso,rows.stream().map(d->vista(d,true,incluido)).toList());});
 }
 public DominioResponse solicitar(Long empresa,Long actor,String hostname){
  String host=hosts.normalizar(hostname);
  try{return tx.execute(status->{bloquearEmpresa(empresa);capacidades.exigirModulo(empresa,"DOMINIO_PROPIO");capacidades.exigirLimite(empresa,"DOMINIOS_PROPIOS_MAX",repo.countByEmpresaIdAndEstadoNot(empresa,RETIRADO));
   var presentes=repo.findByEmpresaIdOrderByIdDesc(empresa);
   if(presentes.stream().anyMatch(d->d.getHostname().equals(host)&&d.getEstado()!=RETIRADO)||reservado(host))throw conflicto("El dominio ya tiene una solicitud vigente o está en uso");
   var d=new EmpresaDominioEntidad();d.setEmpresaId(empresa);d.setHostname(host);d.setEstado(PENDIENTE_VERIFICACION);byte[] token=new byte[32];new SecureRandom().nextBytes(token);d.setTokenVerificacion(Base64.getUrlEncoder().withoutPadding().encodeToString(token));d.setCreadoEn(ahora());d.setCreadoPor(actor);repo.saveAndFlush(d);auditoria.registrar(d,actor,false,"DOMINIO_SOLICITADO");return vista(d,true,true);
  });}catch(DataIntegrityViolationException e){throw conflicto("El dominio ya tiene una solicitud vigente o está en uso");}
 }
 private boolean reservado(String host){return Arrays.stream(new EstadoDominio[]{VERIFICADO,CONECTANDO,ACTIVO,ERROR}).anyMatch(s->repo.findByHostnameAndEstado(host,s).isPresent());}
 public DominioResponse verificar(Long empresa,Long actor,Long id,Long version){return comprobar(empresa,actor,id,version,false);}
 public DominioResponse comprobarPlataforma(Long actor,Long id,Long version){var d=repo.findById(id).orElseThrow(ServicioDominios::noExiste);return comprobar(d.getEmpresaId(),actor,id,version,true);}
 private record Chequeo(Long id,Long empresa,String host,String token,Long version,EstadoDominio estado,String build){}
 private DominioResponse comprobar(Long empresa,Long actor,Long id,Long version,boolean operador){
  Chequeo c=tx.execute(status->{bloquearEmpresa(empresa);capacidades.exigirModulo(empresa,"DOMINIO_PROPIO");var d=propio(empresa,id);version(d,version);
   if(!List.of(PENDIENTE_VERIFICACION,CONECTANDO,ERROR).contains(d.getEstado()))throw conflicto("El dominio no admite esta comprobación");
   var ultimo=repo.ultimoChequeoEmpresa(empresa);if(ultimo!=null&&ultimo.plusSeconds(frecuenciaSegundos).isAfter(ahora()))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Espera antes de volver a comprobar los dominios del negocio");
   d.setUltimoChequeoEn(ahora());d.setChequeoEnCurso(true);repo.saveAndFlush(d);return new Chequeo(id,empresa,d.getHostname(),d.getTokenVerificacion(),d.getVersion(),d.getEstado(),sitioHosting);
  });
  // Network operations deliberately run after the first transaction releases all locks.
  boolean valido=false;String error=null;
  try{if(c.estado()==PENDIENTE_VERIFICACION){byte[] esperado=("fluora-verificacion="+c.token()).getBytes(StandardCharsets.UTF_8);boolean coincide=false;for(String valor:dns.consultar("_fluora."+c.host()))coincide|=MessageDigest.isEqual(esperado,valor.getBytes(StandardCharsets.UTF_8));valido=coincide;}
   else valido=hosting.comprobar(c.host(),c.build());
  }catch(Exception ex){error="COMPROBACION_NO_DISPONIBLE";}
  final boolean ok=valido;final String fallo=error;
  try{return tx.execute(status->{var filas=repo.bloquearHostname(c.host());var d=filas.stream().filter(f->f.getId().equals(id)&&f.getEmpresaId().equals(empresa)).findFirst().orElseThrow(ServicioDominios::noExiste);version(d,c.version());
   EstadoDominio estadoAntes=d.getEstado();String errorAntes=d.getUltimoError();
   if(d.getEstado()!=c.estado()||!MessageDigest.isEqual(d.getTokenVerificacion().getBytes(StandardCharsets.UTF_8),c.token().getBytes(StandardCharsets.UTF_8)))throw conflicto("La solicitud cambió durante la comprobación");
   capacidades.exigirModulo(empresa,"DOMINIO_PROPIO");d.setChequeoEnCurso(false);
   boolean otroReservado=filas.stream().anyMatch(f->!f.getId().equals(id)&&List.of(VERIFICADO,CONECTANDO,ACTIVO,ERROR).contains(f.getEstado()));
   if(c.estado()==PENDIENTE_VERIFICACION&&otroReservado){d.setUltimoError("HOST_EN_USO");}
   else if(!ok){d.setUltimoError(fallo!=null?fallo:c.estado()==PENDIENTE_VERIFICACION?"TXT_NO_ENCONTRADO":"HOSTING_NO_DISPONIBLE");if(c.estado()!=PENDIENTE_VERIFICACION)d.setEstado(ERROR);}
   else if(c.estado()==PENDIENTE_VERIFICACION){
    {for(var otra:filas)if(!otra.getId().equals(id)&&otra.getEstado()==PENDIENTE_VERIFICACION){if(otra.isChequeoEnCurso()&&otra.getUltimoChequeoEn()!=null&&otra.getUltimoChequeoEn().plusSeconds(Math.max(15,frecuenciaSegundos)).isAfter(ahora()))continue;otra.setEstado(RETIRADO);otra.setUltimoError("LIBERADO_POR_OTRA_CUENTA");auditoria.registrar(otra,null,false,"DOMINIO_LIBERADO");}
     d.setIntentosChequeo(0);d.setProximoChequeoEn(null);d.setEstado(VERIFICADO);d.setVerificadoEn(ahora());d.setUltimoError(null);}
   }else{d.setEstado(ACTIVO);d.setActivadoEn(ahora());d.setUltimoError(null);actualizarDerivado(empresa,d.getHostname());}
   repo.saveAndFlush(d);if(estadoAntes!=d.getEstado()||!Objects.equals(errorAntes,d.getUltimoError()))auditoria.registrar(d,actor,operador,"DOMINIO_COMPROBADO");return vista(d,!operador,consulta.incluido(empresa));
  });}catch(DataIntegrityViolationException e){throw conflicto("Otro proceso comprobó la propiedad de este dominio");}
 }
 public DominioResponse conectar(Long actor,Long id,ConexionDominioSolicitud s){return tx.execute(status->{var d=repo.findById(id).orElseThrow(ServicioDominios::noExiste);version(d,s.version());capacidades.exigirModulo(d.getEmpresaId(),"DOMINIO_PROPIO");
  if(!List.of(VERIFICADO,CONECTANDO,ERROR).contains(d.getEstado()))throw conflicto("Primero debe verificarse la propiedad del dominio");
  if(sitioHosting.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"La plataforma todavía no ha preparado el sitio de clientes");
  d.setRegistrosHosting(serializar(s.registros()));d.setIntentosChequeo(0);d.setProximoChequeoEn(null);d.setEstado(CONECTANDO);d.setUltimoError(null);repo.saveAndFlush(d);auditoria.registrar(d,actor,true,"DOMINIO_CONECTANDO");return vista(d,false,true);});}
 public DominioResponse retirar(Long empresa,Long actor,Long id,Long version,boolean operador){return tx.execute(status->{bloquearEmpresa(empresa);var d=propio(empresa,id);version(d,version);if(d.getEstado()==RETIRADO)throw conflicto("El dominio ya está retirado");d.setEstado(RETIRADO);repo.saveAndFlush(d);
  var restantes=repo.findByEmpresaIdOrderByIdDesc(empresa).stream().filter(f->f.getEstado()==ACTIVO).toList();actualizarDerivado(empresa,restantes.isEmpty()?null:restantes.getFirst().getHostname());auditoria.registrar(d,actor,operador,"DOMINIO_RETIRADO");return vista(d,!operador,consulta.incluido(empresa));});}
 public DominioResponse retirarPlataforma(Long actor,Long id,Long version){var d=repo.findById(id).orElseThrow(ServicioDominios::noExiste);return retirar(d.getEmpresaId(),actor,id,version,true);}
 public Page<DominioPlataformaResponse> cola(EstadoDominio estado,int pagina,int tamano){var pageable=PageRequest.of(Math.max(0,pagina),Math.min(100,Math.max(1,tamano)),Sort.by("id").descending());var filas=estado==null?repo.findAll(pageable):repo.findByEstado(estado,pageable);return filas.map(d->{Long limite=limite(d.getEmpresaId());return new DominioPlataformaResponse(d.getEmpresaId(),vista(d,false,consulta.incluido(d.getEmpresaId())),limite!=null&&repo.countByEmpresaIdAndEstadoNot(d.getEmpresaId(),RETIRADO)>limite,d.getEstado()==RETIRADO&&d.getRegistrosHosting()!=null);});}
 private DominioResponse vista(EmpresaDominioEntidad d,boolean propio,boolean incluido){
  List<RegistroDns> registros=List.of();if(propio&&d.getEstado()==PENDIENTE_VERIFICACION)registros=List.of(new RegistroDns("TXT","_fluora."+d.getHostname(),"fluora-verificacion="+d.getTokenVerificacion()));
  else if(d.getRegistrosHosting()!=null)try{registros=json.readValue(d.getRegistrosHosting(),new TypeReference<List<RegistroDns>>(){});}catch(Exception ex){throw new IllegalStateException("Registros DNS inválidos");}
  var acciones=new ArrayList<String>();if(d.getEstado()!=RETIRADO)acciones.add("RETIRAR");if(incluido&&List.of(PENDIENTE_VERIFICACION,CONECTANDO,ERROR).contains(d.getEstado()))acciones.add("VERIFICAR");if(!propio&&incluido&&!sitioHosting.isBlank()&&List.of(VERIFICADO,CONECTANDO,ERROR).contains(d.getEstado()))acciones.add("CONECTAR");
  String aviso=!incluido?"Tu plan actual no incluye dominio propio":switch(d.getEstado()){
   case PENDIENTE_VERIFICACION->d.getCreadoEn().plusDays(avisoDias).isBefore(ahora())?"La verificación sigue pendiente. Revisa el TXT con tu proveedor de DNS.":"Agrega el TXT. La propagación puede tardar hasta 48 h.";
   case VERIFICADO->"En preparación por Fluora";case CONECTANDO,ERROR->"Agrega los registros indicados y vuelve a comprobar la conexión.";case ACTIVO->"Tu dominio está activo";case RETIRADO->"El dominio ya no sirve el sitio. Fluora revisará su retiro de Hosting.";};
  return new DominioResponse(d.getId(),d.getHostname(),d.getEstado().name(),d.getVersion(),hosts.esRaiz(d.getHostname()),registros,aviso,d.getUltimoError(),d.getVerificadoEn(),d.getActivadoEn(),d.getUltimoChequeoEn(),List.copyOf(acciones));
 }
 private Long limite(Long empresa){return capacidades.obtener(empresa).limites().stream().filter(l->l.codigo().equals("DOMINIOS_PROPIOS_MAX")).map(l->l.valor()).findFirst().orElse(null);}
 private void bloquearEmpresa(Long id){empresas.bloquearPorId(id).orElseThrow(ServicioDominios::noExiste);}
 private EmpresaDominioEntidad propio(Long empresa,Long id){return repo.findByIdAndEmpresaId(id,empresa).orElseThrow(ServicioDominios::noExiste);}
 private void actualizarDerivado(Long empresa,String host){sitios.findById(empresa).ifPresent(s->{s.setDominioPrincipal(host);sitios.save(s);});}
 private String serializar(Object o){try{return json.writeValueAsString(o);}catch(Exception ex){throw new IllegalArgumentException("Registros inválidos");}}
 private static void version(EmpresaDominioEntidad d,Long v){if(v==null||!Objects.equals(d.getVersion(),v))throw conflicto("La solicitud cambió. Actualiza la vista e inténtalo de nuevo");}
 private static ResponseStatusException conflicto(String s){return new ResponseStatusException(HttpStatus.CONFLICT,s);}
 private static ResponseStatusException noExiste(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"No existe la solicitud indicada");}
}
