package com.techprotech.agenda.modulos.marketing.aplicacion;
import com.techprotech.agenda.modulos.marketing.api.dto.AtribucionAdquisicionRequest;
import com.techprotech.agenda.modulos.marketing.infraestructura.entidad.AdquisicionEmpresaEntidad;
import com.techprotech.agenda.modulos.marketing.infraestructura.repositorio.AdquisicionEmpresaRepositorio;
import com.techprotech.agenda.modulos.marketing.infraestructura.repositorio.LandingMarketingRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
@Service
public class ServicioAdquisicionEmpresaImpl implements ServicioAdquisicionEmpresa {
 private final AdquisicionEmpresaRepositorio repositorio;
 private final LandingMarketingRepositorio landings;
 public ServicioAdquisicionEmpresaImpl(AdquisicionEmpresaRepositorio repositorio,LandingMarketingRepositorio landings){this.repositorio=repositorio;this.landings=landings;}
 @Override @Transactional(propagation=Propagation.MANDATORY)
 public void registrar(Long empresaId,AtribucionAdquisicionRequest atribucion){
  if(atribucion==null)return;
  AdquisicionEmpresaEntidad entidad=new AdquisicionEmpresaEntidad();
  entidad.setEmpresaId(empresaId);
  entidad.setSource(limpiar(atribucion.source()));entidad.setMedium(limpiar(atribucion.medium()));
  entidad.setCampaign(limpiar(atribucion.campaign()));entidad.setContent(limpiar(atribucion.content()));entidad.setTerm(limpiar(atribucion.term()));
  String vertical=atribucion.landingVertical();
  if(vertical!=null&&vertical.matches("[a-z0-9]+(?:-[a-z0-9]+)*")&&vertical.length()<=80&&landings.findBySlug(vertical).isPresent()){
   entidad.setLandingVertical(vertical);entidad.setFirstLandingPath("/"+vertical);
  }
  if(entidad.getSource()==null&&entidad.getMedium()==null&&entidad.getCampaign()==null&&entidad.getContent()==null&&entidad.getTerm()==null&&entidad.getLandingVertical()==null)return;
  entidad.setAcquiredAt(Instant.now());
  repositorio.save(entidad);
 }
 private String limpiar(String valor){return valor!=null&&valor.matches("[A-Za-z][A-Za-z0-9_-]{0,79}")&&!valor.matches(".*[0-9]{10,}.*")?valor:null;}
}
