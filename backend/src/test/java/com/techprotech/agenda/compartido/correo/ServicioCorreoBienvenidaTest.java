package com.techprotech.agenda.compartido.correo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ServicioCorreoBienvenidaTest {
    @Test void bienvenidaUsaInformativoYRespuestasAlContacto() {
        var cliente=mock(ClienteCorreoSuscripciones.class);
        when(cliente.configuracion()).thenReturn(new ConfiguracionCorreoResolvida(null,true,ProveedorCorreo.SMTP,
                "old@example.test","Fluora",null,"smtp.example.test",465,"mailbox@example.test","fixture",true,false,null,null,null,null,null,null,null));
        var servicio=new ServicioCorreoBienvenida(cliente,new ServicioIdentidadCorreoPlataforma("automatic@example.test","info@example.test","contact@example.test"),
                "legacy-support@example.test","https://example.test/acceso","https://example.test");
        servicio.enviar(new BienvenidaEmpresaCorreo(9L,"Negocio","Ana","admin@example.test","negocio"));
        var config=ArgumentCaptor.forClass(ConfiguracionCorreoResolvida.class);
        var mensaje=ArgumentCaptor.forClass(MensajeCorreoSaliente.class);
        verify(cliente).enviar(config.capture(),mensaje.capture());
        assertThat(config.getValue().remitente()).isEqualTo("info@example.test");
        assertThat(mensaje.getValue().responderA()).isEqualTo("contact@example.test");
        assertThat(mensaje.getValue().destinatario()).isEqualTo("admin@example.test");
    }
}
