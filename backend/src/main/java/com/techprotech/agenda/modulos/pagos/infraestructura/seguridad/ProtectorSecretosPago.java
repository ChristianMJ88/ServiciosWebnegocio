package com.techprotech.agenda.modulos.pagos.infraestructura.seguridad;

import org.springframework.stereotype.Service;

import com.techprotech.agenda.compartido.seguridad.CifradorSecretosAesGcm;

@Service
public class ProtectorSecretosPago {
    private final PropiedadesSeguridadPagos propiedades;

    public ProtectorSecretosPago(PropiedadesSeguridadPagos propiedades) {
        this.propiedades = propiedades;
    }

    public String encriptar(String valor) {
        return CifradorSecretosAesGcm.encriptar(valor, propiedades.llaveCifradoSecretos(), "PAGOS_LLAVE_CIFRADO_SECRETOS");
    }

    public String desencriptar(String valor) {
        return CifradorSecretosAesGcm.desencriptar(valor, propiedades.llaveCifradoSecretos(), "PAGOS_LLAVE_CIFRADO_SECRETOS");
    }
}
