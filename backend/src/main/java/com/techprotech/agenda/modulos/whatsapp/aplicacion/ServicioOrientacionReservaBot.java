package com.techprotech.agenda.modulos.whatsapp.aplicacion;

import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitas;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioPoliticaReservaPago;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

/** No envía mensajes: resuelve la orientación usando únicamente datos del tenant. */
@Service
public class ServicioOrientacionReservaBot {
    private final ServicioCitas citas;
    private final ServicioPoliticaReservaPago pagos;
    private final EmpresaSitioConfigRepositorio sitios;
    private final String origen;
    public ServicioOrientacionReservaBot(ServicioCitas citas, ServicioPoliticaReservaPago pagos,
            EmpresaSitioConfigRepositorio sitios, @Value("${aplicacion.correo-bienvenida.origen-publico}") String origen) {
        this.citas=citas;this.pagos=pagos;this.sitios=sitios;this.origen=origen;
    }
    public boolean requiereAnticipo(Long empresa,Long sucursal,Long servicio) {
        return citas.requierePagoBot(empresa,sucursal,servicio,null);
    }
    public String mensaje(Long empresa,Long sucursal,Long servicio) {
        var sitio=sitios.findById(empresa).filter(s -> s.isPublicado() && s.getSlug()!=null).orElse(null);
        if (sitio==null || !pagos.puedeCobrarOnline(empresa))
            return "Este servicio requiere anticipo; contacta al negocio para reservarlo";
        String base=sitio.getDominioPrincipal()==null || sitio.getDominioPrincipal().isBlank()
                ? origen.replaceAll("/+$","")+"/e/"+sitio.getSlug()
                : "https://"+sitio.getDominioPrincipal().replaceAll("^https?://","").replaceAll("/+$","");
        String enlace=UriComponentsBuilder.fromUriString(base).path("/agendar")
                .queryParam("sucursal",sucursal).queryParam("servicios",servicio).build().encode().toUriString();
        return "Este servicio requiere un anticipo en línea. Resérvalo y págalo aquí: "+enlace;
    }
}
