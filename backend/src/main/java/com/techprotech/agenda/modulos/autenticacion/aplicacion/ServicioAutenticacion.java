package com.techprotech.agenda.modulos.autenticacion.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.api.dto.IniciarSesionRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RegistrarClienteRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaTokenJwt;

public interface ServicioAutenticacion {

    RespuestaTokenJwt iniciarSesion(IniciarSesionRequest request);

    void registrarCliente(RegistrarClienteRequest request);
}

