package com.techprotech.agenda.compartido.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ConfiguracionWhatsappEmpresaRepositorio extends JpaRepository<ConfiguracionWhatsappEmpresaEntidad, Long> {
    List<ConfiguracionWhatsappEmpresaEntidad> findByHabilitadoTrueOrderByEmpresaIdAsc();
    List<ConfiguracionWhatsappEmpresaEntidad> findBySubaccountSid(String subaccountSid);
    List<ConfiguracionWhatsappEmpresaEntidad> findByAccountSid(String accountSid);
    List<ConfiguracionWhatsappEmpresaEntidad> findByMessagingServiceSid(String messagingServiceSid);
    List<ConfiguracionWhatsappEmpresaEntidad> findByNumeroRemitenteIn(Collection<String> numeros);
    List<ConfiguracionWhatsappEmpresaEntidad> findBySenderPhoneNumberIn(Collection<String> numeros);
}
