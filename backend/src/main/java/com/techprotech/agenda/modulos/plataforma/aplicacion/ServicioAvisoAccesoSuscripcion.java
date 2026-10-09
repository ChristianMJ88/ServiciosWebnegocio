package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.plataforma.api.dto.AvisoAccesoSuscripcionResponse;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.SuscripcionPlataformaRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioAvisoAccesoSuscripcion {
    private final ServicioCapacidadesEmpresa capacidades;
    private final SuscripcionPlataformaRepositorio suscripciones;
    public ServicioAvisoAccesoSuscripcion(ServicioCapacidadesEmpresa capacidades, SuscripcionPlataformaRepositorio suscripciones) {
        this.capacidades=capacidades;this.suscripciones=suscripciones;
    }
    @Transactional(readOnly=true)
    public AvisoAccesoSuscripcionResponse obtener(UsuarioAutenticado usuario) {
        var acceso=capacidades.obtener(usuario.empresaId());
        boolean gestionar=usuario.tienePermiso("SUSCRIPCION_EMPRESA_GESTIONAR");
        if(!acceso.suscripcionVigente())return new AvisoAccesoSuscripcionResponse(true,false,
                "Regulariza la suscripción", gestionar
                ? "El negocio no tiene una suscripción vigente. Puedes revisar tu plan y regularizar el pago para continuar. Tus datos se conservan."
                : "El negocio no tiene una suscripción vigente. Contacta al administrador para regularizar el pago. Tus datos se conservan.",null,gestionar);
        if("GRACIA".equals(acceso.estado()))return new AvisoAccesoSuscripcionResponse(false,true,
                "Hay un pago pendiente",gestionar?"Regulariza el pago antes de que termine el periodo de gracia para evitar la suspensión."
                :"Contacta al administrador: hay un pago pendiente y el acceso terminará al vencer la gracia.",
                suscripciones.findById(acceso.suscripcionId()).map(s->s.getGraciaHasta()).orElse(null),gestionar);
        return new AvisoAccesoSuscripcionResponse(false,false,null,null,null,gestionar);
    }
}
