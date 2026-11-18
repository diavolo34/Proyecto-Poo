# Sprint 2 · Dominio con profundidad

**Objetivo:** responder a la corrección: clase Historial, incidencias (como faltar por enfermedad) y obras.
**Historias:** HU-03, HU-04, HU-05.

## Sprint Backlog
- `Historial` (composición en `Usuario`) con consultas por `Periodo`.
- `Incidencia` abstracta y 5 tipos, `EstadoIncidencia` con transiciones, interfaz `Justificable`, `FabricaIncidencias`.
- `RevisorIncidencias` para `Supervisor` y `Gerente`.
- `Obra`; persistencia de obras e incidencias; datos de prueba con dos semanas de historial.

## Incremento
Rutas `/api/incidencias` (listar, reportar, revisar) con permisos por rol, persistidas al reiniciar.

## Review
Se probaron 14 casos con curl. Dos expectativas de la prueba estaban mal (asumían que María y Laura eran de otro
supervisor); el código era correcto. Se encontró y corrigió un error real: «hasta» vacío causaba un error de formato.

## Retrospectiva
- **Bien:** probar casos negativos (403/409/400), no solo el camino feliz.
- **Mejorar:** revisar los datos de prueba antes de escribir las expectativas.
