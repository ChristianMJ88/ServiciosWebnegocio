package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.SuscripcionPlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.stripe.ConsultaSuscripcionStripe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class ProcesadorConciliacionSuscripciones {
    private static final Logger log=LoggerFactory.getLogger(ProcesadorConciliacionSuscripciones.class);
    private final SuscripcionPlataformaRepositorio suscripciones;
    private final ConsultaSuscripcionStripe consulta;
    private final ServicioStripePlataforma servicio;
    private final boolean habilitado;
    private final int lote;
    private long cursor;
    public ProcesadorConciliacionSuscripciones(SuscripcionPlataformaRepositorio suscripciones,
            ConsultaSuscripcionStripe consulta,ServicioStripePlataforma servicio,
            @Value("${aplicacion.plataforma.stripe.conciliacion.habilitado:true}") boolean habilitado,
            @Value("${aplicacion.plataforma.stripe.conciliacion.lote:20}") int lote) {
        this.suscripciones=suscripciones;this.consulta=consulta;this.servicio=servicio;this.habilitado=habilitado;this.lote=lote;
    }
    @Scheduled(fixedDelayString="${aplicacion.plataforma.stripe.conciliacion.delay-ms:300000}",initialDelayString="${aplicacion.plataforma.stripe.conciliacion.delay-ms:300000}")
    public void procesar() {
        if(!habilitado)return;
        var ahora=LocalDateTime.now(ZoneOffset.UTC);
        var pagina=PageRequest.of(0,Math.max(1,lote));
        var pendientes=suscripciones.findParaConciliar(ahora,cursor,pagina);
        if(pendientes.isEmpty()&&cursor>0){cursor=0;pendientes=suscripciones.findParaConciliar(ahora,cursor,pagina);}
        for(var s:pendientes) {
            cursor=s.getId();
            try { servicio.conciliarSuscripcion(s.getId(),consulta.consultar(s.getStripeSuscripcionId())); }
            catch(Exception ex) { log.warn("No se pudo conciliar la suscripción {} ({}). Se mantiene la política local de acceso.",s.getId(),ex.getClass().getSimpleName()); }
        }
    }
}
