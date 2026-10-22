package com.techprotech.agenda.modulos.dominios.aplicacion;
public interface ComprobadorHostingDominio {
    boolean comprobar(String hostname, String sitioHosting) throws Exception;
}
