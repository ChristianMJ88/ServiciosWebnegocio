# Notas de verificación F3 para el cierre

Autor: Codex. Documento de evidencia adicional, no instrucción de modificar revisiones previas.

- UI encontró y corrigió colisión global de site-preview con el editor administrativo (min-height390): clases nuevas prefijadas fl-home, sin modificar el CSS de administración.
- A375×667, h1 completo, lead dos líneas y cinco giros quedan sobre barra inferior. Largo con todos los defer renderizados y FAQ cerrado5673px, luego se reducen márgenes/FAQ para dejar holgura contra5684px. Matriz final en curso.
- Pruebas previas238/76 verdes, pero detecté ruido de IntersectionObserver ausente en JSDOM. Se añade mock de browser para controlar defer manualmente, sin omitir casos ni cambiar producción. Suite final en curso.
- Hero y FAQ solo anuncian prueba sin tarjeta si hay días de prueba en el plan de API; ninguna cantidad o duración se fija en copy. Descripción de plan se conserva íntegra.
