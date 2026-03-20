package com.techprotech.agenda.compartido.exception;

import java.time.OffsetDateTime;

public record ErrorApi(
        String codigo,
        String mensaje,
        OffsetDateTime fechaHora
) {
}

