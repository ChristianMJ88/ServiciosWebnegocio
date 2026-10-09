package com.techprotech.agenda.modulos.plataforma.infraestructura.stripe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Subscription;
import com.stripe.model.Invoice;
import com.stripe.net.RequestOptions;
import org.springframework.stereotype.Component;

@Component
public class ConsultaSuscripcionStripeApi implements ConsultaSuscripcionStripe {
    private final PropiedadesStripePlataforma propiedades;
    private final ObjectMapper mapper;
    public ConsultaSuscripcionStripeApi(PropiedadesStripePlataforma propiedades,ObjectMapper mapper) {
        this.propiedades=propiedades;this.mapper=mapper;
    }
    @Override public Resultado consultar(String id) throws Exception {
        if(!propiedades.habilitado()||propiedades.secretKey()==null||propiedades.secretKey().isBlank())
            throw new IllegalStateException("Stripe no está configurado");
        var opciones=RequestOptions.builder().setApiKey(propiedades.secretKey()).setConnectTimeout(5000).setReadTimeout(10000).build();
        var s=Subscription.retrieve(id,opciones);
        var factura="active".equals(s.getStatus())&&s.getLatestInvoice()!=null
                ? mapper.readTree(Invoice.retrieve(s.getLatestInvoice(),opciones).toJson()) : null;
        return new Resultado(mapper.readTree(s.toJson()),factura);
    }
}
