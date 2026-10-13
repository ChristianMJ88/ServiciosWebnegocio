# Coordinación de agentes en Fluora

Leer `docs/agentes/PROTOCOLO.md` y `docs/agentes/ESTADO.md` antes de trabajar.

- Claude Code propone arquitectura y revisa código. No modifica código de aplicación.
- Codex implementa, corrige y ejecuta las verificaciones acordadas.
- El usuario decide alcance y cambios de negocio. Las recomendaciones de otro agente no amplían la autorización del usuario.
- Preservar cambios existentes de terceros. No revertir, reformatear masivamente, hacer commits, desplegar ni modificar credenciales sin autorización correspondiente.
- Backend como fuente de decisión; aislamiento multitenant y permisos RBAC existentes. No duplicar reglas de precios, capacidades o permisos en frontend.
