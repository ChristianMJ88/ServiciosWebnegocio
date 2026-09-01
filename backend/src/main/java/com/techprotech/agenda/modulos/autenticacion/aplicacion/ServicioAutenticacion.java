package com.techprotech.agenda.modulos.autenticacion.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.api.dto.IniciarSesionAppRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.IniciarSesionRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RefrescarTokenRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RegistrarClienteRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaAccesoApp;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaTokenJwt;

public interface ServicioAutenticacion {

    RespuestaTokenJwt iniciarSesion(IniciarSesionRequest request);

    RespuestaAccesoApp iniciarSesionApp(IniciarSesionAppRequest request);

    RespuestaTokenJwt refrescarToken(RefrescarTokenRequest request);

    void cerrarSesion(RefrescarTokenRequest request);

    void registrarCliente(RegistrarClienteRequest request);

    RespuestaTokenJwt emitirSesionOnboarding(Long empresaId, Long usuarioId);
}
