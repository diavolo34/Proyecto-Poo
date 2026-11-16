# Despliegue: página en Netlify + servidor Java en Render

Netlify solo sirve archivos estáticos (HTML, CSS y JavaScript): **no puede ejecutar Java**.
Por eso la página publicada mostraba antes el «modo demostración». Ahora el sistema se publica en dos partes:

| Parte | Dónde | Qué hace |
|-------|-------|----------|
| Servidor Java (API) | **Render** (gratis) | Ejecuta `construtrack.App`, guarda los datos y aplica las reglas |
| Página web | **Netlify** — https://startling-toffee-914e34.netlify.app | Muestra la portada y el sistema; llama a la API de Render |

```
Navegador ──► Netlify (index.html, app.html, js/…) ──fetch──► Render (Java: /api/...)
```

## Estado actual

| Parte | Dirección | Cómo se actualiza |
|-------|-----------|-------------------|
| Servidor Java | https://construtrack-api.onrender.com | Automático: Render vuelve a desplegar con cada push a `main` |
| Página web | https://startling-toffee-914e34.netlify.app | Manual: se subió arrastrando la carpeta `vista/web` en **Deploys**. Para que sea automático, conecta el repositorio en Netlify (*Site configuration → Build & deploy → Link repository*) y Netlify usará `netlify.toml` |

La primera visita tras un rato sin uso tarda unos 50 s: el plan gratuito de Render apaga el servidor cuando está inactivo
y la página muestra un aviso mientras arranca. Los datos se guardan en **Upstash Redis** (ver abajo), así que no se pierden al reiniciar.

## 1. Publicar el servidor Java en Render

1. Entra a https://render.com e inicia sesión con tu cuenta de GitHub.
2. **New +** → **Blueprint** → elige el repositorio `diavolo34/Proyecto-Poo` → **Apply**.
   Render lee `render.yaml`, construye el `Dockerfile` y publica el servicio `construtrack-api`.
3. Cuando termine, copia la dirección del servicio (algo como `https://construtrack-api.onrender.com`).
   Comprueba que funciona abriendo `https://TU-SERVICIO.onrender.com/api/estado`: debe responder `{"status":"ok","servidor":"Java"...}`.
4. Si la dirección **no** es `https://construtrack-api.onrender.com`, cámbiala en
   `ConstruTrack/src/construtrack/vista/web/js/config.js` (`apiRemota`) y sube el cambio.

> Plan gratuito: el servicio «duerme» tras 15 minutos sin uso y tarda hasta ~1 minuto en despertar.
> La página muestra «Conectando con el servidor…» mientras tanto (no entra en modo demo).

### Datos permanentes con Upstash (gratis)

El plan gratuito de Render **borra el disco** cada vez que el servidor se duerme o se actualiza: sin más, las cuentas
nuevas desaparecerían. Por eso, si existen las variables `UPSTASH_REDIS_REST_URL` y `UPSTASH_REDIS_REST_TOKEN`,
`App` guarda los datos en una base Redis de Upstash (`AlmacenUpstash`) en vez de un archivo (`AlmacenArchivo`).
Ambos implementan la interfaz `Almacen`, así que el resto del sistema no cambia (principio de inversión de dependencias).

1. Entra a https://console.upstash.com con tu cuenta de GitHub → **Create Database** (Redis, plan Free, región cercana a Oregon).
2. En la base creada, sección **REST API**, copia `UPSTASH_REDIS_REST_URL` y `UPSTASH_REDIS_REST_TOKEN`.
3. En Render → servicio `construtrack-api` → **Environment** → agrega las dos variables con esos valores → **Save changes**.
   Render reinicia el servicio y el registro de arranque dice `datos en Upstash Redis (...)`.

Sin esas variables (por ejemplo, en NetBeans) los datos se guardan en `datos_construtrack.txt`.

### Invitaciones por correo desde Gmail (recomendado, gratis y sin teléfono)

