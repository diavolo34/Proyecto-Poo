# Sprint 8 · Seguridad y recuperación de clave

**Objetivo:** que ConstruTrack sea seguro para usarlo con datos reales y que nadie pierda el acceso por olvidar su clave.
**Historias:** HU-31 y HU-32 (la HU-33, asistencia comprobable, queda propuesta para el siguiente sprint).

## Sprint Backlog
- **Recuperar la clave por correo** (HU-31):
  - «¿Olvidaste tu clave?» en la pantalla de inicio de sesión;
  - un enlace de un solo uso que vence en 30 minutos, con un máximo de 3 por hora;
  - la misma respuesta exista o no el correo;
  - al cambiar la clave se cierran todas las sesiones.
- **Claves:** PBKDF2-HMAC-SHA256 con 600 000 iteraciones y sal aleatoria. Las claves antiguas se migran solas al entrar, y las claves nuevas exigen 8 caracteres con letras y números.
- **Ingreso:** bloqueo de 15 minutos tras 5 intentos fallidos, y la respuesta tarda lo mismo exista o no la cuenta.
- **Sesiones:** tokens de 256 bits guardados como huella, que vencen a las 12 horas.
- **Navegador:** cabeceras de seguridad (CSP, HSTS, `X-Frame-Options` y otras) en la API, en el servidor Java y en Netlify (`_headers`). Se quitaron los scripts en línea y se limitó el cambio de API por la URL a `localhost`.
- **Portada:** el texto bajo las cifras «En vivo» ya se lee completo.
- **Documento** [docs/seguridad.md](../seguridad.md): medidas, cookies, lista de verificación, mejoras recomendadas e ideas de asistencia comprobable.

## Incremento
- 69 pruebas JUnit, 12 de ellas nuevas: cifrado, migración, bloqueo, recuperación completa y vencimiento de sesiones.
- Pruebas de humo de la API: cabeceras, recuperación de clave y bloqueo por intentos.

## Review
Se recorrió en el navegador todo el flujo:
1. Pedir el enlace.
2. Abrirlo; el código desaparece de la barra de direcciones.
3. Probar una clave débil, que se rechaza.
4. Guardar la clave nueva.
5. Comprobar que la clave vieja ya no sirve y que el enlace no se puede reutilizar.

Además, se comprobó que no hubiera errores de CSP en ninguna pantalla.

## Retrospectiva
- **Bien:** las pruebas con un reloj ajustable permitieron comprobar vencimientos y bloqueos sin esperar.
- **Mejorar:** cargar los datos ya no calcula cifrados innecesarios; antes se creaba cada persona con una clave «temporal».
- **Para el futuro:** verificar el correo al registrarse, verificación en dos pasos para jefes y la asistencia comprobable (HU-33).
