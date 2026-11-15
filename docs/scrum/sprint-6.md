# Sprint 6 · Calidad y documentación de la entrega

**Objetivo:** dejar el proyecto listo para entregar y para el portafolio.
**Historias:** HU-02, HU-15, HU-16, HU-17.

## Sprint Backlog
- 26 pruebas JUnit 5 (modelo, persistencia y servicios) con reloj fijo, ejecutadas en el CI.
- 13 diagramas UML nuevos; las secuencias usan solo clases del modelo.
- Documentos `docs/diseno/mvc.md`, `solid.md` y `ux-ui.md`; documentación Scrum.
- Capturas nuevas, README y PDF de la entrega.

## Incremento
Pull Request hacia `main` con todo el trabajo; CI con compilación estricta, pruebas unitarias y prueba de humo.

## Review
Se comprobó que las pruebas detectan errores introduciendo fallos a propósito (tolerancia y rechazo sin motivo).
Las capturas revelaron un problema en el celular (encabezado desbordado), que se corrigió.

## Retrospectiva
- **Bien:** medir el contraste de los colores en lugar de suponerlo.
- **Para el futuro:** correo real (`Notificador` SMTP) y gestión de obras desde la aplicación (HU-18, HU-19).
