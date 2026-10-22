package com.techprotech.agenda.modulos.dominios.aplicacion;
import com.techprotech.agenda.modulos.dominios.infraestructura.repositorio.EmpresaDominioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.time.*;
@Service
public class PlanificadorChequeosDominio {
 private final EmpresaDominioRepositorio repo;private final Clock reloj;private final long base,maximo;
 public PlanificadorChequeosDominio(EmpresaDominioRepositorio r,Clock c,@Value("${aplicacion.dominios.reintento-base-segundos:300}") long b,@Value("${aplicacion.dominios.reintento-maximo-segundos:86400}") long m){repo=r;reloj=c;base=Math.max(1,b);maximo=Math.max(base,m);}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void aplazar(Long id){repo.findById(id).ifPresent(d->{long espera=Math.min(maximo,base*(1L<<Math.min(16,Math.max(0,d.getIntentosChequeo()))));repo.planificar(id,LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC).plusSeconds(espera));});}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void porFrecuencia(Long id){repo.descontarIntento(id);}
}
