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
y la página muestra un aviso mientras arranca. Los datos vuelven a los de prueba cuando Render reinicia el servidor.

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
> Los datos viven en el disco temporal: al redeplegar se regeneran los datos de prueba.

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
