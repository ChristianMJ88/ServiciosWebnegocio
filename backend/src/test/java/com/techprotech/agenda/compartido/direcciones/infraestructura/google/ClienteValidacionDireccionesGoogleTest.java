package com.techprotech.agenda.compartido.direcciones.infraestructura.google;

import com.techprotech.agenda.compartido.direcciones.dominio.Direccion;
import com.techprotech.agenda.compartido.direcciones.dominio.ResultadoValidacionDireccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClienteValidacionDireccionesGoogleTest {
    private MockRestServiceServer servidor;
    private ClienteValidacionDireccionesGoogle cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        PropiedadesGoogleMaps propiedades = new PropiedadesGoogleMaps(true, true, "browser-key", "server-key",
                "https://addressvalidation.googleapis.com", "MX", List.of("MX"));
        cliente = new ClienteValidacionDireccionesGoogle(builder, propiedades);
    }

    @Test
    void normalizaUnaDireccionCompletaSinExponerLaClaveEnLaUrl() {
        servidor.expect(requestTo("https://addressvalidation.googleapis.com/v1:validateAddress"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "server-key"))
                .andExpect(content().json("""
                        {"address":{"regionCode":"MX","addressLines":["Reforma 123","Centro","Puebla, Puebla, 72000"]}}
                        """, false))
                .andRespond(withSuccess("""
                        {"result":{"verdict":{"validationGranularity":"PREMISE","addressComplete":true},
                          "address":{"formattedAddress":"Av. Reforma 123, Centro, 72000 Puebla, Pue., México",
                            "postalAddress":{"regionCode":"MX","postalCode":"72000","administrativeArea":"Puebla","locality":"Puebla","sublocality":"Centro"},
                            "addressComponents":[
                              {"componentName":{"text":"Avenida Reforma"},"componentType":"route"},
                              {"componentName":{"text":"123"},"componentType":"street_number"}]},
                          "geocode":{"placeId":"place-123","location":{"latitude":19.0433,"longitude":-98.2019}}}}
                        """, MediaType.APPLICATION_JSON));

        ResultadoValidacionDireccion resultado = cliente.validar(direccion());

        assertThat(resultado.valida()).isTrue();
        assertThat(resultado.direccion().calle()).isEqualTo("Avenida Reforma");
        assertThat(resultado.direccion().codigoPais()).isEqualTo("MX");
        assertThat(resultado.placeId()).isEqualTo("place-123");
        servidor.verify();
    }

    @Test
    void rechazaUnResultadoQueSoloUbicaLaCalle() {
        servidor.expect(requestTo("https://addressvalidation.googleapis.com/v1:validateAddress"))
                .andRespond(withSuccess("""
                        {"result":{"verdict":{"validationGranularity":"ROUTE","addressComplete":false},
                          "address":{"formattedAddress":"Avenida Reforma, Puebla, México"}}}
                        """, MediaType.APPLICATION_JSON));

        ResultadoValidacionDireccion resultado = cliente.validar(direccion());

        assertThat(resultado.valida()).isFalse();
        assertThat(resultado.estadoValidacion()).isEqualTo("INCOMPLETA");
        assertThat(resultado.mensaje()).contains("calle y número");
    }

    private Direccion direccion() {
        return new Direccion("Reforma", "123", null, "Centro", "Puebla", "Puebla", "72000", "MX");
    }
}
