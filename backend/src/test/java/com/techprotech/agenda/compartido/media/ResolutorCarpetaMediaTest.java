package com.techprotech.agenda.compartido.media;

import com.techprotech.agenda.compartido.media.aplicacion.ResolutorCarpetaMedia;
import com.techprotech.agenda.compartido.media.config.PropiedadesCloudinary;
import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResolutorCarpetaMediaTest {
    @Test
    void separaCadaEmpresaYCategoria() {
        ResolutorCarpetaMedia resolutor = new ResolutorCarpetaMedia(propiedades("Fluora"));

        assertThat(resolutor.resolver(42L, CategoriaMedia.GALERIA))
                .isEqualTo("Fluora/multitenant/42/galeria");
        assertThat(resolutor.resolver(42L, CategoriaMedia.COLABORADORES))
                .isEqualTo("Fluora/multitenant/42/colaboradores");
        assertThat(resolutor.resolver(7L, CategoriaMedia.SERVICIOS))
                .isEqualTo("Fluora/multitenant/7/servicios");
        assertThat(resolutor.resolver(7L, CategoriaMedia.BRANDING))
                .isEqualTo("Fluora/multitenant/7/branding");
    }

    @Test
    void rechazaRaicesQuePuedanAlterarLaJerarquia() {
        assertThatThrownBy(() -> new ResolutorCarpetaMedia(propiedades("Fluora/otro")))
                .isInstanceOf(IllegalStateException.class);
    }

    private PropiedadesCloudinary propiedades(String raiz) {
        return new PropiedadesCloudinary(false, "", "", "", raiz, DataSize.ofMegabytes(10));
    }
}
