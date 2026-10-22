package com.techprotech.agenda.soporte;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named="FLUORA_REGRESION_MYSQL_URL", matches=".+")
class EsquemaMigracionesMysqlTest {
    @Test void todasLasEntidadesValidanContraElEsquemaCreadoPorFlyway() throws Exception {
        String base = System.getenv("FLUORA_REGRESION_MYSQL_URL"), user = System.getenv("FLUORA_REGRESION_MYSQL_USER"), password = System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD");
        String schema = "fluora_test_schema_" + UUID.randomUUID().toString().replace("-", ""), ref = schema + "_ref";
        var created = new ArrayList<String>();
        try (var c = DriverManager.getConnection(base, user, password)) {
            MysqlAislado.comprobar(c);
            try {
                for (String name : List.of(schema, ref)) {
                    MysqlAislado.comprobarNombre(name);
                    try (var s = c.createStatement()) { s.execute("CREATE DATABASE `" + name + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"); }
                    created.add(name);
                }
                var ds = new DriverManagerDataSource(MysqlAislado.urlHija(base, schema), user, password);
                var reference = new DriverManagerDataSource(MysqlAislado.urlHija(base, ref), user, password);
                EsquemaMigraciones.vacio(ds); EsquemaMigraciones.vacio(reference);
                String target=System.getenv("FLUORA_ESQUEMA_TARGET");
                if(target!=null && !target.equals("142")) throw new IllegalArgumentException("Solo se admite el diagnóstico previo V142 o la versión actual");
                EsquemaMigraciones.migrar(ds, target);
                var jdbc = new JdbcTemplate(ds);
                var versions = jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE version IS NOT NULL AND success=1 ORDER BY CAST(version AS UNSIGNED)", String.class);
                assertThat(versions).doesNotHaveDuplicates().containsExactlyElementsOf(EsquemaMigraciones.versiones().stream()
                        .filter(v->target==null || Integer.parseInt(v)<=142).toList());
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=0", Long.class)).isZero();
                EsquemaMigraciones.hibernate(reference, "create");
                var report = EsquemaMigraciones.comparar(ds, reference);
                Path output = Path.of("target", "esquema-migraciones.json");
                Files.createDirectories(output.getParent());
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
                assertThat(report.diferencias()).withFailMessage("Todas las diferencias de tipos Flyway/Hibernate: %s; informe %s", report.diferencias(), output).isEmpty();
                var columns=EsquemaMigraciones.columnas(ds);
                for(String key:List.of("checkout_orden.public_id","sucursal.pais_codigo")) {
                    assertThat(columns.get(key).tipo()).isEqualTo("varchar");
                    assertThat(columns.get(key).charset()).isEqualTo("utf8mb4");
                    assertThat(columns.get(key).collation()).isEqualTo("utf8mb4_0900_ai_ci");
                }
                EsquemaMigraciones.hibernate(ds, "validate");
            } finally {
                Collections.reverse(created);
                for (String name : created) {
                    MysqlAislado.comprobarNombre(name);
                    try (var s = c.createStatement()) { s.execute("DROP DATABASE `" + name + "`"); }
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"CHAR", "VARCHAR", "PARCIAL"})
    void v143ConservaDatosIndicesNulabilidadYCollationIndividual(String original) throws Exception {
        String base=System.getenv("FLUORA_REGRESION_MYSQL_URL"), user=System.getenv("FLUORA_REGRESION_MYSQL_USER"), password=System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD");
        String schema="fluora_test_v143_"+UUID.randomUUID().toString().replace("-", "");
        try (var c=DriverManager.getConnection(base,user,password)) {
            MysqlAislado.comprobar(c);
            try (var s=c.createStatement()) { s.execute("CREATE DATABASE `"+schema+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"); }
            try {
                var ds=new DriverManagerDataSource(MysqlAislado.urlHija(base,schema),user,password);
                EsquemaMigraciones.vacio(ds); EsquemaMigraciones.migrar(ds,"142");
                var jdbc=new JdbcTemplate(ds);
                // La collation de las columnas es deliberadamente distinta a la de su tabla/base.
                jdbc.execute("ALTER TABLE checkout_orden MODIFY public_id "+(original.equals("PARCIAL")?"VARCHAR":original)+"(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL");
                jdbc.execute("ALTER TABLE sucursal MODIFY pais_codigo "+(original.equals("PARCIAL")?"CHAR":original)+"(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL");
                String id=UUID.randomUUID().toString();
                jdbc.update("INSERT INTO checkout_orden(public_id,empresa_id,sucursal_id,idempotencia,estado,correo_cliente,moneda,subtotal,descuento_total,impuestos_total,total,expira_en) VALUES(?,1,1,'fixture-v143','CREADA','fixture@example.test','MXN',100,0,0,100,DATE_ADD(UTC_TIMESTAMP(),INTERVAL 1 DAY))",id);
                jdbc.update("INSERT INTO checkout_orden(public_id,empresa_id,sucursal_id,idempotencia,estado,correo_cliente,moneda,subtotal,descuento_total,impuestos_total,total,expira_en) VALUES('corto',1,1,'fixture-v143-corto','CREADA','fixture@example.test','MXN',100,0,0,100,DATE_ADD(UTC_TIMESTAMP(),INTERVAL 1 DAY))");
                jdbc.update("UPDATE sucursal SET pais_codigo='MX' WHERE id=1");
                jdbc.update("INSERT INTO sucursal(empresa_id,nombre,pais_codigo) VALUES(1,'Fixture null',NULL),(1,'Fixture vacío',''),(1,'Fixture corto','M')");
                var before=EsquemaMigraciones.columnas(ds);
                var valuesBefore=jdbc.queryForList("SELECT HEX(public_id) AS bytes,creada_en,actualizada_en FROM checkout_orden ORDER BY id");
                var countryBefore=jdbc.queryForList("SELECT id,HEX(pais_codigo) AS bytes FROM sucursal ORDER BY id");
                var indices=jdbc.queryForList("SELECT TABLE_NAME,INDEX_NAME,COLUMN_NAME,SEQ_IN_INDEX,NON_UNIQUE FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('checkout_orden','sucursal') ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX");
                var timings=new ArrayList<EsquemaMigraciones.MedicionV143>();
                EsquemaMigraciones.migrar(EsquemaMigraciones.medirV143(ds,timings),null);
                assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version='143'",Boolean.class)).isTrue();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=0",Integer.class)).isZero();
                assertThat(timings).hasSize(2).allMatch(EsquemaMigraciones.MedicionV143::completado);
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(Path.of("target","v143-preservacion-"+original+".json").toFile(),
                        Map.of("origen",original,"filasCheckout",valuesBefore.size(),"filasSucursal",countryBefore.size(),"ejecucionesV143",timings));
                var after=EsquemaMigraciones.columnas(ds);
                for (String key:List.of("checkout_orden.public_id","sucursal.pais_codigo")) {
                    assertThat(after.get(key).tipo()).isEqualTo("varchar");
                    assertThat(after.get(key).nulable()).isEqualTo(before.get(key).nulable());
                    assertThat(after.get(key).longitud()).isEqualTo(before.get(key).longitud());
                    assertThat(after.get(key).charset()).isEqualTo(before.get(key).charset());
                    assertThat(after.get(key).collation()).isEqualTo(before.get(key).collation()).isEqualTo("utf8mb4_bin");
                }
                assertThat(jdbc.queryForObject("SELECT public_id FROM checkout_orden WHERE idempotencia='fixture-v143'",String.class)).isEqualTo(id);
                assertThat(jdbc.queryForObject("SELECT pais_codigo FROM sucursal WHERE id=1",String.class)).isEqualTo("MX");
                assertThat(jdbc.queryForList("SELECT HEX(public_id) AS bytes,creada_en,actualizada_en FROM checkout_orden ORDER BY id")).isEqualTo(valuesBefore);
                assertThat(jdbc.queryForList("SELECT id,HEX(pais_codigo) AS bytes FROM sucursal ORDER BY id")).isEqualTo(countryBefore);
                assertThat(jdbc.queryForList("SELECT TABLE_NAME,INDEX_NAME,COLUMN_NAME,SEQ_IN_INDEX,NON_UNIQUE FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('checkout_orden','sucursal') ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX")).isEqualTo(indices);
                assertThat(after.keySet()).isEqualTo(before.keySet());
                assertThat(jdbc.queryForObject("SELECT NON_UNIQUE FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='checkout_orden' AND INDEX_NAME='uk_checkout_orden_public_id'",Integer.class)).isZero();
                assertThatThrownBy(()->jdbc.update("INSERT INTO checkout_orden(public_id,empresa_id,sucursal_id,idempotencia,estado,correo_cliente,moneda,subtotal,descuento_total,impuestos_total,total,expira_en) VALUES(?,1,1,'fixture-duplicado','CREADA','fixture@example.test','MXN',100,0,0,100,UTC_TIMESTAMP())",id))
                        .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
            } finally {
                MysqlAislado.comprobarNombre(schema);
                try (var s=c.createStatement()) { s.execute("DROP DATABASE `"+schema+"`"); }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"LONGITUD", "NULABILIDAD"})
    void v143RechazaUnTercerEstadoSinCambiarLasColumnas(String kind) throws Exception {
        String base=System.getenv("FLUORA_REGRESION_MYSQL_URL"),user=System.getenv("FLUORA_REGRESION_MYSQL_USER"),password=System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD");
        String schema="fluora_test_v143_bad_"+UUID.randomUUID().toString().replace("-", "");
        try(var c=DriverManager.getConnection(base,user,password)) {
            MysqlAislado.comprobar(c);
            try(var s=c.createStatement()){s.execute("CREATE DATABASE `"+schema+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");}
            try {
                var ds=new DriverManagerDataSource(MysqlAislado.urlHija(base,schema),user,password);
                EsquemaMigraciones.vacio(ds);EsquemaMigraciones.migrar(ds,"142");
                var jdbc=new JdbcTemplate(ds);
                if (kind.equals("LONGITUD")) jdbc.execute("ALTER TABLE checkout_orden MODIFY public_id VARCHAR(40) NOT NULL");
                else {
                    jdbc.execute("ALTER TABLE checkout_orden MODIFY public_id VARCHAR(36) NOT NULL");
                    jdbc.update("UPDATE sucursal SET pais_codigo='MX'");
                    jdbc.execute("ALTER TABLE sucursal MODIFY pais_codigo CHAR(2) NOT NULL");
                }
                var before=EsquemaMigraciones.columnas(ds);
                assertThatThrownBy(()->EsquemaMigraciones.migrar(ds,null)).isInstanceOf(IllegalStateException.class).hasStackTraceContaining("143");
                assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version='143'",Boolean.class)).isFalse();
                assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version='142'",Boolean.class)).isTrue();
                assertThat(EsquemaMigraciones.columnas(ds)).isEqualTo(before);
            } finally {
                MysqlAislado.comprobarNombre(schema);
                try(var s=c.createStatement()){s.execute("DROP DATABASE `"+schema+"`");}
            }
        }
    }
}
