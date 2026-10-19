package com.techprotech.agenda.soporte;

import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="FLUORA_REGRESION_MYSQL_URL", matches=".+")
class MysqlPrivilegiosTest {
    @Test void usuarioDePruebasNoAccedeALaBaseHabitual() throws Exception {
        try (var c = DriverManager.getConnection(System.getenv("FLUORA_REGRESION_MYSQL_URL"),
                System.getenv("FLUORA_REGRESION_MYSQL_USER"), System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD"));
             var s = c.createStatement()) {
            MysqlAislado.comprobar(c);
            // WHERE 1=0 no altera filas incluso si se ejecuta con una cuenta mal configurada.
            assertThatThrownBy(() -> s.executeQuery("SELECT id FROM agenda_db.cita WHERE 1=0"))
                    .isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getErrorCode()).isIn(1044,1142));
            assertThatThrownBy(() -> s.executeUpdate("UPDATE agenda_db.cita SET notas=notas WHERE 1=0"))
                    .isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getErrorCode()).isIn(1044,1142));
            try (var grants = s.executeQuery("SHOW GRANTS")) {
                while (grants.next()) {
                    String grant = grants.getString(1);
                    MysqlAislado.comprobarConcesion(grant);
                }
            }
            String db = "fluora_test_permisos_" + UUID.randomUUID().toString().replace("-", "");
            MysqlAislado.comprobarNombre(db);
            s.execute("CREATE DATABASE `" + db + "`");
            try { s.execute("CREATE TABLE `" + db + "`.prueba(id INT PRIMARY KEY)"); }
            finally { s.execute("DROP DATABASE `" + db + "`"); }
        }
    }
}
