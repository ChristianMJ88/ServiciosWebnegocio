package com.techprotech.agenda.compartido.correo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BandejaSalidaNotificacionRepositorioImpl implements BandejaSalidaNotificacionRepositorioCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public List<BandejaSalidaNotificacionEntidad> reclamarPendientesEmail(int limite, LocalDateTime ahora) {
        @SuppressWarnings("unchecked")
        List<Number> ids = entityManager.createNativeQuery("""
                SELECT id
                FROM bandeja_salida_notificacion
                WHERE canal = 'EMAIL'
                  AND estado = 'PENDIENTE'
                  AND programada_en <= :ahora
                ORDER BY programada_en ASC
                FOR UPDATE SKIP LOCKED
                """)
                .setParameter("ahora", Timestamp.valueOf(ahora))
                .setMaxResults(limite)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        List<Long> idsLong = ids.stream().map(Number::longValue).toList();
        entityManager.createQuery("""
                UPDATE BandejaSalidaNotificacionEntidad b
                SET b.estado = 'PROCESANDO'
                WHERE b.id IN :ids
                """)
                .setParameter("ids", idsLong)
                .executeUpdate();

        return entityManager.createQuery("""
                SELECT b
                FROM BandejaSalidaNotificacionEntidad b
                WHERE b.id IN :ids
                ORDER BY b.programadaEn ASC
                """, BandejaSalidaNotificacionEntidad.class)
                .setParameter("ids", idsLong)
                .getResultList();
    }
}
