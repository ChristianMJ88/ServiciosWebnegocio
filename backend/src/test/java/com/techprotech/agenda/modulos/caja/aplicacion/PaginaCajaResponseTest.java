package com.techprotech.agenda.modulos.caja.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.caja.api.dto.PaginaCajaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.HashSet;
import static org.assertj.core.api.Assertions.assertThat;

class PaginaCajaResponseTest {
    @Test void fijaContratoJsonSinCamposInternosDeSpring() throws Exception {
        var respuesta=PaginaCajaResponse.de(new PageImpl<>(List.of("venta"),PageRequest.of(1,20),41));
        var json=new ObjectMapper().valueToTree(respuesta);
        var claves=new HashSet<String>(); json.fieldNames().forEachRemaining(claves::add);
        assertThat(claves).containsExactlyInAnyOrder("content","pagina","tamano","totalElements","totalPages");
        assertThat(json.get("totalElements").asLong()).isEqualTo(41);
        assertThat(json.get("totalPages").asInt()).isEqualTo(3);
        assertThat(json.get("content").get(0).asText()).isEqualTo("venta");
    }
}
