package com.techprotech.agenda.compartido.correo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import java.util.List;

@Service
public class ServicioCorreoSuscripciones {
    private final ClienteCorreoSuscripciones cliente;
    private final String urlAcceso;
    private final String remitente;
    public ServicioCorreoSuscripciones(ClienteCorreoSuscripciones cliente,
            @Value("${aplicacion.correo-bienvenida.url-acceso}") String urlAcceso,
            @Value("${aplicacion.suscripciones.correo.remitente:}") String remitente) {
        this.cliente=cliente;this.urlAcceso=urlAcceso;this.remitente=remitente;
    }
    public void enviar(SuscripcionCorreoPayload p) {
        var config=cliente.configuracion();
        if(!config.habilitado())throw new IllegalStateException("El remitente de Fluora no está configurado");
        if(config.proveedor()==ProveedorCorreo.SENDGRID&&remitente!=null&&!remitente.isBlank())config=new ConfiguracionCorreoResolvida(null,true,ProveedorCorreo.SENDGRID,
                remitente,config.nombreRemitente(),config.responderA(),null,0,null,null,false,false,null,null,null,null,null,null,null);
        String detalle=p.descripcion()+"\nNegocio: "+p.negocio()+"\nPlan: "+p.plan()
                +(p.fecha()==null?"":"\nFecha: "+p.fecha())
                +(p.importe()==null?"":"\nImporte pagado: "+p.importe().setScale(2)+" "+p.moneda())
                +(p.referencia()==null?"":"\nReferencia: "+p.referencia());
        String texto=p.titulo()+"\n\n"+detalle+"\n\nAdministra tu suscripción: "+urlAcceso;
        String html="""
                <!doctype html><html lang="es"><meta name="viewport" content="width=device-width,initial-scale=1">
                <body style="margin:0;background:#f3f4f8;font-family:Arial,sans-serif;color:#17213b">
                <table role="presentation" width="100%%" style="padding:24px 12px"><tr><td align="center">
                <table role="presentation" width="100%%" style="max-width:600px;background:white;border-radius:18px">
                <tr><td style="padding:28px;background:#302568;color:white;border-radius:18px 18px 0 0">
                <strong>%s</strong><h1 style="font-size:24px;line-height:1.3">%s</h1></td></tr>
                <tr><td style="padding:28px;line-height:1.7"><p>%s</p>
                <a href="%s" style="display:inline-block;background:#302568;color:white;padding:12px 18px;border-radius:10px;text-decoration:none">Administrar suscripción</a>
                </td></tr></table></td></tr></table></body></html>
                """.formatted(esc(config.nombreRemitente()),esc(p.titulo()),esc(detalle).replace("\n","<br>"),esc(urlAcceso));
        cliente.enviar(config,new MensajeCorreoSaliente(p.destinatario(),p.titulo()+" | "+p.negocio(),texto,html,config.responderA(),List.of()));
    }
    private String esc(String valor){return HtmlUtils.htmlEscape(valor==null?"":valor);}
}
