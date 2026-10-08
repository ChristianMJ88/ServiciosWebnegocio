package com.techprotech.agenda.compartido.correo;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="aplicacion.suscripciones.correo")
public record PropiedadesCorreoSuscripciones(String proveedor,String remitente,String nombreRemitente,String responderA,
        String smtpHost,int smtpPort,String smtpUsername,String smtpPassword,boolean smtpSsl,boolean smtpStartTls) {}
