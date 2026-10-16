package com.techprotech.agenda.compartido.tiempo;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Expone como instante un DATETIME que la aplicación almacena en UTC; no sirve para horas de agenda. */
public final class MomentoUtc {
    private MomentoUtc() {}

    public static OffsetDateTime de(LocalDateTime momento) {
        return momento == null ? null : momento.atOffset(ZoneOffset.UTC);
    }
}
