package com.techprotech.agenda.compartido.correo;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ServicioIdentidadCorreoPlataformaTest {
    private ConfiguracionCorreoResolvida base(Long empresa) {
        return new ConfiguracionCorreoResolvida(empresa,true,ProveedorCorreo.SMTP,"legacy@example.test","Fluora",null,
                "smtp.example.test",465,"mailbox@example.test","fixture",true,false,null,null,null,null,null,null,null);
    }
    @Test void seleccionaIdentidadSinCambiarAutenticacion() {
        var servicio=new ServicioIdentidadCorreoPlataforma("automatic@example.test","info@example.test","contact@example.test");
        assertThat(servicio.resolver(base(null),PropositoCorreoPlataforma.TRANSACCIONAL).remitente()).isEqualTo("automatic@example.test");
        assertThat(servicio.resolver(base(null),PropositoCorreoPlataforma.INFORMATIVO).remitente()).isEqualTo("info@example.test");
        var atencion=servicio.resolver(base(null),PropositoCorreoPlataforma.ATENCION);
        assertThat(atencion.remitente()).isEqualTo("contact@example.test");
        assertThat(atencion.responderA()).isEqualTo("contact@example.test");
        assertThat(atencion.smtpUsername()).isEqualTo("mailbox@example.test");
    }
    @Test void conservaConfiguracionAnteriorSiNoHayAlias() {
        assertThat(new ServicioIdentidadCorreoPlataforma(null,"",null).resolver(base(null),PropositoCorreoPlataforma.INFORMATIVO)).isEqualTo(base(null));
    }
    @Test void impideAplicarIdentidadDeFluoraAUnTenant() {
        assertThatThrownBy(()->new ServicioIdentidadCorreoPlataforma(null,null,null).resolver(base(7L),PropositoCorreoPlataforma.TRANSACCIONAL)).isInstanceOf(IllegalArgumentException.class);
    }
}
