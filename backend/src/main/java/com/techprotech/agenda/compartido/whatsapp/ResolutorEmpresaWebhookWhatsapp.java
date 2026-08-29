package com.techprotech.agenda.compartido.whatsapp;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class ResolutorEmpresaWebhookWhatsapp {

    private final ConfiguracionWhatsappEmpresaRepositorio repositorio;

    public ResolutorEmpresaWebhookWhatsapp(ConfiguracionWhatsappEmpresaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public ConfiguracionWhatsappEmpresaEntidad resolver(MultiValueMap<String, String> parametros) {
        Set<Long> empresas = new LinkedHashSet<>();
        String accountSid = texto(parametros.getFirst("AccountSid"));
        if (accountSid != null) {
            agregar(empresas, repositorio.findBySubaccountSid(accountSid));
            agregar(empresas, repositorio.findByAccountSid(accountSid));
        }
        String messagingServiceSid = texto(parametros.getFirst("MessagingServiceSid"));
        if (messagingServiceSid != null) {
            agregar(empresas, repositorio.findByMessagingServiceSid(messagingServiceSid));
        }

        String destino = normalizarDireccion(parametros.getFirst("To"));
        if (destino != null) {
            agregar(empresas, repositorio.findByNumeroRemitenteIn(Set.of(destino, "whatsapp:" + destino)));
            agregar(empresas, repositorio.findBySenderPhoneNumberIn(Set.of(destino, "whatsapp:" + destino)));
        }

        if (empresas.size() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    empresas.isEmpty()
                            ? "No se pudo identificar el tenant del webhook de WhatsApp"
                            : "El webhook de WhatsApp coincide con mas de un tenant"
            );
        }

        return repositorio.findById(empresas.iterator().next())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant de WhatsApp no disponible"));
    }

    private void agregar(Set<Long> empresas, Iterable<ConfiguracionWhatsappEmpresaEntidad> configuraciones) {
        if (configuraciones == null) return;
        for (ConfiguracionWhatsappEmpresaEntidad configuracion : configuraciones) {
            if (configuracion != null && configuracion.isHabilitado()) {
                empresas.add(configuracion.getEmpresaId());
            }
        }
    }

    private String normalizarDireccion(String valor) {
        String limpio = texto(valor);
        if (limpio == null) return null;
        return limpio.regionMatches(true, 0, "whatsapp:", 0, 9) ? limpio.substring(9) : limpio;
    }

    private String texto(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
