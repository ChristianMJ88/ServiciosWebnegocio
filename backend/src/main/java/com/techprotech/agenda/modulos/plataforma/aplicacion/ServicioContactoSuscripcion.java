package com.techprotech.agenda.modulos.plataforma.aplicacion;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.SuscripcionPlataformaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Locale;

@Service
public class ServicioContactoSuscripcion {
    private final SuscripcionPlataformaRepositorio suscripciones;
    public ServicioContactoSuscripcion(SuscripcionPlataformaRepositorio suscripciones){this.suscripciones=suscripciones;}
    @Transactional(readOnly=true)
    public String obtener(Long empresaId){return suscripciones.findFirstByEmpresaIdOrderByCreadaEnDesc(empresaId).map(s->s.getCorreoFacturacion()).orElse(null);}
    @Transactional
    public void guardar(Long empresaId,String correo){
        var s=suscripciones.findFirstByEmpresaIdOrderByCreadaEnDesc(empresaId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"El negocio no tiene una suscripción"));
        s.setCorreoFacturacion(correo.trim().toLowerCase(Locale.ROOT));suscripciones.save(s);
    }
}
