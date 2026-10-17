package com.techprotech.agenda.modulos.politicas.aplicacion;

import com.techprotech.agenda.modulos.politicas.dominio.ModoNoShow;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Solo precarga el borrador; nunca sustituye una versión publicada o aceptada. */
@Getter @Setter @Component @Validated
@ConfigurationProperties(prefix="fluora.politica.predeterminada")
public class PredeterminadosPolitica {
    @NotNull private ModoNoShow modoNoShow;
    @Positive private int diasReagendar;
    @Positive private int diasAclaracion;
    @Positive private int reagendadosMax;
    @Positive private int recordatorio1Dias;
    @Positive private int recordatorio2Dias;
}
