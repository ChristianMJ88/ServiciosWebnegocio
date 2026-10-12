package com.techprotech.agenda.modulos.marketing.aplicacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.marketing.api.dto.LandingMarketingDto;
import com.techprotech.agenda.modulos.marketing.infraestructura.entidad.LandingMarketingEntidad;
import com.techprotech.agenda.modulos.marketing.infraestructura.repositorio.LandingMarketingRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioAuditoriaPlataforma;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.util.*;
@Service
public class ServicioLandingMarketingImpl implements ServicioLandingMarketing {
 private final LandingMarketingRepositorio repo;
 private final ObjectMapper json;
 private final ServicioAuditoriaPlataforma auditoria;
 public ServicioLandingMarketingImpl(LandingMarketingRepositorio repo,ObjectMapper json,ServicioAuditoriaPlataforma auditoria){this.repo=repo;this.json=json;this.auditoria=auditoria;}
 @Transactional(readOnly=true) public LandingMarketingDto publica(String slug){
  return dto(repo.findBySlugAndPublicadoTrue(slug).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Landing no disponible")));
 }
 @Transactional(readOnly=true) public List<LandingMarketingDto> listar(){return repo.findAll().stream().map(this::dto).toList();}
 @Transactional(readOnly=true) public List<String> indice(){return repo.findAllByPublicadoTrueOrderBySlugAsc().stream().map(LandingMarketingEntidad::getSlug).toList();}
 @Transactional(readOnly=true) public List<com.techprotech.agenda.modulos.marketing.api.dto.EnlaceLandingPublicaDto> navegacion(){
  return repo.findAllByPublicadoTrueOrderBySlugAsc().stream().map(this::dto)
   .map(d->new com.techprotech.agenda.modulos.marketing.api.dto.EnlaceLandingPublicaDto(d.slug(),d.etiqueta(),d.descripcion())).toList();
 }
 @Transactional public LandingMarketingDto guardar(String slug,LandingMarketingDto s,Long usuarioId){
  if(!slug.equals(s.slug()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El slug no coincide");
  validarImagen(s.imagenUrl());
  var e=repo.findBySlug(slug).orElseGet(LandingMarketingEntidad::new);
  if(e.getId()!=null&&!Objects.equals(s.version(),e.getVersion()))throw new ResponseStatusException(HttpStatus.CONFLICT,"La landing cambió. Actualiza antes de guardar");
  if(e.getId()==null&&s.version()!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Landing no encontrada");
  e.setSlug(slug);e.setPublicado(s.publicado());
  try{e.setContenido(json.writeValueAsString(s));}catch(Exception ex){throw new IllegalStateException("No se pudo guardar el contenido",ex);}
  e=repo.saveAndFlush(e);
  auditoria.registrar(usuarioId,"LANDING_GUARDAR","LANDING",slug,Map.of("publicado",s.publicado(),"version",e.getVersion()));
  return dto(e);
 }
 private void validarImagen(String url){
  if(url==null||url.isBlank())return;
  try{var u=URI.create(url);if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new IllegalArgumentException();}
  catch(IllegalArgumentException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La imagen debe usar una URL HTTPS válida");}
 }
 private LandingMarketingDto dto(LandingMarketingEntidad e){
  try{var s=json.readValue(e.getContenido(),LandingMarketingDto.class);return new LandingMarketingDto(e.getSlug(),e.isPublicado(),e.getVersion(),s.etiqueta(),s.titulo(),s.descripcion(),s.ctaPrincipal(),s.ctaSecundario(),s.tituloFinal(),s.imagenUrl(),s.imagenAlt(),s.seoTitulo(),s.seoDescripcion(),s.beneficios(),s.funcionalidades(),s.pasos(),s.preguntas());}
  catch(Exception ex){throw new IllegalStateException("Contenido de landing inválido",ex);}
 }
}
