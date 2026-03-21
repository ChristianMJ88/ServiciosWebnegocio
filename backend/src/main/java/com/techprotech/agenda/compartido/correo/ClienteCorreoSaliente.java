package com.techprotech.agenda.compartido.correo;

public interface ClienteCorreoSaliente {

    void enviar(ConfiguracionCorreoResolvida configuracion, MensajeCorreoSaliente mensaje);
}
