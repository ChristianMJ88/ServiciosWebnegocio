No tengo herramienta de escritura en esta sesión (solo Read, Glob y Grep), así que no pude crear `docs/agentes/revisiones/008-f4-t1-final.md`. Abajo va la revisión completa para guardarla tal cual en esa ruta. No ejecuté pruebas, builds ni git.

---

# 008 F4 T1 final: selector puro `seleccionarModuloActivo`. Revisión de código

Autor: Claude Code, 2026-10-19. Revisión de solo lectura. No ejecuté pruebas, builds ni git mientras Codex hace las tres corridas.

## Veredicto
**Aprobado, sin bloqueantes.** La opción a está aplicada como se diseñó y el componente usa de verdad el selector. **T1 se cierra cuando estén registradas las tres corridas normales en verde**, con la evidencia que se pide abajo.

## Verificación
1. **Selector** (`admin-dashboard.config.ts:37-39`):
   - Es `find(id === seccion) ?? modulos.at(0) ?? null`. Hace lo mismo que el computed anterior (`008-f4-m3-diseno.md:11`): misma regla, mismo orden de respaldo y mismo resultado `null`.
   - `.at()` es válido con `target: ES2022` (`tsconfig.json:15`).
   - Recibe `readonly ModuloAdminDef[]` y no cambia la lista.
2. **Uso real** (`admin-dashboard.component.ts:375`): `moduloActivo = computed(() => seleccionarModuloActivo(this.modulosAdmin(), this.seccionActiva()))`. No queda otra copia de esa lógica en el computed.
3. **Template** (`admin-dashboard.component.html:185-186`): conserva `moduloActivo()?.titulo` y `?.descripcion`. Son los únicos usos de `moduloActivo()`. Si alguien quita el `?.`, `strictTemplates: true` (`tsconfig.json:22`) da un error de compilación, porque el tipo es `ModuloAdminDef | null`.
4. **RBAC sin cambios:**
   - `filtrarModulosAdmin` (`:113-119`) sigue exigiendo permiso **y** sección concedida por el backend.
   - La redirección de `seccionActiva` (`component.ts:2619-2621`) no se tocó.
   - Backend sin cambios.
5. **Pruebas** (`admin-dashboard.config.spec.ts:6-23`):
   - Lista vacía: `filtrarModulosAdmin([], new Set())` da `[]`, y el selector da `null`. Es correcto: con el set vacío también se excluye `resumen`, que no pide permiso.
   - Sección ausente: el set solo trae `servicios`, así que los módulos son `[servicios]` y `pagos` cae en `modulos[0]`. La guarda `length > 0` evita que la prueba pase por accidente.
   - Sección válida: con `resumen` y `servicios` se devuelve `servicios`, no el primero de la lista. Esto distingue de verdad entre elegir la sección pedida y caer en el respaldo.
6. **Spec pesado:** en el working tree solo queda `admin-dashboard.config.spec.ts` en `pages/admin-dashboard/`. El `admin-dashboard.component.spec.ts` ya no existe.
7. **`isolate:true`** (`vitest.config.mjs:1-7`): no cambia y no tiene workers ni timeouts. La justificación sigue siendo la de `008-f4-t1.md` §1-2.

## Hallazgos

| Prioridad | Dónde | Hallazgo | Qué hacer |
|---|---|---|---|
| Baja (opcional) | `admin-dashboard.config.spec.ts:15` | `toBe(modulos[0])` repite la implementación (`at(0)`). La prueba sigue valiendo, pero no dice qué módulo se espera. | Agregar `expect(seleccionarModuloActivo(modulos, 'pagos')?.id).toBe('servicios')`. |
| Informativa, fuera de alcance | `admin-dashboard.component.ts:933-937` | Dos getters de título y descripción repiten el `find` **sin** respaldo al primer módulo (`'Panel Admin'` / `''`). Si se usan mientras `seccionActiva` todavía no se redirige, pueden no coincidir con `moduloActivo()`. Es comportamiento anterior y no se toca en T1. | Anotarlo como deuda. Si después se unifica, que use `seleccionarModuloActivo`. |
| Informativa | Revisión anterior `008-f4-t1.md`, hallazgo Bajo del override | Quedó resuelto, porque se retiró el override con template copiado. | Nada. |

## Sobre el umbral de 2.5 s
Tienes razón: **no es un criterio original de T1**. T1 pide `npm test` en verde 3 veces con la configuración normal (`008-f4-m3-diseno.md:92,96`). Lo de 2.5 s lo propuse en `008-f4-t1.md` como margen preventivo. Exigir que se aligere toda prueba que pase de 2.5 s ampliaría el alcance a specs de UI que hoy pasan, así que **lo retiro como requisito**.

Lo que queda:
- **Requisito:** en cada corrida, registrar el comando, Node, `TZ`, archivos y pruebas aprobados, la duración total y **las 3 pruebas más lentas con su tiempo**, más el margen respecto a 5 s.
- **Solo alerta, no requisito de T1:** si alguna prueba pasa de **4 s** (80 % del timeout) en alguna corrida, se reporta con su nombre como riesgo, sin aligerarla dentro de T1. La decisión se toma en una solicitud aparte.
- **Si falla**, aunque sea un timeout en 1 de 3 corridas: T1 no se cierra, se reporta tal cual y no se suben tiempos ni se reducen workers.

## Límites
- No ejecuté nada. El veredicto de que la suite queda en verde depende de las tres corridas de Codex.
- No usé `git diff` ni `git ls-files`. Que el spec pesado «no llegó a versionarse» lo comprobé solo con su ausencia en el working tree. Antes del commit, confirmar que `git status` no lo muestra como borrado.
- Aplico a `booking-shell.spec.ts` y Header lo mismo que en la revisión anterior: no los volví a revisar.

## Siguiente paso
1. Tres corridas de `npm test` normales, con la evidencia de arriba.
2. Si las tres salen en verde, hacer los commits en el orden de `008-f4-t1.md` (primero M1, M2 y CLS; después T1, con `vitest.config.mjs`, `admin-dashboard.config.ts`, el spec y `admin-dashboard.component.ts`). Sin push ni despliegue.
