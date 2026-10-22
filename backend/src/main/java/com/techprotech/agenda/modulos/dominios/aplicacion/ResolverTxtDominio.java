package com.techprotech.agenda.modulos.dominios.aplicacion;
import java.util.List;
public interface ResolverTxtDominio {
    List<String> consultar(String hostname) throws Exception;
}
