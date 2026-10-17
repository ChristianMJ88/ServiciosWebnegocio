package com.techprotech.agenda.modulos.politicas.api.dto;

import com.techprotech.agenda.modulos.politicas.dominio.ModoNoShow;
import jakarta.validation.constraints.*;

public record PublicarPoliticaRequest(
        @NotNull @Min(0) Integer versionBase, @NotNull ModoNoShow modoNoShow,
        @Positive Integer diasReagendar, @NotNull @Positive Integer diasAclaracion,
        @Positive Integer reagendadosMax, @Positive Integer recordatorio1Dias, @Positive Integer recordatorio2Dias,
        @Size(max=2000) String textoAdicional,
        @Email @Size(max=255) String contactoReclamosCorreo,
        @Pattern(regexp="[+0-9() .-]*") @Size(max=30) String contactoReclamosTelefono) {}
