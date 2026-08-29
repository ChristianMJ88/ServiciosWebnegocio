package com.techprotech.agenda.modulos.whatsapp.onboarding.aplicacion;

import com.techprotech.agenda.compartido.whatsapp.ClienteWhatsappTwilio;
import com.techprotech.agenda.compartido.whatsapp.ConfiguracionWhatsappEmpresaEntidad;
import com.techprotech.agenda.compartido.whatsapp.ConfiguracionWhatsappEmpresaRepositorio;
import com.techprotech.agenda.compartido.whatsapp.PropiedadesWhatsapp;
import com.techprotech.agenda.modulos.admin.api.dto.AsociarChannelSenderWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarMessagingServiceWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarSubcuentaWhatsappRequest;
import com.techprotech.agenda.modulos.admin.aplicacion.ServicioAdminCitas;
import com.techprotech.agenda.modulos.whatsapp.onboarding.api.CompletarOnboardingWhatsappRequest;
import com.techprotech.agenda.modulos.whatsapp.onboarding.api.IniciarOnboardingWhatsappRequest;
import com.techprotech.agenda.modulos.whatsapp.onboarding.api.OnboardingWhatsappResponse;
import com.techprotech.agenda.modulos.whatsapp.onboarding.infraestructura.WhatsappOnboardingEntidad;
import com.techprotech.agenda.modulos.whatsapp.onboarding.infraestructura.WhatsappOnboardingRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;
import java.util.List;

@Service
public class ServicioOnboardingWhatsapp {

    private final WhatsappOnboardingRepositorio onboardingRepositorio;
    private final ConfiguracionWhatsappEmpresaRepositorio configuracionRepositorio;
    private final ServicioAdminCitas servicioAdminCitas;
    private final ClienteWhatsappTwilio clienteTwilio;
    private final PropiedadesWhatsapp propiedades;

    public ServicioOnboardingWhatsapp(
            WhatsappOnboardingRepositorio onboardingRepositorio,
            ConfiguracionWhatsappEmpresaRepositorio configuracionRepositorio,
            ServicioAdminCitas servicioAdminCitas,
            ClienteWhatsappTwilio clienteTwilio,
            PropiedadesWhatsapp propiedades
    ) {
        this.onboardingRepositorio = onboardingRepositorio;
        this.configuracionRepositorio = configuracionRepositorio;
        this.servicioAdminCitas = servicioAdminCitas;
        this.clienteTwilio = clienteTwilio;
        this.propiedades = propiedades;
    }

    public synchronized OnboardingWhatsappResponse iniciar(
            Long empresaId,
            Long usuarioActorId,
            IniciarOnboardingWhatsappRequest request
    ) {
        requerirConfiguracionEmbeddedSignup();
        WhatsappOnboardingEntidad onboarding = onboardingRepositorio.findByEmpresaId(empresaId)
                .orElseGet(() -> onboardingDesdeConfiguracionExistente(empresaId));
        if ("ACTIVO".equals(onboarding.getEstado()) || tieneTexto(onboarding.getChannelSenderSid())) {
            return mapear(onboarding);
        }
        onboarding.setUsuarioActorId(usuarioActorId);
        onboarding.setTelefonoE164(request.telefonoE164().trim());
        onboarding.setDisplayName(request.displayName().trim());
        onboarding.setEstado("ESPERANDO_META");
        onboarding.setPasoActual("EMBEDDED_SIGNUP");
        onboarding.setUltimoError(null);
        return mapear(onboardingRepositorio.save(onboarding));
    }

