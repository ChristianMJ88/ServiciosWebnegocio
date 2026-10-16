package com.techprotech.agenda.compartido.tiempo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.techprotech.agenda.modulos.caja.api.dto.*;
import com.techprotech.agenda.modulos.recepcion.api.dto.*;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.SolicitudHorarioResponse;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;

@Isolated("Contrato JSON independiente de la zona de la JVM")
class MomentosApiTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-15T14:00:00Z"), ZoneOffset.UTC);

    static Stream<Class<?>> respuestas() {
        return Stream.of(PagoCitaResponse.class, MovimientoCajaResponse.class, CajaSesionResponse.class,
                VentaResponse.class, CitaRecepcionResponse.class, SolicitudEsperaRecepcionResponse.class,
                SolicitudHorarioResponse.class);
    }

    @ParameterizedTest @MethodSource("respuestas")
    void momentosIncluyenZSinModificarHorasDeAgendaConJvmMexico(Class<?> tipo) throws Exception {
        var anterior = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
            var componentes = tipo.getRecordComponents();
            var tipos = Arrays.stream(componentes).map(c -> c.getType()).toArray(Class<?>[]::new);
            var valores = new Object[componentes.length];
            for (int i = 0; i < componentes.length; i++) {
                var campo = componentes[i];
                if (campo.getType() == boolean.class) valores[i] = false;
                if (campo.getType() == OffsetDateTime.class) {
                    valores[i] = Set.of("inicio", "fin").contains(campo.getName())
                            ? OffsetDateTime.parse("2026-10-15T10:00:00-06:00")
                            : MomentoUtc.de(LocalDateTime.now(reloj));
                }
                if (campo.getType() == LocalDate.class) valores[i] = LocalDate.of(2026, 10, 15);
                if (campo.getType() == LocalTime.class) valores[i] = LocalTime.of(9, 0);
            }
            var respuesta = tipo.getDeclaredConstructor(tipos).newInstance(valores);
            var arbol = json.readTree(json.writeValueAsString(respuesta));
            for (var campo : componentes) {
                if (campo.getType() != OffsetDateTime.class) continue;
                var valor = arbol.get(campo.getName()).asText();
                if (Set.of("inicio", "fin").contains(campo.getName())) {
                    assertThat(valor).isEqualTo("2026-10-15T10:00:00-06:00");
                } else {
                    assertThat(valor).isEqualTo("2026-10-15T14:00:00Z");
                    assertThat(OffsetDateTime.parse(valor).toInstant()).isEqualTo(reloj.instant());
                    assertThat(OffsetDateTime.parse(valor).atZoneSameInstant(ZoneId.of("America/Mexico_City")).getHour()).isEqualTo(8);
                }
            }
            if (tipo == SolicitudEsperaRecepcionResponse.class) {
                assertThat(arbol.get("fechaDeseada").asText()).isEqualTo("2026-10-15");
                assertThat(arbol.get("horaDesde").asText()).isEqualTo("09:00:00");
            }
        } finally { TimeZone.setDefault(anterior); }
    }

    @Test void momentosOpcionalesConservanNull() throws Exception {
        assertThat(MomentoUtc.de(null)).isNull();
        var pago = new PagoCitaResponse(1L, 2L, null, null, null, null, null, null);
        assertThat(json.readTree(json.writeValueAsString(pago)).get("registradoEn").isNull()).isTrue();
    }
}
