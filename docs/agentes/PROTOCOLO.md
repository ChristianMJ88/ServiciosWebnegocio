# Comunicación Claude Code ↔ Codex

Los archivos de esta carpeta son el canal persistente. No implican comunicación automática con una sesión de terminal ya abierta.

## Responsabilidades

Claude define arquitectura y revisa la implementación; Codex escribe código y pruebas. Ninguno cambia el alcance autorizado por el usuario. Las discrepancias se documentan con evidencia antes de implementar una decisión incompatible.

## Ciclo de trabajo

1. Codex crea una solicitud numerada: objetivo, alcance, archivos relevantes, restricciones, criterios de aceptación y estado del working tree.
2. Claude devuelve diseño o hallazgos con prioridad, ubicación, motivo y prueba esperada. Si no hay hallazgos, indica alcance revisado y límites.
3. Codex evalúa cada hallazgo, implementa lo que corresponde al alcance y registra resultado o motivo para descartarlo.
4. Claude revisa el resultado. Codex registra pruebas y pendientes en ESTADO.md.

Usar `solicitudes/NNN.md`, `revisiones/NNN.md` y `respuestas/NNN.md`. Cada archivo tiene un solo autor: solicitud y respuesta, Codex; revisión, Claude. No escribir simultáneamente el mismo archivo. ESTADO.md lo mantiene Codex.

## Seguridad y concurrencia

- No incluir secretos ni datos privados en solicitudes o revisiones.
- Leer el diff y archivos nuevos; no atribuir todo el working tree a la tarea actual.
- Claude trabaja en lectura. Codex es el único escritor de código de aplicación.
- Las migraciones nuevas se coordinan antes de asignar versión. Nunca editar una migración ya aplicada.
- No usar bypass de permisos. Ningún agente autoriza a otro a publicar, cobrar, enviar correos o cambiar accesos.
- El frontend presenta decisiones del backend; los contratos, cálculos y RBAC se validan por tenant en backend.

## Terminal

`bash scripts/claude-review.sh docs/agentes/solicitudes/001.md docs/agentes/revisiones/001.md`

El comando inicia una revisión independiente con las credenciales locales de Claude Code, limitada a Read, Glob y Grep. Guarda la salida solo si finaliza correctamente y evita sobrescribir una revisión existente. No controla otra sesión de Claude ya abierta.
