package com.techprotech.agenda.compartido.correo;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class ServicioCorreoSuscripcionesTest {
    @Test void escapaContenidoYUsaRemitenteConfiguradoSinCambiarElCorreoDeOtrosModulos(){
        var cliente=mock(ClienteCorreoSuscripciones.class);
        var config=new ConfiguracionCorreoResolvida(null,true,ProveedorCorreo.SENDGRID,"default@example.test","Fluora","support@example.test",null,0,null,null,false,false,null,null,null,null,null,null,null);
        when(cliente.configuracion()).thenReturn(config);
        var servicio=new ServicioCorreoSuscripciones(cliente,"https://example.test/acceso","sales@example.test");
        servicio.enviar(new SuscripcionCorreoPayload("billing@example.test","<script>negocio</script>","Plan","Prueba","<img src=x>",null,null,"MXN",null));
        var mensaje=ArgumentCaptor.forClass(MensajeCorreoSaliente.class);
        var enviado=ArgumentCaptor.forClass(ConfiguracionCorreoResolvida.class);
        verify(cliente).enviar(enviado.capture(),mensaje.capture());
        assertThat(enviado.getValue().remitente()).isEqualTo("sales@example.test");
        assertThat(mensaje.getValue().contenidoHtml()).doesNotContain("<script>","<img src=x>").contains("&lt;img");
        assertThat(config.remitente()).isEqualTo("default@example.test");
    }
}
