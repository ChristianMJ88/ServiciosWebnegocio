package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioSitioPublico {

    private final EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio;

    public ServicioSitioPublico(EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio) {
        this.empresaSitioConfigRepositorio = empresaSitioConfigRepositorio;
    }

    public SitioPublicoResponse obtenerPorSlug(String slug) {
        EmpresaSitioConfigEntidad sitio = empresaSitioConfigRepositorio.findBySlug(slug)
                .filter(EmpresaSitioConfigEntidad::isPublicado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un sitio publicado para ese tenant"));

        return mapearSitio(sitio);
    }

    public SitioPublicoResponse obtenerPorHostname(String hostname) {
        EmpresaSitioConfigEntidad sitio = empresaSitioConfigRepositorio.findByDominioPrincipalIgnoreCase(hostname)
                .filter(EmpresaSitioConfigEntidad::isPublicado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un sitio publicado para ese dominio"));

        return mapearSitio(sitio);
    }

    private SitioPublicoResponse mapearSitio(EmpresaSitioConfigEntidad sitio) {
        return new SitioPublicoResponse(
                sitio.getEmpresaId(),
                sitio.getSlug(),
                sitio.getNombreComercial(),
                sitio.getDominioPrincipal(),
                sitio.getLogoUrl(),
                sitio.getDescripcionCorta(),
                sitio.getColorPrimario(),
                sitio.getColorSecundario(),
                sitio.getHeroTitulo(),
                sitio.getHeroSubtitulo(),
                sitio.getWhatsapp(),
                sitio.getTelefono(),
                sitio.getCorreo(),
                sitio.getDireccion(),
                sitio.getInstagramUrl(),
                sitio.getFacebookUrl(),
                sitio.getTema(),
                sitio.isPublicado()
        );
    }
}
