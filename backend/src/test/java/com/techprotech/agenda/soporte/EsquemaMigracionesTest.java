package com.techprotech.agenda.soporte;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EsquemaMigracionesTest {
    private EsquemaMigraciones.Columna column(String type, String full) {
        return new EsquemaMigraciones.Columna("fixture", "valor", type, full, "YES", 36L, "utf8mb4", "utf8mb4_0900_ai_ci", null,null,null,null,"");
    }
    @Test void charNoEsVarcharNiUnaColumnaAusente() {
        assertThat(EsquemaMigraciones.equivalentes(column("char", "char(36)"), column("varchar", "varchar(36)"))).isFalse();
        assertThat(EsquemaMigraciones.equivalentes(column("varchar", "varchar(36)"), column("char", "char(36)"))).isFalse();
        assertThat(EsquemaMigraciones.equivalentes(null, column("varchar", "varchar(36)"))).isFalse();
        assertThat(EsquemaMigraciones.equivalentes(column("date", "date"), column("datetime", "datetime(6)"))).isFalse();
        assertThat(EsquemaMigraciones.equivalentes(column("bit", "bit(1)"), column("tinyint", "tinyint"))).isFalse();
        assertThat(EsquemaMigraciones.equivalentes(column("tinyint", "tinyint"), column("int", "int"))).isFalse();
    }
    @Test void equivalenciasSonSimetricas() {
        for (String a:java.util.List.of("char","varchar","enum","text","int","bigint","datetime","timestamp","date"))
            for (String b:java.util.List.of("char","varchar","enum","text","int","bigint","datetime","timestamp","date"))
                assertThat(EsquemaMigraciones.equivalentes(column(a,a),column(b,b)))
                        .isEqualTo(EsquemaMigraciones.equivalentes(column(b,b),column(a,a)));
    }
    @Test void equivalenciasExplicitasDeMysqlConservanLaValidacionFinalDeHibernate() {
        assertThat(EsquemaMigraciones.equivalentes(column("bit", "bit(1)"), column("tinyint", "tinyint(1)"))).isTrue();
        assertThat(EsquemaMigraciones.equivalentes(column("timestamp", "timestamp"), column("datetime", "datetime(6)"))).isTrue();
        assertThat(EsquemaMigraciones.equivalentes(column("enum", "enum('A')"), column("varchar", "varchar(30)"))).isTrue();
    }
}
