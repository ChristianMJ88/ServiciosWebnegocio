package com.techprotech.agenda.soporte;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="FLUORA_REGRESION_MYSQL_URL",matches=".+")
class CalificacionResenaMysqlTest {
    @ParameterizedTest @ValueSource(strings={"TINYINT","INT","SMALLINT"})
    void v144ConservaValoresIndicesForaneaYCheckSinCambiarLaPolitica(String source) throws Exception {
        String base=System.getenv("FLUORA_REGRESION_MYSQL_URL"),user=System.getenv("FLUORA_REGRESION_MYSQL_USER"),password=System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD");
        String schema="fluora_test_v144_"+UUID.randomUUID().toString().replace("-","");
        try(var parent=DriverManager.getConnection(base,user,password)) {
            MysqlAislado.comprobar(parent);
            try(var s=parent.createStatement()){s.execute("CREATE DATABASE `"+schema+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");}
            try {
                var ds=new DriverManagerDataSource(MysqlAislado.urlHija(base,schema),user,password);
                EsquemaMigraciones.vacio(ds);EsquemaMigraciones.migrar(ds,"143");
                var jdbc=new JdbcTemplate(ds);
                jdbc.execute("ALTER TABLE empresa_sitio_resena MODIFY calificacion "+source+" NOT NULL");
                jdbc.update("INSERT INTO empresa_sitio_resena(empresa_id,nombre,calificacion,comentario) VALUES(1,'Fixture uno',1,'Comentario sintético'),(1,'Fixture cinco',5,'Comentario sintético')");
                var before=EsquemaMigraciones.columnas(ds);
                var rows=jdbc.queryForList("SELECT * FROM empresa_sitio_resena ORDER BY id");
                String checkSql="SELECT CONSTRAINT_NAME,CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='chk_sitio_resena_calificacion'";
                var constraint=jdbc.queryForList(checkSql);assertThat(constraint).hasSize(1);
                String indexSql="SELECT INDEX_NAME,COLUMN_NAME,SEQ_IN_INDEX,NON_UNIQUE FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='empresa_sitio_resena' ORDER BY INDEX_NAME,SEQ_IN_INDEX";
                var indices=jdbc.queryForList(indexSql);
                String fkSql="SELECT CONSTRAINT_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='empresa_sitio_resena' AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY CONSTRAINT_NAME,ORDINAL_POSITION";
                var fk=jdbc.queryForList(fkSql);assertThat(fk).isNotEmpty();
                if(source.equals("SMALLINT")) {
                    assertThatThrownBy(()->EsquemaMigraciones.migrar(ds,null)).isInstanceOf(IllegalStateException.class).hasStackTraceContaining("144");
                    assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version='144'",Boolean.class)).isFalse();
                    assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version='143'",Boolean.class)).isTrue();
                    assertThat(EsquemaMigraciones.columnas(ds)).isEqualTo(before);
                } else {
                    EsquemaMigraciones.migrar(ds,null);
                    var after=EsquemaMigraciones.columnas(ds).get("empresa_sitio_resena.calificacion");
                    assertThat(after.tipo()).isEqualTo("int");
                    assertThat(after.nulable()).isEqualTo(before.get("empresa_sitio_resena.calificacion").nulable());
                    assertThat(after.defecto()).isNull();
                    assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version='144'",Boolean.class)).isTrue();
                    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=0",Integer.class)).isZero();
                }
                assertThat(jdbc.queryForList("SELECT * FROM empresa_sitio_resena ORDER BY id")).isEqualTo(rows);
                assertThat(jdbc.queryForList(checkSql)).isEqualTo(constraint);
                assertThat(jdbc.queryForList(indexSql)).isEqualTo(indices);
                assertThat(jdbc.queryForList(fkSql)).isEqualTo(fk);
                for(int invalid:new int[]{0,6})
                    assertThatThrownBy(()->jdbc.update("INSERT INTO empresa_sitio_resena(empresa_id,nombre,calificacion,comentario) VALUES(1,'Inválida',?,'No debe insertarse')",invalid)).hasRootCauseInstanceOf(java.sql.SQLException.class).rootCause().satisfies(error -> {
                        assertThat(((java.sql.SQLException) error).getErrorCode()).isEqualTo(3819);
                        assertThat(error.getMessage()).contains("chk_sitio_resena_calificacion");
                    });
            } finally {
                MysqlAislado.comprobarNombre(schema);
                try(var s=parent.createStatement()){s.execute("DROP DATABASE `"+schema+"`");}
            }
        }
    }
}
