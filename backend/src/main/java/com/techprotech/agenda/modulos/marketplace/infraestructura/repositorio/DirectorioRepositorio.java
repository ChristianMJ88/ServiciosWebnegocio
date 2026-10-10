package com.techprotech.agenda.modulos.marketplace.infraestructura.repositorio;
import com.techprotech.agenda.modulos.marketplace.api.dto.NegocioDirectorioResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public class DirectorioRepositorio {
 private final JdbcTemplate jdbc;
 public DirectorioRepositorio(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public List<NegocioDirectorioResponse> buscar(String texto,long cursor,int limite){
  String patron="%"+texto.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
  return jdbc.query("""
   SELECT c.empresa_id,c.slug,c.nombre_comercial,c.descripcion_corta,c.logo_url,c.direccion,c.telefono
   FROM empresa_marketplace m JOIN empresa_sitio_config c ON c.empresa_id=m.empresa_id
   JOIN empresa e ON e.id=c.empresa_id
   WHERE m.participante=true AND c.publicado=true AND e.estado='ACTIVA' AND c.empresa_id>?
   AND (c.nombre_comercial LIKE ? ESCAPE '!' OR c.direccion LIKE ? ESCAPE '!'
    OR EXISTS (SELECT 1 FROM servicio s WHERE s.empresa_id=c.empresa_id AND s.activo=true
      AND s.visible_publico=true AND s.nombre LIKE ? ESCAPE '!')
    OR EXISTS (SELECT 1 FROM sucursal b WHERE b.empresa_id=c.empresa_id AND b.activa=true
      AND b.direccion LIKE ? ESCAPE '!'))
   ORDER BY c.empresa_id LIMIT ?
   """,(rs,n)->new NegocioDirectorioResponse(rs.getLong("empresa_id"),rs.getString("slug"),
    rs.getString("nombre_comercial"),rs.getString("descripcion_corta"),rs.getString("logo_url"),
    rs.getString("direccion"),rs.getString("telefono")),cursor,patron,patron,patron,patron,limite);
 }
}
