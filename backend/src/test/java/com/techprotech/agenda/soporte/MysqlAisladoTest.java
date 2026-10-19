package com.techprotech.agenda.soporte;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
class MysqlAisladoTest {
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"agenda_db","fluora_test","fluoraXtest_base","fluora_test_x`;DROP DATABASE agenda_db"})
    void rechazaBaseHabitualONombreInseguro(String nombre) {
        assertThatThrownBy(() -> MysqlAislado.comprobarNombre(nombre)).isInstanceOf(IllegalStateException.class);
    }
    @ParameterizedTest @ValueSource(strings={"jdbc:mysql://localhost/?database=agenda_db", "jdbc:mysql://localhost/agenda_db", "jdbc:mysql://localhost", "jdbc:mysql://localhost/fluora_test_otro?databaseName=agenda_db", "jdbc:mysql://localhost/fluora_test_a?catalog=agenda_db"})
    void rechazaUrlSinBaseEnRuta(String url) {
        assertThatThrownBy(() -> MysqlAislado.urlHija(url,"fluora_test_hija")).isInstanceOf(IllegalStateException.class);
    }
    @org.junit.jupiter.api.Test void urlHijaConservaOpcionesYNoReutilizaBase() {
        assertThat(MysqlAislado.urlHija("jdbc:mysql://h:3306/fluora_test_a?useSSL=false","fluora_test_hija")).isEqualTo("jdbc:mysql://h:3306/fluora_test_hija?useSSL=false");
        assertThatThrownBy(() -> MysqlAislado.urlHija("jdbc:mysql://h/fluora_test_a","fluora_test_a")).isInstanceOf(IllegalStateException.class);
    }
    @ParameterizedTest @ValueSource(strings={"GRANT SELECT, SUPER, FILE, PROCESS ON *.* TO 't'@'%'", "GRANT ALL PRIVILEGES ON `agenda\\_db`.* TO 't'@'%'", "GRANT `rol`@`%` TO 't'@'%'", "GRANT ALL PRIVILEGES ON `fluora\\_test\\_%`.* TO 't'@'%' WITH GRANT OPTION"})
    void rechazaConcesionesAmplias(String grant) { assertThatThrownBy(() -> MysqlAislado.comprobarConcesion(grant)).isInstanceOf(IllegalStateException.class); }
    @ParameterizedTest @ValueSource(strings={"GRANT USAGE ON *.* TO 't'@'%'", "GRANT ALL PRIVILEGES ON `fluora\\_test\\_%`.* TO 't'@'%'"})
    void aceptaSoloConcesionesAcotadas(String grant) { assertThatCode(() -> MysqlAislado.comprobarConcesion(grant)).doesNotThrowAnyException(); }
    @ParameterizedTest @ValueSource(strings={"fluora_test_base","fluora_test_011_abc123"})
    void aceptaSolamenteElPatronAislado(String nombre) { assertThatCode(() -> MysqlAislado.comprobarNombre(nombre)).doesNotThrowAnyException(); }
}
