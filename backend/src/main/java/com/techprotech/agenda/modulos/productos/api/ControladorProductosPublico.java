package com.techprotech.agenda.modulos.productos.api;

import com.techprotech.agenda.modulos.productos.api.dto.CatalogoProductosPublicoResponse;
import com.techprotech.agenda.modulos.productos.api.dto.IndiceProductosPublicosResponse;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioCatalogoProductosPublico;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publico/productos/sitio")
public class ControladorProductosPublico {
    private final ServicioCatalogoProductosPublico servicio;

    public ControladorProductosPublico(ServicioCatalogoProductosPublico servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/indice")
    public IndiceProductosPublicosResponse indice() {
        return servicio.indice();
    }

    @GetMapping("/{slug}")
    public CatalogoProductosPublicoResponse catalogo(@PathVariable String slug,
                                                      @RequestParam(required = false) String zona, @RequestParam(required=false) Long sucursalId) {
        return servicio.obtener(slug, zona, null, sucursalId);
    }

    @GetMapping("/{slug}/{productoSlug}")
    public CatalogoProductosPublicoResponse producto(@PathVariable String slug, @PathVariable String productoSlug,
                                                     @RequestParam(required = false) String zona, @RequestParam(required=false) Long sucursalId) {
        return servicio.obtener(slug, zona, productoSlug, sucursalId);
    }
}
