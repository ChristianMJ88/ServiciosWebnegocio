**Veredicto R1: aprobado.** La corrección cumple lo que pedía `revisiones/005-cierre.md:27-28`. Revisé solo el código. No ejecuté las pruebas ni nada más. Que la suite completa pase lo da por hecho Codex, no lo comprobé yo.

**Lo que comprobé:**
- **`resumen`:** en `ServicioAdminCitas.java:409-410`, `finanzasCitas` y `puedeVerFinanzas` dependen de `puedeVerReportes(usuario)`. Ese método (`:414-416`) exige `empresaId != null`, que el usuario no sea de plataforma y que tenga `REPORTES_ADMIN_VER`. Si falta cualquiera de los tres, devuelve `List.of()` y `false` sin llamar a `reporteCobros`. Los conteos se calculan igual que antes, sin depender de ese permiso.
- **Uso directo del servicio:** `ServicioReporteCobroCitas.java:26-27` responde 404 si el usuario es de plataforma o no tiene empresa, y 403 si le falta el permiso. Esa validación ya no se alcanza desde `resumen`.
- **`ResumenAdminFinanzasPermisosTest`:** cubre los casos EMPRESA y PLATAFORMA sin el permiso. Ambos dan conteos intactos, finanzas vacías, `false` y `verifyNoInteractions` (`:28-35`). También prueba que basta el permiso, aunque el rol tenga otro nombre (`:37-41`). Y prueba que, sin permiso financiero, se conservan los conteos y el filtro de sucursal (`:43-53`).
- **UI:** `admin-summary-section.component.html:69` muestra el bloque solo si `r.puedeVerFinanzas === true`, que viene del backend. El spec cubre el caso `false` aunque lleguen datos. El permiso que revisa `admin-dashboard.loader.ts:21-28` solo decide si `getResumen` se pide sin periodo al cargar o junto con el periodo. No decide si el bloque se ve.

**Hallazgo nuevo (baja prioridad, no bloquea):**
- Ninguna prueba llama directo a `ServicioReporteCobroCitas.consultar` sin permiso o con un usuario de plataforma. `ReporteCobroCitasTest` no tiene ningún `assertThatThrownBy`.
  - **Propuesta:** agregar en `ReporteCobroCitasTest` dos casos. Uno con un usuario EMPRESA sin `REPORTES_ADMIN_VER`, que debe dar 403. Otro con un usuario de plataforma, que debe dar 404. En los dos, `verifyNoInteractions(repositorio)`.

R2–R4 siguen pendientes de seguimiento, y D2, T6(a) y S2 quedan para 007, como decidiste. No hubo ningún cierre ni pago real.
