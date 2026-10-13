package com.techprotech.agenda.compartido.tiempo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Auditoría UTC independiente de la zona de la JVM; no se aplica a horarios locales de citas. */
@Converter
public class FechaUtcPersistencia implements AttributeConverter<LocalDateTime, Timestamp> {
    public Timestamp convertToDatabaseColumn(LocalDateTime fecha) {
        return fecha == null ? null : Timestamp.from(fecha.toInstant(ZoneOffset.UTC));
    }
    public LocalDateTime convertToEntityAttribute(Timestamp fecha) {
        return fecha == null ? null : LocalDateTime.ofInstant(fecha.toInstant(), ZoneOffset.UTC);
    }
}
