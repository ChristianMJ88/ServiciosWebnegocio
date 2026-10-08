package com.techprotech.agenda.compartido.correo;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ClienteCorreoSuscripcionesTest {
    @Test void smtpExigeCredencialesYTransporteCifrado(){
        var sinClave=new PropiedadesCorreoSuscripciones("SMTP","sender@example.test","Fluora",null,"smtp.example.test",465,"mailbox@example.test",null,true,false);
        var sinTls=new PropiedadesCorreoSuscripciones("SMTP","sender@example.test","Fluora",null,"smtp.example.test",25,"mailbox@example.test","fixture",false,false);
        assertThat(new ClienteCorreoSuscripciones(mock(ClienteCorreoSendgrid.class),sinClave).configuracion().habilitado()).isFalse();
        assertThat(new ClienteCorreoSuscripciones(mock(ClienteCorreoSendgrid.class),sinTls).configuracion().habilitado()).isFalse();
    }
    @Test void aliasDeSalidaSeMantieneSeparadoDeLaIdentidadDeAutenticacion(){
        var p=new PropiedadesCorreoSuscripciones("SMTP","alias@example.test","Fluora",null,"smtp.example.test",465,"mailbox@example.test","fixture",true,false);
        var config=new ClienteCorreoSuscripciones(mock(ClienteCorreoSendgrid.class),p).configuracion();
        assertThat(config.habilitado()).isTrue();assertThat(config.remitente()).isEqualTo("alias@example.test");
        assertThat(config.smtpUsername()).isEqualTo("mailbox@example.test");
    }
}
