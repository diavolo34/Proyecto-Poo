# Sprint 4 · Vista de escritorio (Swing)

**Objetivo:** una aplicación Swing con MVC que use el mismo modelo que la web.
**Historias:** HU-09, HU-10.

## Sprint Backlog
- `Tema` (sistema de diseño), `VentanaLogin`, `VentanaPrincipal` con pestañas por rol.
- Paneles: asistencia, incidencias (combos dependientes), tablero Kanban, supervisión (gráfico Java2D) y personal.
- Una `Controladora` por pantalla (patrón visto en clase).

## Incremento
Al ejecutar en NetBeans se abren a la vez el servidor web y la ventana de escritorio, que comparten los datos.

## Review
Se revisaron 13 capturas reales (pantalla virtual) y se probaron interacciones (login fallido, aprobar desde el
tablero). Se encontraron y corrigieron: un componente sin accesibilidad que fallaba al crearse, el formulario de
login desbordado, el botón «Rechazar» cortado, supervisores con 0 % (sin historial de demo) y un error confuso
al ejecutar dos veces (puerto ocupado).

## Retrospectiva
- **Bien:** revisar capturas reales encontró errores que la compilación no ve.
- **Mejorar:** los datos de demo deben ser coherentes (José asistía en días de reposo).
