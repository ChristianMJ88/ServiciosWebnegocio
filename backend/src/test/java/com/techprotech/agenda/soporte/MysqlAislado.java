package com.techprotech.agenda.soporte;

import java.sql.Connection;
import java.sql.SQLException;

/** Guarda anterior a cualquier DDL. Los permisos MySQL son una segunda barrera. */
public final class MysqlAislado {
    private MysqlAislado() {}
    public static void comprobar(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT DATABASE()")) {
            if (!result.next()) throw new IllegalStateException("Pruebas MySQL sin base seleccionada");
            comprobarNombre(result.getString(1));
        }
        try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT CURRENT_ROLE()")) {
            if (!result.next() || !"NONE".equals(result.getString(1)))
                throw new IllegalStateException("Roles MySQL no permitidos en pruebas");
        }
        try (var statement = connection.createStatement(); var result = statement.executeQuery("SHOW GRANTS")) {
            while (result.next()) comprobarConcesion(result.getString(1));
        }
    }
    public static String urlHija(String base, String destino) {
        comprobarNombre(destino);
        if (base == null || !base.matches("jdbc:mysql://[^/?]+/fluora_test_[a-z0-9_]+(?:[?].*)?")
                || base.matches("(?i).*[?&](database|databaseName|catalog)=.*"))
            throw new IllegalStateException("URL JDBC aislada sin base explícita válida");
        String hija = base.replaceFirst("/[^/?]+(?=\\?|$)", "/" + destino);
        if (base.equals(hija)) throw new IllegalStateException("La base hija debe ser diferente");
        return hija;
    }
    public static void comprobarConcesion(String grant) {
        String regla = grant.split(" TO ", 2)[0];
        if (grant.contains("WITH GRANT OPTION")) throw new IllegalStateException("GRANT OPTION prohibido");
        if (regla.equals("GRANT USAGE ON *.*")) return;
        int on = regla.indexOf(" ON ");
        if (!regla.startsWith("GRANT ") || on < 0
                || !regla.substring(6, on).matches("[A-Z_, ]+")
                || !regla.substring(on + 4).equals("`fluora\\_test\\_%`.*"))
            throw new IllegalStateException("Permisos MySQL fuera del patrón aislado");
    }
    public static void comprobarNombre(String nombre) {
        if (nombre == null || !nombre.matches("fluora_test_[a-z0-9_]+"))
            throw new IllegalStateException("Pruebas MySQL: solo se permiten esquemas fluora_test_*");
    }
}
