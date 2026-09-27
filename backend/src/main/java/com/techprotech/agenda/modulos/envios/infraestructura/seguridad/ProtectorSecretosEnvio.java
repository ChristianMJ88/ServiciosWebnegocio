package com.techprotech.agenda.modulos.envios.infraestructura.seguridad;

import com.techprotech.agenda.compartido.seguridad.CifradorSecretosAesGcm;
import com.techprotech.agenda.modulos.pagos.infraestructura.seguridad.PropiedadesSeguridadPagos;
import org.springframework.stereotype.Service;

@Service
public class ProtectorSecretosEnvio {
    private final PropiedadesSeguridadPagos propiedades;

    public ProtectorSecretosEnvio(PropiedadesSeguridadPagos propiedades) {
        this.propiedades = propiedades;
    }

    public String encriptar(String valor) {
        return CifradorSecretosAesGcm.encriptar(valor, propiedades.llaveCifradoSecretos(), "PAGOS_LLAVE_CIFRADO_SECRETOS");
    }

    public String desencriptar(String valor) {
        return CifradorSecretosAesGcm.desencriptar(valor, propiedades.llaveCifradoSecretos(), "PAGOS_LLAVE_CIFRADO_SECRETOS");
    }
}
