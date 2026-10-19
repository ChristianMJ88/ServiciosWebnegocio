package com.techprotech.agenda.modulos.caja.aplicacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="FLUORA_REGRESION_MYSQL_URL",matches=".+")
class MigracionPointDivididoMysqlTest {
 @Test void v139AgregaColumnasNulasSinAlterarFilasPrevias() throws Exception {
  String db="fluora_test_010_"+UUID.randomUUID().toString().replace("-","");
  try(var c=DriverManager.getConnection(System.getenv("FLUORA_REGRESION_MYSQL_URL"),System.getenv("FLUORA_REGRESION_MYSQL_USER"),System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD"));var s=c.createStatement()) {
   com.techprotech.agenda.soporte.MysqlAislado.comprobar(c);
   var origenAntes=estadoOrigen(c);
   String original=c.getCatalog();s.execute("CREATE DATABASE `"+db+"`");
   try {
    for(String tabla:List.of("orden_point_caja","pago_venta"))s.execute("CREATE TABLE `"+db+"`."+tabla+" LIKE `"+original+"`."+tabla);
    String aislada=System.getenv("FLUORA_REGRESION_MYSQL_URL").replaceFirst("/[^/?]+(?=\\?|$)","/"+db);
    try(var efimera=DriverManager.getConnection(aislada,System.getenv("FLUORA_REGRESION_MYSQL_USER"),System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD"))) {
    com.techprotech.agenda.soporte.MysqlAislado.comprobar(efimera);
    assertThat(efimera.getCatalog()).isEqualTo(db);
    for(var columna:List.of(new String[]{"orden_point_caja","pagos_previstos_json"},new String[]{"pago_venta","monto_recibido"},new String[]{"pago_venta","cambio"})) {
     try(var rs=c.getMetaData().getColumns(db,null,columna[0],columna[1])){if(rs.next())s.execute("ALTER TABLE `"+db+"`."+columna[0]+" DROP COLUMN "+columna[1]);}
    }
    s.execute("INSERT INTO `"+db+"`.orden_point_caja(id,public_id,empresa_id,venta_id,terminal_id,idempotencia,estado,importe,moneda,creado_por_usuario_id) VALUES(1,'prueba',7,8,9,'prueba','CREADO',700,'MXN',10)");
    s.execute("INSERT INTO `"+db+"`.pago_venta(id,empresa_id,venta_id,monto,metodo_pago,registrado_en) VALUES(1,7,8,700,'TARJETA','2026-10-17 10:00:00')");
    var antes=huellas(efimera);
    org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(efimera,new org.springframework.core.io.FileSystemResource("src/main/resources/db/migration/V139__partes_previstas_point_y_cambio_pago_venta.sql"));
    assertThat(huellas(efimera)).isEqualTo(antes);
    try(var rs=s.executeQuery("SELECT pagos_previstos_json FROM `"+db+"`.orden_point_caja")){rs.next();assertThat(rs.getString(1)).isNull();}
    try(var rs=s.executeQuery("SELECT monto_recibido,cambio FROM `"+db+"`.pago_venta")){rs.next();assertThat(rs.getBigDecimal(1)).isNull();assertThat(rs.getBigDecimal(2)).isNull();}
    try(var rs=c.getMetaData().getColumns(db,null,"orden_point_caja","pagos_previstos_json")){assertThat(rs.next()).isTrue();assertThat(rs.getString("TYPE_NAME")).isEqualToIgnoringCase("JSON");}
    }
   } finally {c.setCatalog(original);s.execute("DROP DATABASE `"+db+"`");assertThat(estadoOrigen(c)).isEqualTo(origenAntes);}
  }
 }
 private List<String> estadoOrigen(Connection c)throws Exception {
  var resultado=new ArrayList<>(huellas(c));
  for(String tabla:List.of("orden_point_caja","pago_venta"))try(var r=c.getMetaData().getColumns(c.getCatalog(),null,tabla,null)) {while(r.next())resultado.add(tabla+":"+r.getString("COLUMN_NAME")+":"+r.getString("TYPE_NAME")+":"+r.getInt("NULLABLE"));}
  return resultado;
 }
 private List<String> huellas(Connection c)throws Exception {
  var resultado=new ArrayList<String>();
  for(String tabla:List.of("orden_point_caja","pago_venta")) {
   var columnas=new ArrayList<String>();try(var r=c.getMetaData().getColumns(c.getCatalog(),null,tabla,null)){while(r.next()){String n=r.getString("COLUMN_NAME");if(!Set.of("pagos_previstos_json","monto_recibido","cambio").contains(n))columnas.add("COALESCE(CAST(`"+n+"` AS CHAR),'NULL')");}}
   try(var s=c.createStatement();var r=s.executeQuery("SELECT SHA2(CONCAT_WS('|',"+String.join(",",columnas)+"),256) FROM "+tabla+" ORDER BY id")){while(r.next())resultado.add(r.getString(1));}
  }return resultado;
 }
}