Brevo exige verificar un teléfono y no acepta números de Venezuela; por eso la opción principal es enviar desde tu propio
Gmail con un Google Apps Script (`NotificadorGmail`). Google permite unos 100 correos al día.

1. Entra a https://script.google.com con tu cuenta de Google → **Nuevo proyecto**.
2. Borra lo que trae y pega el contenido de [`tools/correo-gmail/Codigo.gs`](../tools/correo-gmail/Codigo.gs).
   Cambia `CLAVE` por una frase larga que solo tú conozcas y guarda (Ctrl+S).
3. (Opcional) Elige la función `probar` y pulsa **Ejecutar**: Google pide permiso para enviar correos en tu nombre
   («Revisar permisos» → tu cuenta → «Configuración avanzada» → «Ir al proyecto» → «Permitir»). Te llega un correo de prueba.
4. **Implementar → Nueva implementación** → tipo **Aplicación web** → *Ejecutar como*: **Yo** · *Quién tiene acceso*:
   **Cualquier usuario** → **Implementar** → copia la **URL** que termina en `/exec`.
5. En Render → `construtrack-api` → **Environment** agrega `GMAIL_SCRIPT_URL` (la URL) y `GMAIL_SCRIPT_CLAVE`
   (la misma frase de `CLAVE`) → **Save, rebuild and deploy**.

Al arrancar, el registro dice `correo real activado`. Si cambias el código del script, vuelve a implementarlo
(**Implementar → Gestionar implementaciones → editar → Nueva versión**) para que la URL use la versión nueva.

### Invitaciones por correo con Brevo (alternativa)

Render gratis bloquea el envío por SMTP, así que el correo sale por la **API HTTP de Brevo** (`NotificadorBrevo`).

1. Crea una cuenta en https://www.brevo.com (plan Free: 300 correos al día, sin tarjeta).
2. **Senders, Domains & Dedicated IPs → Senders → Add a sender**: agrega tu correo (por ejemplo, tu Gmail) y confírmalo
   con el enlace que te llega.
3. **SMTP & API → API Keys → Generate a new API key** y cópiala.
4. En Render → `construtrack-api` → **Environment** agrega `BREVO_API_KEY` (la clave) y `CORREO_REMITENTE`
   (el correo verificado del paso 2) → **Save changes**.

Al arrancar, el registro dice `correo con Brevo`. En **Invitaciones** el Jefe escribe el correo de la persona y le llega
un mensaje con el código y un botón para crear su cuenta. Sin estas variables, el envío solo se registra en la consola.

### Hora de Venezuela

`ZONA=America/Caracas` (ya en `render.yaml`): el servidor está en UTC y sin esto las entradas quedarían 4 h adelantadas.

## 2. Conectar Netlify con GitHub (para que se actualice solo)

Tu sitio actual se subió arrastrando una carpeta, así que no se actualiza con los cambios del repositorio.

1. En https://app.netlify.com abre el sitio **startling-toffee-914e34**.
2. **Site configuration** → **Build & deploy** → **Continuous deployment** → **Link repository** → GitHub → `diavolo34/Proyecto-Poo`.
3. Rama: `main` (o la rama del proyecto mientras no se fusione). Netlify toma la carpeta a publicar de `netlify.toml`
   (`ConstruTrack/src/construtrack/vista/web`); no hay comando de compilación.
4. Guarda. Cada `git push` vuelve a publicar la página.

Alternativa rápida sin conectar: arrastra otra vez la carpeta `ConstruTrack/src/construtrack/vista/web` en la pestaña **Deploys**.

## 3. Comprobar

- https://startling-toffee-914e34.netlify.app → la portada muestra **«● Sistema en línea»** y las obras en vivo.
- **Iniciar sesión** con `carlos.mendez@construtrack.demo` / `jefe123`: ya no aparece el aviso de modo demostración.

## Ejecutar todo en local (NetBeans)

**Run** (F6) en el proyecto `ConstruTrack`: se abre la ventana de escritorio y el servidor queda en http://localhost:8080
(portada en `/` y sistema en `/app.html`). No hace falta internet.
