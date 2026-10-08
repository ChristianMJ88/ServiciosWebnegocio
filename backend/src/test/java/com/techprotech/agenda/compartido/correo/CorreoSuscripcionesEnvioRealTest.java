package com.techprotech.agenda.compartido.correo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import java.util.List;

/** Envío externo explícito; permanece desactivado en CI y en la suite normal. */
@EnabledIfEnvironmentVariable(named="FLUORA_CORREO_PRUEBA_DESTINATARIO",matches=".+")
class CorreoSuscripcionesEnvioRealTest {
    @Test void enviaEjemplosIdentificadosComoPruebaAlDestinatarioAutorizado(){
        String destino=System.getenv("FLUORA_CORREO_PRUEBA_DESTINATARIO");
        String remitente=System.getenv("SUSCRIPCIONES_CORREO_REMITENTE");
        var propiedades=new PropiedadesCorreoPlataforma(true,System.getenv("SENDGRID_API_KEY"),remitente,
                System.getenv().getOrDefault("CORREO_PLATAFORMA_NOMBRE_REMITENTE","Fluora"),System.getenv("CORREO_PLATAFORMA_RESPONDER_A"));
        var smtp=new PropiedadesCorreoSuscripciones(System.getenv().getOrDefault("SUSCRIPCIONES_CORREO_PROVEEDOR","SENDGRID"),remitente,"Fluora",null,
                System.getenv("SUSCRIPCIONES_CORREO_SMTP_HOST"),Integer.parseInt(System.getenv().getOrDefault("SUSCRIPCIONES_CORREO_SMTP_PORT","465")),
                System.getenv("SUSCRIPCIONES_CORREO_SMTP_USERNAME"),System.getenv("SUSCRIPCIONES_CORREO_SMTP_PASSWORD"),
                Boolean.parseBoolean(System.getenv().getOrDefault("SUSCRIPCIONES_CORREO_SMTP_SSL","true")),Boolean.parseBoolean(System.getenv().getOrDefault("SUSCRIPCIONES_CORREO_SMTP_START_TLS","false")));
        var servicio=new ServicioCorreoSuscripciones(new ClienteCorreoSuscripciones(new ClienteCorreoSendgrid(RestClient.builder(),propiedades),smtp),
                System.getenv().getOrDefault("CORREO_BIENVENIDA_URL_ACCESO","http://localhost:4200/admin/suscripcion"),remitente);
        for(String evento:List.of("Inicio de prueba","Pago confirmado","Cambio de plan","Pago fallido","Cancelación programada","Cancelación confirmada","Vencimiento")) {
            servicio.enviar(new SuscripcionCorreoPayload(destino,"Demostración de Fluora","Ejemplo de plan",
                    "PRUEBA · "+evento,"Este correo comprueba el diseño y el envío de avisos. No confirma un cobro, una contratación ni un cambio real de tu cuenta.",
                    null,null,null,null));
        }
    }
}
