# Sprint 5 · Registro, invitaciones y web operativa

**Objetivo:** que cualquier persona pueda registrarse y unirse, y que la página publicada funcione con el servidor real.
**Historias:** HU-11, HU-12, HU-13, HU-14.

## Sprint Backlog
- **5A:** `Cuenta`, `Invitacion`, `CodigoInvitacion`, `EstadoInvitacion`; login con correo o cédula; pantallas en Swing y web.
- **5B:** nueva portada (estilo landing de Jira), acceso (estilo Atlassian) y aplicación con menú lateral; eliminar
  `servidorLocal.js` (modo demo); CSS propio; `config.js` + CORS por dominio; `Dockerfile`, `render.yaml` y `netlify.toml`.

## Incremento
Flujo completo: crear cuenta → código del gerente → entrar con el rol correcto. Página sin modo demo; imagen Docker probada
y CORS verificado desde otro origen (y bloqueado para orígenes no autorizados).

## Review
El usuario pidió el registro con correo durante el sprint; se agregó como 5A sin romper lo existente.
Impedimentos: la URL de Netlify no se conocía al inicio; se dejó todo configurable en `config.js`.

## Retrospectiva
- **Bien:** la arquitectura MVC permitió rehacer toda la web sin tocar el modelo.
- **Mejorar:** el sitio de Netlify estaba subido a mano; se documentó cómo conectarlo a GitHub.
