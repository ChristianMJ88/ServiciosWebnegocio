package com.techprotech.agenda.compartido.direcciones.aplicacion;

import com.techprotech.agenda.compartido.direcciones.dominio.Direccion;
import com.techprotech.agenda.compartido.direcciones.dominio.ResultadoValidacionDireccion;
import com.techprotech.agenda.compartido.direcciones.dominio.ValidadorDireccionPort;
import com.techprotech.agenda.compartido.direcciones.infraestructura.google.PropiedadesGoogleMaps;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ServicioValidacionDirecciones {
    private final ValidadorDireccionPort validador;
    private final PropiedadesGoogleMaps propiedades;

    public ServicioValidacionDirecciones(ValidadorDireccionPort validador, PropiedadesGoogleMaps propiedades) {
        this.validador = validador;
        this.propiedades = propiedades;
    }

    public ResultadoValidacionDireccion validar(Direccion direccion) {
        Direccion limpia = limpiar(direccion);
        validarRegion(limpia.codigoPais());
        return validador.validar(limpia);
    }

    public ResultadoValidacionDireccion validarParaPersistencia(Direccion direccion) {
        Direccion limpia = limpiar(direccion);
        if (!propiedades.habilitado()) {
            return new ResultadoValidacionDireccion(false, "NO_CONFIGURADA",
                    "La validación de direcciones no está habilitada", null, limpia, null, null, null);
        }
        ResultadoValidacionDireccion resultado = validar(limpia);
        if (!resultado.valida() && propiedades.validacionRequerida()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, resultado.mensaje());
        }
        return resultado.valida() ? resultado
                : new ResultadoValidacionDireccion(false, resultado.estadoValidacion(), resultado.mensaje(),
                resultado.direccionFormateada(), limpia, resultado.placeId(), resultado.latitud(), resultado.longitud());
    }

    public Configuracion configuracion() {
        return new Configuracion(propiedades.autocompleteDisponible(), propiedades.servidorDisponible(),
                propiedades.validacionRequerida(), propiedades.autocompleteDisponible() ? propiedades.browserApiKey() : null,
                regionPredeterminada(), regionesPermitidas());
    }

    private Direccion limpiar(Direccion direccion) {
        if (direccion == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Captura la dirección completa");
        return new Direccion(requerido(direccion.calle()), requerido(direccion.numeroExterior()), opcional(direccion.numeroInterior()),
                requerido(direccion.colonia()), requerido(direccion.municipio()), requerido(direccion.estado()),
                requerido(direccion.codigoPostal()), pais(direccion.codigoPais()));
    }

    private void validarRegion(String region) {
        if (!regionesPermitidas().contains(region.toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El país indicado no está habilitado para validar direcciones");
        }
    }

    private List<String> regionesPermitidas() {
        List<String> regiones = propiedades.regionesPermitidas();
        if (regiones == null || regiones.isEmpty()) return List.of(regionPredeterminada());
        return regiones.stream().filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.trim().toUpperCase(Locale.ROOT)).distinct().toList();
    }

    private String regionPredeterminada() {
        String region = propiedades.regionPredeterminada();
        if (region != null && !region.isBlank()) return region.trim().toUpperCase(Locale.ROOT);
        if (propiedades.regionesPermitidas() != null) {
            return propiedades.regionesPermitidas().stream().filter(valor -> valor != null && !valor.isBlank())
                    .map(valor -> valor.trim().toUpperCase(Locale.ROOT)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Configura una región predeterminada para Google Maps"));
        }
        throw new IllegalStateException("Configura una región predeterminada para Google Maps");
    }

    private String pais(String valor) {
        return valor == null || valor.isBlank() ? regionPredeterminada() : valor.trim().toUpperCase(Locale.ROOT);
    }

    private String requerido(String valor) {
        if (valor == null || valor.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La dirección está incompleta");
        return valor.trim();
    }
    private String opcional(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }

    public record Configuracion(boolean autocompleteHabilitado, boolean validacionServidorHabilitada,
                                boolean validacionRequerida, String browserApiKey,
                                String regionPredeterminada, List<String> regionesPermitidas) {}
}
