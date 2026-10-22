package com.techprotech.agenda.modulos.dominios.infraestructura;
import org.flywaydb.core.api.callback.*;
import org.springframework.stereotype.Component;
import java.sql.SQLException;

/** V141 is immutable. Sanitize incompatible, unverified legacy inputs before its first execution. */
@Component
public class PrepararLegadoDominiosFlyway implements Callback {
 private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(PrepararLegadoDominiosFlyway.class);
 public boolean supports(Event event,Context context){return event==Event.BEFORE_EACH_MIGRATE&&context.getMigrationInfo()!=null&&context.getMigrationInfo().getVersion()!=null&&"141".equals(context.getMigrationInfo().getVersion().toString());}
 public boolean canHandleInTransaction(Event event,Context context){return true;}
 public void handle(Event event,Context context){
  try(var st=context.getConnection().prepareStatement("UPDATE empresa_sitio_config SET dominio_principal=NULL WHERE dominio_principal IS NOT NULL AND (CHAR_LENGTH(dominio_principal)>253 OR LENGTH(dominio_principal)<>CHAR_LENGTH(dominio_principal) OR NOT REGEXP_LIKE(dominio_principal,?,'c'))")){
   st.setString(1,"^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?([.][a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+\\z");
   LOG.info("Dominios legacy incompatibles retirados antes de V141: {}",st.executeUpdate());
  }catch(SQLException ex){throw new org.flywaydb.core.api.FlywayException("No se pudo preparar el legado de dominios antes de V141",ex);}
 }
 public String getCallbackName(){return "Preparar legado de dominios antes de V141";}
}
