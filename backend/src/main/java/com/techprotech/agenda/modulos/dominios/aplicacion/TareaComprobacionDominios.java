package com.techprotech.agenda.modulos.dominios.aplicacion;
import com.techprotech.agenda.modulos.dominios.infraestructura.repositorio.EmpresaDominioRepositorio;
import com.techprotech.agenda.modulos.dominios.infraestructura.entidad.EstadoDominio;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
@ConditionalOnProperty(name="aplicacion.dominios.verificacion-automatica",havingValue="true")
public class TareaComprobacionDominios {
 private final EmpresaDominioRepositorio repo;private final ServicioDominios servicio;private final int lote;private final PlanificadorChequeosDominio planificador;private final java.time.Clock reloj;
 public TareaComprobacionDominios(EmpresaDominioRepositorio r,ServicioDominios s,PlanificadorChequeosDominio p,java.time.Clock c,@Value("${aplicacion.dominios.lote:50}") int n){repo=r;servicio=s;planificador=p;reloj=c;lote=Math.max(1,Math.min(500,n));}
 @Scheduled(fixedDelayString="${aplicacion.dominios.intervalo-ms:300000}")
 public void ejecutar(){for(Long id:repo.candidatos(List.of(EstadoDominio.PENDIENTE_VERIFICACION,EstadoDominio.CONECTANDO,EstadoDominio.ERROR),java.time.LocalDateTime.ofInstant(reloj.instant(),java.time.ZoneOffset.UTC),PageRequest.of(0,lote))){
  try{planificador.aplazar(id);var actual=repo.findById(id);if(actual.isPresent()){var d=actual.get();servicio.verificar(d.getEmpresaId(),null,id,d.getVersion());}}
  catch(org.springframework.web.server.ResponseStatusException ex){if(ex.getStatusCode().value()==429)planificador.porFrecuencia(id);}
  catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(TareaComprobacionDominios.class).warn("Chequeo automático no completado: tipo={}",ex.getClass().getSimpleName());}
 }}
}
