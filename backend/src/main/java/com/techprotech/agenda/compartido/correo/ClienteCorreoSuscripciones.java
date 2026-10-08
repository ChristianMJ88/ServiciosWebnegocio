package com.techprotech.agenda.compartido.correo;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.mail.MailProperties;

@Component
public class ClienteCorreoSuscripciones {
    private final ClienteCorreoSendgrid sendgrid;
    private final PropiedadesCorreoSuscripciones propiedades;
    public ClienteCorreoSuscripciones(ClienteCorreoSendgrid sendgrid,PropiedadesCorreoSuscripciones propiedades){this.sendgrid=sendgrid;this.propiedades=propiedades;}
    public ConfiguracionCorreoResolvida configuracion(){
        if(!"SMTP".equalsIgnoreCase(propiedades.proveedor()))return sendgrid.configuracionCanonica();
        boolean listo=presente(propiedades.remitente())&&presente(propiedades.smtpHost())&&presente(propiedades.smtpUsername())&&presente(propiedades.smtpPassword())
                &&propiedades.smtpPort()>0&&(propiedades.smtpSsl()||propiedades.smtpStartTls());
        return new ConfiguracionCorreoResolvida(null,listo,ProveedorCorreo.SMTP,propiedades.remitente(),propiedades.nombreRemitente(),propiedades.responderA(),
                propiedades.smtpHost(),propiedades.smtpPort(),propiedades.smtpUsername(),propiedades.smtpPassword(),true,propiedades.smtpStartTls(),null,null,null,null,null,null,null);
    }
    public void enviar(ConfiguracionCorreoResolvida config,MensajeCorreoSaliente mensaje){
        if(config.proveedor()==ProveedorCorreo.SENDGRID){sendgrid.enviar(config,mensaje);return;}
        var smtp=new MailProperties();
        smtp.getProperties().put("mail.smtp.ssl.enable",String.valueOf(propiedades.smtpSsl()));
        smtp.getProperties().put("mail.smtp.ssl.checkserveridentity","true");
        smtp.getProperties().put("mail.smtp.starttls.required",String.valueOf(propiedades.smtpStartTls()));
        smtp.getProperties().put("mail.smtp.connectiontimeout","10000");smtp.getProperties().put("mail.smtp.timeout","20000");smtp.getProperties().put("mail.smtp.writetimeout","20000");
        new ClienteCorreoSmtp(smtp).enviar(config,mensaje);
    }
    private boolean presente(String s){return s!=null&&!s.isBlank();}
}
