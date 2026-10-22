package com.techprotech.agenda.modulos.dominios.aplicacion;

import java.net.IDN;
import java.net.URI;
import java.util.Locale;
import org.apache.hc.client5.http.psl.PublicSuffixMatcherLoader;
import org.apache.hc.client5.http.psl.PublicSuffixMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ValidadorHostname {
    private static final PublicSuffixMatcher SUFIJOS = cargarSufijos();
    private final String marketing, app;
    public ValidadorHostname(@Value("${aplicacion.dominios.origen-marketing:${aplicacion.correo-bienvenida.origen-publico}}") String marketing,
            @Value("${aplicacion.dominios.origen-app:}") String app) {
        this.marketing = hostOrigen(marketing); this.app = hostOrigen(app);
        if (this.marketing.isEmpty()) throw new IllegalArgumentException("Falta el origen público de la plataforma");
    }
    public String normalizar(String valor) {
        try {
            if (valor == null || valor.length() > 253 || !valor.equals(valor.trim()) || valor.endsWith(".")) throw new IllegalArgumentException();
            String host = IDN.toASCII(valor.toLowerCase(Locale.ROOT), IDN.USE_STD3_ASCII_RULES);
            if (host.length() > 253 || !host.contains(".") || host.matches("[0-9.]+") || host.indexOf(':') >= 0) throw new IllegalArgumentException();
            for (String label : host.split("\\.", -1)) if (label.isEmpty() || label.length() > 63) throw new IllegalArgumentException();
            if (reservado(host, marketing) || reservado(host, app) || reservado(host, "web.app") || reservado(host, "firebaseapp.com")
                    || reservado(host, "localhost") || SUFIJOS.getDomainRoot(host) == null) throw new IllegalArgumentException();
            return host;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica un dominio o subdominio válido, sin protocolo, puerto ni ruta");
        }
    }
    public boolean esRaiz(String host) { return host.equals(SUFIJOS.getDomainRoot(host)); }
    private static PublicSuffixMatcher cargarSufijos() {
        // Load the actual packaged PSL explicitly; the library's default can silently fall back to "com".
        var recurso = PublicSuffixMatcherLoader.class.getResource("/org/publicsuffix/list/effective_tld_names.dat");
        if (recurso == null) throw new IllegalStateException("No está disponible la lista de sufijos públicos");
        try { return PublicSuffixMatcherLoader.load(recurso); }
        catch (java.io.IOException ex) { throw new IllegalStateException("No se pudo cargar la lista de sufijos públicos", ex); }
    }
    private static boolean reservado(String host, String base) { return !base.isEmpty() && (host.equals(base) || host.endsWith("." + base)); }
    private static String hostOrigen(String origen) {
        if (origen == null || origen.isBlank()) return "";
        String host = URI.create(origen).getHost();
        if (host == null) throw new IllegalArgumentException("Origen de plataforma inválido");
        return IDN.toASCII(host.toLowerCase(Locale.ROOT));
    }
}
