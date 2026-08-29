package com.techprotech.agenda.modulos.whatsapp.onboarding.aplicacion;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ReconciliadorOnboardingWhatsapp {

    private final ServicioOnboardingWhatsapp servicioOnboardingWhatsapp;

    public ReconciliadorOnboardingWhatsapp(ServicioOnboardingWhatsapp servicioOnboardingWhatsapp) {
        this.servicioOnboardingWhatsapp = servicioOnboardingWhatsapp;
    }

    @Scheduled(
            fixedDelayString = "${aplicacion.whatsapp.onboarding.reconciliacion-delay-ms:30000}",
            initialDelayString = "${aplicacion.whatsapp.onboarding.reconciliacion-initial-delay-ms:20000}"
    )
    public void reconciliar() {
        servicioOnboardingWhatsapp.reconciliarPendientes();
    }
}
