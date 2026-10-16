package com.techprotech.agenda.modulos.marketplace.infraestructura.repositorio;
import com.techprotech.agenda.modulos.marketplace.api.dto.NegocioDirectorioResponse;
import com.techprotech.agenda.modulos.marketplace.api.dto.ServicioDirectorioResponse;
import com.techprotech.agenda.modulos.marketplace.api.dto.SucursalDirectorioResponse;
import com.techprotech.agenda.modulos.sucursales.aplicacion.HorarioComercial;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
@Repository
public class DirectorioRepositorio {
    private final java.time.Clock reloj;
 private final JdbcTemplate jdbc;
 public DirectorioRepositorio(JdbcTemplate jdbc, java.time.Clock reloj){
        this.reloj = reloj;this.jdbc=jdbc;}
 private String patron(String texto){return "%"+texto.replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
 public List<NegocioDirectorioResponse> buscar(String texto,long cursor,int limite){return buscar(texto,"","",cursor,limite);}
 public List<NegocioDirectorioResponse> buscar(String texto,String ubicacion,String categoria,long cursor,int limite){
  String patron=patron(texto);
  return jdbc.query("""
   SELECT c.empresa_id,c.slug,c.nombre_comercial,c.descripcion_corta,c.logo_url,c.direccion,c.telefono,c.hero_imagen_url
   FROM empresa_marketplace m JOIN empresa_sitio_config c ON c.empresa_id=m.empresa_id
   JOIN empresa e ON e.id=c.empresa_id
   WHERE m.participante=true AND c.publicado=true AND e.estado='ACTIVA' AND c.empresa_id>?
   AND (c.nombre_comercial LIKE ? ESCAPE '!' OR c.direccion LIKE ? ESCAPE '!'
    OR EXISTS (SELECT 1 FROM servicio s WHERE s.empresa_id=c.empresa_id AND s.activo=true
      AND s.visible_publico=true AND s.nombre LIKE ? ESCAPE '!'
      AND EXISTS (SELECT 1 FROM servicio_sucursal ss JOIN sucursal b ON b.id=ss.sucursal_id AND b.empresa_id=ss.empresa_id
       WHERE ss.servicio_id=s.id AND ss.empresa_id=s.empresa_id AND ss.activo=true AND b.activa=true))
    OR EXISTS (SELECT 1 FROM sucursal b WHERE b.empresa_id=c.empresa_id AND b.activa=true
      AND CONCAT_WS(' ',b.direccion,b.municipio,b.estado_region,b.pais_codigo,b.codigo_postal) LIKE ? ESCAPE '!'))
   AND (?='' OR c.direccion LIKE ? ESCAPE '!' OR EXISTS
    (SELECT 1 FROM sucursal b WHERE b.empresa_id=c.empresa_id AND b.activa=true AND CONCAT_WS(' ',b.direccion,b.municipio,b.estado_region,b.pais_codigo,b.codigo_postal) LIKE ? ESCAPE '!'))
   AND (?='' OR EXISTS (SELECT 1 FROM servicio s JOIN grupo_servicio g ON g.id=s.grupo_id AND g.empresa_id=s.empresa_id
    WHERE s.empresa_id=c.empresa_id AND s.activo=true AND s.visible_publico=true AND g.activo=true AND g.nombre LIKE ? ESCAPE '!'
     AND EXISTS (SELECT 1 FROM servicio_sucursal ss JOIN sucursal b ON b.id=ss.sucursal_id AND b.empresa_id=ss.empresa_id
       WHERE ss.servicio_id=s.id AND ss.empresa_id=s.empresa_id AND ss.activo=true AND b.activa=true)))
   ORDER BY c.empresa_id LIMIT ?
   """,(rs,n)->new NegocioDirectorioResponse(rs.getLong("empresa_id"),rs.getString("slug"),
    rs.getString("nombre_comercial"),rs.getString("descripcion_corta"),rs.getString("logo_url"),
    rs.getString("direccion"),rs.getString("telefono"),rs.getString("hero_imagen_url"),List.of()),
    cursor,patron,patron,patron,patron,ubicacion,patron(ubicacion),patron(ubicacion),categoria,patron(categoria),limite);
 }
 public Map<Long,List<SucursalDirectorioResponse>> sucursales(List<Long> empresas){
  if(empresas.isEmpty())return Map.of();
  var resultado=new HashMap<Long,List<SucursalDirectorioResponse>>();
  String marcadores=String.join(",",Collections.nCopies(empresas.size(),"?"));
  var empresaDeSucursal=new HashMap<Long,Long>();
  var sedes=new java.util.LinkedHashMap<Long,SucursalDirectorioResponse>();
  var horarios=new HashMap<Long,List<HorarioComercial>>();
  jdbc.query("SELECT b.*,r.dia_semana,r.hora_inicio,r.hora_fin,r.vigente_desde,r.vigente_hasta FROM sucursal b "
   +"LEFT JOIN regla_disponibilidad r ON r.empresa_id=b.empresa_id AND r.tipo_sujeto='SUCURSAL' AND r.sujeto_id=b.id "
   +"WHERE b.activa=true AND b.empresa_id IN ("+marcadores+") ORDER BY b.empresa_id,b.id,r.dia_semana,r.hora_inicio",rs->{
   long id=rs.getLong("id");
   empresaDeSucursal.put(id,rs.getLong("empresa_id"));
   sedes.putIfAbsent(id,new SucursalDirectorioResponse(id,rs.getString("nombre"),rs.getString("direccion"),rs.getString("municipio"),rs.getString("estado_region"),
    rs.getString("pais_codigo"),rs.getString("codigo_postal"),rs.getString("zona_horaria"),List.of()));
   var dia=rs.getObject("dia_semana");
   if(dia!=null){
    var fecha=java.time.LocalDate.now(reloj.withZone(java.time.ZoneId.of(rs.getString("zona_horaria"))));
    var desde=rs.getObject("vigente_desde",java.time.LocalDate.class);
    var hasta=rs.getObject("vigente_hasta",java.time.LocalDate.class);
    if((desde==null||!fecha.isBefore(desde))&&(hasta==null||!fecha.isAfter(hasta))){
     var h=new HorarioComercial(((Number)dia).intValue(),rs.getTime("hora_inicio").toLocalTime().toString(),rs.getTime("hora_fin").toLocalTime().toString());
     var lista=horarios.computeIfAbsent(id,k->new ArrayList<>());if(!lista.contains(h))lista.add(h);
    }
   }
  },empresas.toArray());
  for(var sede:sedes.values()){
   var completa=new SucursalDirectorioResponse(sede.id(),sede.nombre(),sede.direccion(),sede.municipio(),sede.estadoRegion(),sede.paisCodigo(),sede.codigoPostal(),sede.zonaHoraria(),List.copyOf(horarios.getOrDefault(sede.id(),List.of())));
   // Empresa del registro proviene de la consulta, no del cliente.
   // Se agrupa abajo mediante el mapa de ids que conserva el aislamiento del lote.
   resultado.computeIfAbsent(empresaDeSucursal.get(sede.id()),k->new ArrayList<>()).add(completa);
  }
  return resultado;
 }
 public Map<Long,List<ServicioDirectorioResponse>> servicios(List<Long> empresas){
  return servicios(empresas,"","");
 }
 public Map<Long,List<ServicioDirectorioResponse>> servicios(List<Long> empresas,String texto,String categoria){
  if(empresas.isEmpty())return Map.of();
  String marcadores=String.join(",",Collections.nCopies(empresas.size(),"?"));
  Map<Long,List<ServicioDirectorioResponse>> resultado=new HashMap<>();
  jdbc.query("""
    SELECT * FROM (SELECT s.empresa_id,s.id,s.nombre,s.precio,s.moneda,s.duracion_minutos,g.nombre categoria,
     ROW_NUMBER() OVER (PARTITION BY s.empresa_id ORDER BY
      CASE WHEN ?<>'' AND g.nombre LIKE ? ESCAPE '!' THEN 0 ELSE 1 END,
      CASE WHEN ?<>'' AND s.nombre LIKE ? ESCAPE '!' THEN 0 ELSE 1 END,s.orden_publico,s.id) posicion
     FROM servicio s
     LEFT JOIN grupo_servicio g ON g.id=s.grupo_id AND g.empresa_id=s.empresa_id AND g.activo=true
     WHERE s.activo=true AND s.visible_publico=true
      AND EXISTS (SELECT 1 FROM servicio_sucursal ss JOIN sucursal b ON b.id=ss.sucursal_id AND b.empresa_id=ss.empresa_id
       WHERE ss.servicio_id=s.id AND ss.empresa_id=s.empresa_id AND ss.activo=true AND b.activa=true)
      AND s.empresa_id IN (
    """+marcadores+") ) seleccion WHERE posicion<=3 ORDER BY empresa_id,posicion",
    rs->{resultado.computeIfAbsent(rs.getLong("empresa_id"),id->new ArrayList<>()).add(
      new ServicioDirectorioResponse(rs.getLong("id"),rs.getString("nombre"),rs.getString("categoria"),
        rs.getBigDecimal("precio"),rs.getString("moneda"),rs.getInt("duracion_minutos")));},argumentosServicios(empresas,texto,categoria));
  return resultado;
 }
 private Object[] argumentosServicios(List<Long> empresas,String texto,String categoria){
  var argumentos=new ArrayList<Object>();
  argumentos.add(categoria);argumentos.add(patron(categoria));argumentos.add(texto);argumentos.add(patron(texto));
  argumentos.addAll(empresas);return argumentos.toArray();
 }
}
