# Scrum en ConstruTrack

La entrega 2 se trabajó con **Scrum**: un Product Backlog priorizado, siete sprints cortos con un objetivo cada uno,
un incremento funcionando al final de cada sprint (subido a GitHub con la integración continua en verde) y una
revisión y retrospectiva por sprint.

## Roles

| Rol | Quién | Responsabilidad en este proyecto |
|-----|-------|----------------------------------|
| **Product Owner** | Profesora de la materia (cliente) | Define y prioriza los requisitos: corrección de la entrega 1, MVC, SOLID, UX/UI y Swing. |
| **Scrum Master** | Francisco Manzano | Cuida el proceso: sprints cortos, revisión con capturas y CI, y quitar impedimentos (despliegue, firma de commits). |
| **Development Team** | Francisco Manzano, con asistencia de IA (Claude Code; prompts en `docs/prompts.md`) | Diseña, programa, prueba y documenta cada incremento. |

## Artefactos

- [Product Backlog](product-backlog.md): historias de usuario priorizadas con criterios de aceptación.
- Sprint Backlog e incremento de cada sprint: [Sprint 1](sprint-1.md) · [Sprint 2](sprint-2.md) · [Sprint 3](sprint-3.md) ·
  [Sprint 4](sprint-4.md) · [Sprint 5](sprint-5.md) · [Sprint 6](sprint-6.md) · [Sprint 7](sprint-7.md).
- **Definición de terminado (DoD)**, que aplica a toda historia:
  1. Compila sin advertencias (`-Xlint:all -Werror`) desde una copia limpia del repositorio.
  2. Las pruebas (JUnit y prueba de humo de la API) pasan en el CI de GitHub.
  3. Se probó en la interfaz real (capturas de web y/o Swing revisadas).
  4. Commit firmado con un mensaje que explica el porqué.

## Eventos

| Evento | Cómo se hizo |
|--------|--------------|
| Sprint Planning | Al inicio de cada sprint se eligió el objetivo y las historias del backlog (ver cada `sprint-N.md`). |
| Daily | Revisión corta del avance: qué se terminó, qué sigue y qué lo bloquea (por ejemplo, la URL de Netlify). |
| Sprint Review | Demostración del incremento funcionando: capturas reales, pruebas con curl/Playwright y CI en verde. |
| Retrospectiva | Qué salió bien, qué salió mal y qué se cambia en el siguiente sprint (por ejemplo, compilar desde una copia limpia antes de subir). |
