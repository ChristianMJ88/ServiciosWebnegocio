package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.SuscripcionPlataformaRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service
public class ProcesadorVencimientosSuscripcion {
    private final SuscripcionPlataformaRepositorio suscripciones;
    private final ServicioAvisosSuscripcion avisos;
    private final int lote;
    public ProcesadorVencimientosSuscripcion(SuscripcionPlataformaRepositorio suscripciones,ServicioAvisosSuscripcion avisos,
            @Value("${aplicacion.suscripciones.correo.lote:100}") int lote) {
        this.suscripciones=suscripciones;this.avisos=avisos;this.lote=lote;
    }
    @Scheduled(fixedDelayString="${aplicacion.suscripciones.correo.vencimientos-delay-ms:60000}")
    @Transactional
    public void procesar() {
        var ahora=LocalDateTime.now(ZoneOffset.UTC);
        for(var s:suscripciones.findVencidasSinAvisar(ahora,PageRequest.of(0,lote))) {
            avisos.programar(s,"VENCIMIENTO",s.getEstado()+":"+s.getPruebaHasta()+":"+s.getGraciaHasta()+":"+s.getPeriodoActualHasta(),null);
            s.setAvisoVencimientoEn(ahora);suscripciones.save(s);
        }
    }
}
