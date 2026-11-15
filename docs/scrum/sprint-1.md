# Sprint 1 · Refactor a MVC + SOLID

**Objetivo:** reorganizar el proyecto en capas sin cambiar lo que el sistema hace.
**Historias:** HU-01, HU-02.

## Sprint Backlog
- Renombrar el proyecto a `ConstruTrack` y crear los paquetes `modelo`, `persistencia`, `servicio`, `controlador`, `vista` (con `git mv` para conservar el historial).
- Sacar `toJson()` de las entidades hacia `JsonMapper`.
- Dividir `ControladorApi` en `Enrutador` y controladores por recurso.
- Interfaces `LectorDatos`/`EscritorDatos` y servicios con inyección por constructor.
- Jerarquía de excepciones propia; claves con resumen SHA-256; Java 17.

## Incremento
La misma API, pero en capas. Se verificó con curl: login, permisos (401/403/404/409) y registro de usuarios.

## Review
La funcionalidad no cambió y el código quedó listo para crecer. El cambio a Java 17 resuelve que el proyecto pedía
Java 26, una posible causa de que no se pudiera ejecutar en NetBeans.

## Retrospectiva
- **Bien:** mover con `git mv` conservó el historial de cada clase.
- **Mejorar:** el CI y el README quedaron con rutas viejas; se actualizaron en un commit aparte.
