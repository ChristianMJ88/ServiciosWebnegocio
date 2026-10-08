package com.techprotech.agenda.compartido.correo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServicioIdentidadCorreoPlataforma {
    private final String automatico;
    private final String informativo;
    private final String contacto;

    public ServicioIdentidadCorreoPlataforma(
            @Value("${aplicacion.correo-identidades.automatico:}") String automatico,
            @Value("${aplicacion.correo-identidades.informativo:}") String informativo,
            @Value("${aplicacion.correo-identidades.contacto:}") String contacto) {
        this.automatico = automatico;
        this.informativo = informativo;
        this.contacto = contacto;
    }

    public ConfiguracionCorreoResolvida resolver(ConfiguracionCorreoResolvida base, PropositoCorreoPlataforma proposito) {
        if (base.empresaId() != null) {
            throw new IllegalArgumentException("Las identidades de Fluora no aplican al correo de un tenant");
        }
        String remitente = switch (proposito) {
            case TRANSACCIONAL -> automatico;
            case INFORMATIVO -> informativo;
            case ATENCION -> contacto;
        };
        return new ConfiguracionCorreoResolvida(null, base.habilitado(), base.proveedor(),
                valor(remitente, base.remitente()), base.nombreRemitente(), valor(contacto, base.responderA()),
                base.smtpHost(), base.smtpPort(), base.smtpUsername(), base.smtpPassword(),
                base.smtpAuth(), base.smtpStartTls(), base.graphTenantId(), base.graphClientId(),
                base.graphClientSecret(), base.graphUserId(), base.graphCertificateThumbprint(),
                base.graphPrivateKeyPem(), base.graphOauthRefreshToken());
    }

    private String valor(String preferido, String anterior) {
        return preferido == null || preferido.isBlank() ? anterior : preferido.trim();
    }
}
