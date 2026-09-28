package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EmpresaEnvioConfigEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.envia.PropiedadesEnvia;
import com.techprotech.agenda.modulos.envios.infraestructura.seguridad.ProtectorSecretosEnvio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ResolutorCredencialEnvio {
    private final ProtectorSecretosEnvio secretos;
    private final PropiedadesEnvia propiedades;

    public ResolutorCredencialEnvio(ProtectorSecretosEnvio secretos, PropiedadesEnvia propiedades) {
        this.secretos = secretos;
        this.propiedades = propiedades;
    }

    public String resolver(EmpresaEnvioConfigEntidad config) {
        String token;
        if ("PLATAFORMA".equals(config.getModoCredencial())) {
            token = "PRODUCCION".equals(config.getAmbiente())
                    ? propiedades.tokenPlataformaProduccion() : propiedades.tokenPlataformaPruebas();
        } else {
            token = config.getTokenCifrado() == null ? null : secretos.desencriptar(config.getTokenCifrado());
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La credencial de envío no está disponible para este negocio");
        }
        String valor = token.trim();
        return valor.regionMatches(true, 0, "Bearer ", 0, 7) ? valor.substring(7).trim() : valor;
    }
}
