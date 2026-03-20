package com.techprotech.agenda.modulos.autenticacion.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.api.dto.IniciarSesionRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RegistrarClienteRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaTokenJwt;
import com.techprotech.agenda.seguridad.jwt.ServicioTokenJwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioAutenticacionImpl implements ServicioAutenticacion {

    private final ServicioTokenJwt servicioTokenJwt;

    public ServicioAutenticacionImpl(ServicioTokenJwt servicioTokenJwt) {
        this.servicioTokenJwt = servicioTokenJwt;
    }

    @Override
    public RespuestaTokenJwt iniciarSesion(IniciarSesionRequest request) {
        throw new UnsupportedOperationException(
                "La autenticacion real con base de datos aun no esta implementada. " +
                "El siguiente paso es crear repositorios de usuario, roles y refresh tokens."
        );
    }

    @Override
    public void registrarCliente(RegistrarClienteRequest request) {
        throw new UnsupportedOperationException(
                "El registro de clientes aun no esta implementado. " +
                "El siguiente paso es persistir usuario, cliente y rol CLIENTE."
        );
    }

    @SuppressWarnings("unused")
    private RespuestaTokenJwt construirRespuestaDemo(Long empresaId) {
        List<String> roles = List.of("CLIENTE");
        return new RespuestaTokenJwt(
                servicioTokenJwt.generarTokenAcceso("demo@agenda.local", 1L, empresaId, roles),
                servicioTokenJwt.generarTokenActualizacion("demo@agenda.local", 1L, empresaId),
                "Bearer",
                1L,
                empresaId,
                roles
        );
    }
}

