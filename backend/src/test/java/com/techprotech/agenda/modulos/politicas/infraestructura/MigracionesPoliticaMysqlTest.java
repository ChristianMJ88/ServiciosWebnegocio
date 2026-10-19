package com.techprotech.agenda.modulos.politicas.infraestructura;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** DDL real sobre tablas vacías de una base efímera. Nunca modifica tablas ni datos del negocio. */
@EnabledIfEnvironmentVariable(named="FLUORA_REGRESION_MYSQL_URL",matches=".+")
class MigracionesPoliticaMysqlTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path temporal;
    @Test void migraSinCambiarDatosExistentesYRechazaPoliticaDeOtroTenant() throws Exception {
        String db = "fluora_test_007_" + UUID.randomUUID().toString().replace("-", "");
        try (var administracion = DriverManager.getConnection(System.getenv("FLUORA_REGRESION_MYSQL_URL"),
                System.getenv("FLUORA_REGRESION_MYSQL_USER"),System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD")); var admin = administracion.createStatement()) {
            com.techprotech.agenda.soporte.MysqlAislado.comprobar(administracion);
            String original = administracion.getCatalog();
            admin.execute("CREATE DATABASE `"+db+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            String urlAislada = System.getenv("FLUORA_REGRESION_MYSQL_URL").replaceFirst("/[^/?]+(?=\\?|$)", "/"+db);
            try (var c = DriverManager.getConnection(urlAislada, System.getenv("FLUORA_REGRESION_MYSQL_USER"),
                    System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD")); var s = c.createStatement()) {
                com.techprotech.agenda.soporte.MysqlAislado.comprobar(c);
                try (var actual = s.executeQuery("SELECT DATABASE()")) {actual.next(); assertThat(actual.getString(1)).isEqualTo(db);}
                s.execute("CREATE TABLE `"+db+"`.cita LIKE `"+original+"`.cita");
                s.execute("CREATE TABLE `"+db+"`.checkout_orden LIKE `"+original+"`.checkout_orden");
                // LIKE no copia FK. Permite repetir la regresión si el esquema local ya recibió la 007.
                for (String table : List.of("cita","checkout_orden")) {
                    var checks = new ArrayList<String>();
                    try (var q = c.prepareStatement("SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=? AND TABLE_NAME=? AND CONSTRAINT_TYPE='CHECK'")) {
                        q.setString(1,db); q.setString(2,table); try(var rs=q.executeQuery()){while(rs.next())checks.add(rs.getString(1));}
                    }
                    for (String check : checks) s.execute("ALTER TABLE "+table+" DROP CHECK `"+check.replace("`","``")+"`");
                    var indexes = new HashSet<String>();
                    try (var rs = c.getMetaData().getIndexInfo(db,null,table,false,false)) {while(rs.next()) indexes.add(rs.getString("INDEX_NAME"));}
                    for (String index : List.of("uk_cita_empresa_folio","uk_cita_empresa_id"))
                        if (indexes.contains(index)) s.execute("ALTER TABLE "+table+" DROP INDEX "+index);
                    for (String column : List.of("folio","politica_version_id","politica_aceptada_en")) {
                        boolean exists;
                        try (var rs = c.getMetaData().getColumns(db,null,table,column)) { exists=rs.next(); }
                        if (exists) s.execute("ALTER TABLE "+table+" DROP COLUMN "+column);
                    }
                }
                s.execute("CREATE TABLE empresa(id BIGINT PRIMARY KEY)"); s.execute("CREATE TABLE usuario(id BIGINT PRIMARY KEY)");
                s.execute("INSERT INTO empresa VALUES(7),(8)"); s.execute("INSERT INTO usuario VALUES(10)");
                s.execute("INSERT INTO cita(id,empresa_id,sucursal_id,servicio_id,prestador_id,cliente_id,estado,inicio,fin,precio,moneda,creada_por_usuario_id,reserva_grupo_id) VALUES"
                        +"(1,7,2,3,4,10,'CONFIRMADA','2026-10-17 10:00:00','2026-10-17 11:00:00',185,'MXN',10,NULL),"
                        +"(2,7,2,3,4,10,'PENDIENTE','2026-10-18 09:00:00','2026-10-18 10:00:00',80,'MXN',10,'grupo'),"
                        +"(3,7,2,3,4,10,'PENDIENTE','2026-10-18 10:00:00','2026-10-18 11:00:00',90,'MXN',10,'grupo'),"
                        +"(4,8,2,3,4,10,'PENDIENTE','2026-10-18 12:00:00','2026-10-18 13:00:00',95,'MXN',10,'grupo')");
                List<String> antes = filas(c);
                ScriptUtils.executeSqlScript(c,new FileSystemResource("src/main/resources/db/migration/V137__politica_versionada_y_base_ajustes_cita.sql"));
                ScriptUtils.executeSqlScript(c,new FileSystemResource("src/main/resources/db/migration/V138__folios_citas_existentes_con_guardas.sql"));
                var registro = new org.hibernate.boot.registry.StandardServiceRegistryBuilder().applySettings(Map.of(
                        "hibernate.connection.url", urlAislada,
                        "hibernate.connection.username", System.getenv("FLUORA_REGRESION_MYSQL_USER"),
                        "hibernate.connection.password", System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD"),
                        "hibernate.hbm2ddl.auto", "validate", "hibernate.connection.pool_size", 1)).build();
                try (var factory = new org.hibernate.boot.MetadataSources(registro)
                        .addAnnotatedClass(com.techprotech.agenda.modulos.politicas.infraestructura.entidad.PoliticaEmpresaEntidad.class)
                        .buildMetadata().buildSessionFactory()) { assertThat(factory.isOpen()).isTrue(); }
                finally { org.hibernate.boot.registry.StandardServiceRegistryBuilder.destroy(registro); }
                assertThat(filas(c)).isEqualTo(antes);
                try(var rs=s.executeQuery("SELECT folio FROM cita ORDER BY id")) {
                    var folios = new ArrayList<String>(); while(rs.next()) folios.add(rs.getString(1));
                    assertThat(folios).hasSize(4).doesNotHaveDuplicates();
                    assertThat(folios.get(0)).matches("FL-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{8}");
                    assertThat(folios.get(1)).endsWith("-1"); assertThat(folios.get(2)).isEqualTo(folios.get(1).replace("-1","-2"));
                    assertThat(folios.get(3)).matches("FL-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{8}");
                }
                try(var rs=s.executeQuery("SELECT COUNT(*) FROM ajuste_saldo_cita")){rs.next(); assertThat(rs.getInt(1)).isZero();}
                try(var rs=s.executeQuery("SELECT COUNT(*) FROM politica_empresa")){rs.next(); assertThat(rs.getInt(1)).isZero();}
                s.execute("INSERT INTO politica_empresa(id,empresa_id,version,modo_no_show,dias_aclaracion,contacto_reclamos_correo,plantilla_texto,texto_generado,texto_sha256,publicada_en,publicada_por_usuario_id) VALUES(1,7,1,'RETENER',90,'n@example.com','TEST','Texto',REPEAT('a',64),UTC_TIMESTAMP(),10)");
                assertThatThrownBy(() -> s.execute("UPDATE cita SET politica_version_id=1,politica_aceptada_en=UTC_TIMESTAMP() WHERE id=4"))
                        .isInstanceOf(SQLException.class);
                s.execute("UPDATE cita SET politica_version_id=1,politica_aceptada_en=UTC_TIMESTAMP() WHERE id=1");
                assertThatThrownBy(() -> s.execute("UPDATE cita SET politica_version_id=NULL WHERE id=1")).isInstanceOf(SQLException.class);
                var jpa = new org.hibernate.boot.registry.StandardServiceRegistryBuilder().applySettings(Map.of(
                        "hibernate.connection.url",urlAislada,"hibernate.connection.username",System.getenv("FLUORA_REGRESION_MYSQL_USER"),
                        "hibernate.connection.password",System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD"),"hibernate.hbm2ddl.auto","validate",
                        "hibernate.type.java_time_use_direct_jdbc",true,"hibernate.connection.pool_size",1)).build();
                try (var factory = new org.hibernate.boot.MetadataSources(jpa)
                        .addAnnotatedClass(com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad.class)
                        .buildMetadata().buildSessionFactory();var session=factory.openSession()) {
                    var tx=session.beginTransaction();
                    var antigua=session.find(com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad.class,2L);
                    assertThat(antigua.getPoliticaVersionId()).isNull();
                    s.execute("UPDATE cita SET politica_version_id=1,politica_aceptada_en=UTC_TIMESTAMP() WHERE id=2 AND empresa_id=7 AND politica_version_id IS NULL");
                    antigua.setNotas("Guardado posterior de entidad vieja");session.flush();tx.commit();
                    try(var rs=s.executeQuery("SELECT politica_version_id,politica_aceptada_en FROM cita WHERE id=2")) {
                        rs.next(); assertThat(rs.getLong(1)).isEqualTo(1L);assertThat(rs.getTimestamp(2)).isNotNull();
                    }
                } finally {org.hibernate.boot.registry.StandardServiceRegistryBuilder.destroy(jpa);}
                // Fuerza la guarda posterior con Flyway real. Una migración fallida no puede dejar folios a medias.
                s.execute("ALTER TABLE cita MODIFY COLUMN folio VARCHAR(16) NULL"); s.execute("UPDATE cita SET folio=NULL");
                String backfill=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/migration/V138__folios_citas_existentes_con_guardas.sql"));
                backfill=backfill.replace("INSERT INTO tmp_007_guardia SELECT COUNT(*) FROM tmp_007_huella", "UPDATE tmp_007_posterior SET huella=REPEAT('x',64) WHERE id=1;\nINSERT INTO tmp_007_guardia SELECT COUNT(*) FROM tmp_007_huella");
                java.nio.file.Files.writeString(temporal.resolve("V138__folios_falla_controlada.sql"),backfill);
                var flyway=org.flywaydb.core.Flyway.configure().dataSource(urlAislada,System.getenv("FLUORA_REGRESION_MYSQL_USER"),System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD"))
                        .locations("filesystem:"+temporal).baselineOnMigrate(true).baselineVersion("137").load();
                assertThatThrownBy(flyway::migrate).isInstanceOf(org.flywaydb.core.api.FlywayException.class);
                try(var rs=s.executeQuery("SELECT COUNT(*) FROM cita WHERE folio IS NOT NULL")){rs.next();assertThat(rs.getInt(1)).isZero();}
                try(var rs=s.executeQuery("SELECT success FROM flyway_schema_history WHERE version='138'")){assertThat(rs.next()).isTrue();assertThat(rs.getBoolean(1)).isFalse();}

            } finally {admin.execute("DROP DATABASE `"+db+"`");}
        }
    }
    private List<String> filas(Connection c) throws SQLException {
        var result = new ArrayList<String>();
        try(var s=c.createStatement();var rs=s.executeQuery("SELECT id,empresa_id,estado,inicio,fin,precio,moneda,creada_en,reserva_grupo_id FROM cita ORDER BY id")) {
            while(rs.next()) {var row=new StringBuilder();for(int i=1;i<=9;i++)row.append(rs.getString(i)).append('|');result.add(row.toString());}
        }
        return result;
    }
}