    public synchronized OnboardingWhatsappResponse completar(
            Long empresaId,
            Long usuarioActorId,
            CompletarOnboardingWhatsappRequest request
    ) {
        WhatsappOnboardingEntidad onboarding = onboardingRepositorio
                .findByIdAndEmpresaId(request.onboardingId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding de WhatsApp no encontrado"));
        if ("ACTIVO".equals(onboarding.getEstado())) return mapear(onboarding);

        onboarding.setUsuarioActorId(usuarioActorId);
        onboarding.setWabaId(request.wabaId().trim());
        onboarding.setPhoneNumberId(limpiar(request.phoneNumberId()));
        onboarding.setTelefonoE164(request.telefonoE164().trim());
        return provisionar(empresaId, usuarioActorId, onboarding);
    }

    public synchronized OnboardingWhatsappResponse reintentar(Long empresaId, Long usuarioActorId) {
        WhatsappOnboardingEntidad onboarding = onboardingRepositorio.findByEmpresaId(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding de WhatsApp no encontrado"));
        if ("ACTIVO".equals(onboarding.getEstado())) return mapear(onboarding);
        if (!tieneTexto(onboarding.getWabaId()) || !tieneTexto(onboarding.getTelefonoE164())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Debes completar nuevamente Embedded Signup porque faltan los datos entregados por Meta"
            );
        }
        onboarding.setUsuarioActorId(usuarioActorId);
        return provisionar(empresaId, usuarioActorId, onboarding);
    }

    private OnboardingWhatsappResponse provisionar(
            Long empresaId,
            Long usuarioActorId,
            WhatsappOnboardingEntidad onboarding
    ) {
        onboarding.setIntentos(onboarding.getIntentos() + 1);
        onboarding.setEstado("CONFIGURANDO");
        onboarding.setPasoActual("CREANDO_SUBCUENTA");
        onboarding.setUltimoError(null);
        onboardingRepositorio.save(onboarding);

        try {
            ConfiguracionWhatsappEmpresaEntidad configuracion = obtenerConfiguracion(empresaId);
            if (!tieneTexto(configuracion.getSubaccountSid())) {
                servicioAdminCitas.provisionarSubcuentaWhatsapp(
                        empresaId, usuarioActorId, new ProvisionarSubcuentaWhatsappRequest(null)
                );
                configuracion = obtenerConfiguracion(empresaId);
            }

            String inboundUrl = urlWebhook("/api/v1/publico/whatsapp/twilio/webhook");
            String statusUrl = urlWebhook("/api/v1/publico/whatsapp/twilio/status");

            if (!tieneTexto(configuracion.getChannelSenderSid())) {
                onboarding.setPasoActual("REGISTRANDO_SENDER");
                onboardingRepositorio.save(onboarding);
                ClienteWhatsappTwilio.SenderTwilioWhatsapp sender = clienteTwilio.registrarSenderWhatsapp(
                        empresaId,
                        onboarding.getTelefonoE164(),
                        onboarding.getWabaId(),
                        inboundUrl,
                        statusUrl,
                        onboarding.getDisplayName()
                );
                onboarding.setChannelSenderSid(sender.sid());
                configuracion.setChannelSenderSid(sender.sid());
                configuracion.setNumeroRemitente(sender.senderId());
                configuracion.setSenderPhoneNumber(sender.senderId());
                configuracion.setSenderDisplayName(sender.displayName());
                configuracion.setSenderStatus(sender.status());
                configuracion.setWabaId(onboarding.getWabaId());
                configuracion.setStatusCallbackUrl(statusUrl);
                configuracionRepositorio.save(configuracion);
            } else {
                onboarding.setChannelSenderSid(configuracion.getChannelSenderSid());
            }

            configuracion = obtenerConfiguracion(empresaId);
            if (!tieneTexto(configuracion.getMessagingServiceSid())) {
                onboarding.setPasoActual("CREANDO_MESSAGING_SERVICE");
                onboardingRepositorio.save(onboarding);
                servicioAdminCitas.provisionarMessagingServiceWhatsapp(
                        empresaId,
                        usuarioActorId,
                        new ProvisionarMessagingServiceWhatsappRequest(null, inboundUrl)
                );
            }

            configuracion = obtenerConfiguracion(empresaId);
            if (tieneTexto(configuracion.getChannelSenderSid()) && !onboarding.isSenderAsociado()) {
                onboarding.setPasoActual("ASOCIANDO_SENDER");
                onboardingRepositorio.save(onboarding);
                servicioAdminCitas.asociarChannelSenderWhatsapp(
                        empresaId,
                        usuarioActorId,
                        new AsociarChannelSenderWhatsappRequest(configuracion.getChannelSenderSid())
                );
                onboarding.setSenderAsociado(true);
                onboardingRepositorio.save(onboarding);
            }

            return actualizarEstadoDesdeTwilio(empresaId, onboarding);
        } catch (Exception ex) {
            onboarding.setEstado("ERROR_RECUPERABLE");
            onboarding.setPasoActual("REINTENTAR");
            onboarding.setUltimoError(limitar(ex.getMessage(), 1000));
            onboardingRepositorio.save(onboarding);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo completar la conexion de WhatsApp", ex);
        }
    }

    public OnboardingWhatsappResponse estado(Long empresaId) {
        WhatsappOnboardingEntidad onboarding = onboardingRepositorio.findByEmpresaId(empresaId)
                .orElseGet(() -> onboardingDesdeConfiguracionExistente(empresaId));
        if (tieneTexto(onboarding.getChannelSenderSid()) && !"ACTIVO".equals(onboarding.getEstado())) {
            try {
                return actualizarEstadoDesdeTwilio(empresaId, onboarding);
            } catch (Exception ignored) {
                // El estado persistido sigue siendo util aunque Twilio no este disponible temporalmente.
            }
        }
        return mapear(onboarding);
    }

    public synchronized void reconciliarPendientes() {
        List<WhatsappOnboardingEntidad> pendientes = onboardingRepositorio
                .findTop50ByEstadoInOrderByActualizadoEnAsc(List.of("CONFIGURANDO", "PENDIENTE_APROBACION"));
        for (WhatsappOnboardingEntidad onboarding : pendientes) {
            if (!tieneTexto(onboarding.getChannelSenderSid())) continue;
            try {
                actualizarEstadoDesdeTwilio(onboarding.getEmpresaId(), onboarding);
            } catch (Exception ignored) {
                // Un fallo temporal no cambia el estado; el siguiente ciclo volvera a consultar Twilio.
            }
        }
    }

    private OnboardingWhatsappResponse actualizarEstadoDesdeTwilio(
            Long empresaId,
            WhatsappOnboardingEntidad onboarding
    ) {
        ClienteWhatsappTwilio.SenderTwilioWhatsapp sender = clienteTwilio.obtenerSenderWhatsapp(
                empresaId, onboarding.getChannelSenderSid()
        );
        ConfiguracionWhatsappEmpresaEntidad configuracion = obtenerConfiguracion(empresaId);
        configuracion.setSenderStatus(sender.status());
        configuracion.setSenderPhoneNumber(sender.senderId());
        configuracion.setSenderDisplayName(sender.displayName());
        configuracion.setWabaId(onboarding.getWabaId());

        boolean activo = esEstadoActivo(sender.status());
        boolean rechazado = esEstadoRechazado(sender.status());
        configuracion.setHabilitado(activo);
        configuracionRepositorio.save(configuracion);
        onboarding.setEstado(activo ? "ACTIVO" : rechazado ? "ERROR_RECUPERABLE" : "PENDIENTE_APROBACION");
        onboarding.setPasoActual(activo ? "COMPLETADO" : rechazado ? "REVISAR_SENDER" : "REVISION_TWILIO_META");
        onboarding.setUltimoError(rechazado ? "Twilio o Meta rechazaron el sender. Revisa el nombre, el numero y el estado del WABA." : null);
        return mapear(onboardingRepositorio.save(onboarding));
    }

    private ConfiguracionWhatsappEmpresaEntidad obtenerConfiguracion(Long empresaId) {
        return configuracionRepositorio.findById(empresaId)
                .orElseGet(() -> {
                    ConfiguracionWhatsappEmpresaEntidad configuracion = new ConfiguracionWhatsappEmpresaEntidad();
                    configuracion.setEmpresaId(empresaId);
                    configuracion.setHabilitado(false);
                    configuracion.setTipoCuentaTwilio("SUBCUENTA");
                    return configuracionRepositorio.save(configuracion);
                });
    }

    private WhatsappOnboardingEntidad nuevoOnboarding(Long empresaId) {
        WhatsappOnboardingEntidad onboarding = new WhatsappOnboardingEntidad();
        onboarding.setId(UUID.randomUUID().toString());
        onboarding.setEmpresaId(empresaId);
        onboarding.setEstado("NO_INICIADO");
        onboarding.setPasoActual("CAPTURAR_TELEFONO");
        return onboarding;
    }

    private WhatsappOnboardingEntidad onboardingDesdeConfiguracionExistente(Long empresaId) {
        ConfiguracionWhatsappEmpresaEntidad configuracion = configuracionRepositorio.findById(empresaId).orElse(null);
        WhatsappOnboardingEntidad onboarding = nuevoOnboarding(empresaId);
        if (configuracion == null || !tieneTexto(configuracion.getChannelSenderSid())) {
            return onboarding;
        }
        onboarding.setTelefonoE164(normalizarTelefono(configuracion.getSenderPhoneNumber()));
        onboarding.setDisplayName(configuracion.getSenderDisplayName());
        onboarding.setWabaId(configuracion.getWabaId());
        onboarding.setChannelSenderSid(configuracion.getChannelSenderSid());
        onboarding.setSenderAsociado(tieneTexto(configuracion.getMessagingServiceSid()));
        onboarding.setEstado(configuracion.isHabilitado() ? "ACTIVO" : "PENDIENTE_APROBACION");
        onboarding.setPasoActual(configuracion.isHabilitado() ? "COMPLETADO" : "REVISION_TWILIO_META");
        return onboardingRepositorio.save(onboarding);
    }

    private OnboardingWhatsappResponse mapear(WhatsappOnboardingEntidad onboarding) {
        return new OnboardingWhatsappResponse(
                onboarding.getId(), onboarding.getEstado(), onboarding.getPasoActual(),
                onboarding.getTelefonoE164(), onboarding.getDisplayName(), onboarding.getWabaId(),
                onboarding.getPhoneNumberId(), onboarding.getChannelSenderSid(), onboarding.getUltimoError(),
                onboarding.getActualizadoEn(), embeddedSignupDisponible(), propiedades.metaAppId(),
                propiedades.embeddedSignupConfigurationId(), propiedades.partnerSolutionId()
        );
    }

    private void requerirConfiguracionEmbeddedSignup() {
        if (!embeddedSignupDisponible()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Embedded Signup aun no esta configurado en la plataforma");
        }
        if (!tieneTexto(propiedades.webhookPublicBaseUrl())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Falta configurar la URL publica de webhooks de Twilio");
        }
    }

    private boolean embeddedSignupDisponible() {
        return tieneTexto(propiedades.metaAppId())
                && tieneTexto(propiedades.embeddedSignupConfigurationId())
                && tieneTexto(propiedades.partnerSolutionId());
    }

    private String urlWebhook(String path) {
        String base = propiedades.webhookPublicBaseUrl();
        if (!tieneTexto(base)) throw new IllegalStateException("Falta TWILIO_WEBHOOK_PUBLIC_BASE_URL");
        return base.replaceAll("/+$", "") + path;
    }

    private boolean esEstadoActivo(String estado) {
        if (estado == null) return false;
        String normalizado = estado.trim().toUpperCase(Locale.ROOT);
        return "ONLINE".equals(normalizado) || "ACTIVE".equals(normalizado);
    }

    private boolean esEstadoRechazado(String estado) {
        if (estado == null) return false;
        String normalizado = estado.trim().toUpperCase(Locale.ROOT);
        return "FAILED".equals(normalizado)
                || "REJECTED".equals(normalizado)
                || "OFFLINE".equals(normalizado)
                || "DISCONNECTED".equals(normalizado);
    }

    private String limpiar(String valor) {
        return tieneTexto(valor) ? valor.trim() : null;
    }

    private String normalizarTelefono(String valor) {
        String limpio = limpiar(valor);
        if (limpio == null) return null;
        return limpio.regionMatches(true, 0, "whatsapp:", 0, 9) ? limpio.substring(9) : limpio;
    }

    private String limitar(String valor, int maximo) {
        if (valor == null) return null;
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
