package com.techprotech.agenda.compartido.tiempo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import static org.assertj.core.api.Assertions.assertThat;

@Isolated("Verifica la zona de arranque fuera de pruebas que cambian la JVM")
class UsoRelojTest {
    private static final Pattern SIN_RELOJ = Pattern.compile(
            "\\b(?:LocalDate|LocalDateTime|LocalTime|OffsetDateTime|ZonedDateTime|Instant)\\s*\\.\\s*now\\s*\\(\\s*\\)");
    private static final Pattern IMPORT_NOW = Pattern.compile(
            "import\\s+static\\s+java\\.time\\.\\w+\\.(?:now|\\*)\\s*;");

    @Test void codigoDeAplicacionNoConsultaAhoraSinReloj() throws Exception {
        var infracciones = new ArrayList<String>();
        try (var rutas = Files.walk(Path.of("src/main/java"))) {
            for (var ruta : rutas.filter(p -> p.toString().endsWith(".java")).toList()) {
                var codigo = Files.readString(ruta);
                // Ignorar literales y comentarios para evitar falsos positivos.
                codigo = codigo.replaceAll("(?s)\"\"\".*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|/\\*.*?\\*/|//[^\\r\\n]*", " ");
                var matcher = SIN_RELOJ.matcher(codigo);
                while (matcher.find()) infracciones.add(ruta + ": " + matcher.group());
                if (IMPORT_NOW.matcher(codigo).find()) infracciones.add(ruta + ": import estático de now");
                if (!ruta.getFileName().toString().equals("ConfiguracionReloj.java")
                        && Pattern.compile("\\bClock\\s*\\.\\s*system(?:UTC|DefaultZone)?\\s*\\(").matcher(codigo).find()) {
                    infracciones.add(ruta + ": reloj de sistema alternativo al bean compartido");
                }
            }
        }
        assertThat(infracciones).as("Usar el Clock inyectado para ahora").isEmpty();
    }

    @Test void surefireArrancaLaJvmEnUtc() {
        assertThat(java.time.ZoneId.systemDefault()).isEqualTo(java.time.ZoneId.of("UTC"));
    }
}
