package com.techprotech.agenda.modulos.plataforma.aplicacion;

import java.time.LocalDateTime;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.SuscripcionPlataformaEntidad;

public final class PoliticaVigenciaSuscripcion {
    private PoliticaVigenciaSuscripcion() {}
    public static boolean estaVigente(SuscripcionPlataformaEntidad suscripcion,LocalDateTime ahora) {
        if(suscripcion.getTerminaEn()!=null&&!suscripcion.getTerminaEn().isAfter(ahora))return false;
        if(suscripcion.isCancelarAlFinalPeriodo()&&suscripcion.getPeriodoActualHasta()!=null
                &&!suscripcion.getPeriodoActualHasta().isAfter(ahora))return false;
        if("GRACIA".equals(suscripcion.getEstado()))
            return suscripcion.getGraciaHasta()!=null&&suscripcion.getGraciaHasta().isAfter(ahora);
        if("ACTIVA".equals(suscripcion.getEstado())&&suscripcion.getStripeSuscripcionId()!=null)
            return suscripcion.getPeriodoActualHasta()!=null&&suscripcion.getPeriodoActualHasta().isAfter(ahora);
        return !"PRUEBA".equals(suscripcion.getEstado())
                ||(suscripcion.getPruebaHasta()!=null&&suscripcion.getPruebaHasta().isAfter(ahora));
    }
}
