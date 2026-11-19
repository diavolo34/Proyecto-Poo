# Seguridad de ConstruTrack

Este documento resume cómo se protegen la aplicación y los datos. Tiene cinco partes:

1. Lo que ya está implementado.
2. Cómo se manejan las cookies y la sesión.
3. Una lista de verificación para operar el sistema de forma segura.
4. Las mejoras recomendadas.
5. Ideas para que la asistencia sea comprobable.

## 1. Lo que ya protege la aplicación

### Cuentas y claves
| Medida | Detalle |
|--------|---------|
| Claves cifradas con PBKDF2 | PBKDF2-HMAC-SHA256 con 600 000 iteraciones (la recomendación de OWASP) y una sal aleatoria de 128 bits por clave. La clave nunca se guarda en texto plano. |
| Migración automática | Las claves guardadas con el formato anterior (SHA-256) siguen funcionando. La primera vez que la persona entra, su clave se vuelve a cifrar con PBKDF2. |
| Claves nuevas más fuertes | Al registrarse, restablecer la clave o cuando el gerente asigna una: de 8 a 64 caracteres, con al menos una letra y un número. |
| Bloqueo por intentos | Tras 5 intentos fallidos con el mismo correo o cédula, se bloquea 15 minutos. El servidor responde `429` y el mensaje sugiere restablecer la clave. |
| Sin pistas para un atacante | El inicio de sesión responde lo mismo («correo, cédula o clave incorrectos») tanto si la cuenta existe como si no. Además, tarda lo mismo en ambos casos, porque se calcula un cifrado de relleno. |
| Correo verificado | Al registrarse llega un código de 6 dígitos al correo: vence a los 15 minutos, admite 5 intentos y se puede pedir 4 veces por hora. Sin confirmarlo, la cuenta no puede unirse a una empresa ni crearla, así que nadie puede usar un correo ajeno. Las cuentas anteriores a este cambio quedan como verificadas. |
| Verificación en dos pasos para gerentes | El gerente la activa en **Administración → Seguridad**: escanea un QR con Google Authenticator o Microsoft Authenticator (códigos TOTP del RFC 6238) y recibe 8 códigos de respaldo de un solo uso, por si pierde el teléfono. Desde entonces, además de la clave, se le pide el código al entrar, en la web y en la aplicación de escritorio. Un mismo código no sirve dos veces, y tras 5 códigos incorrectos hay que volver a empezar. Para desactivarla hacen falta la clave y un código. |
| Restablecer la clave por correo | «¿Olvidaste tu clave?» envía un enlace con un código aleatorio de 256 bits. El enlace sirve una sola vez, vence a los 30 minutos y se puede pedir como máximo 3 veces por hora. El servidor solo guarda la huella SHA-256 del código. Al usarlo, se cierran todas las sesiones de esa cuenta. La respuesta es la misma aunque el correo no exista. |

### Sesiones
- **Token de sesión:** al entrar, el servidor entrega un token aleatorio de 256 bits y guarda solo su huella SHA-256. Si alguien viera la memoria del servidor, no podría usar esos tokens.
- **Vencimiento:** cada sesión vence a las 12 horas. Cerrar sesión la borra del servidor.

### Permisos y datos
- **Permisos en el servidor:** cada acción se valida en el servidor según el rol (empleado, supervisor o gerente). Ocultar un botón en la pantalla no basta para saltarse un permiso.
- **Aislamiento entre empresas:** cada empresa ve y modifica solo sus propios datos. Hay pruebas automáticas que lo comprueban.
- **Validación de datos:** se validan cédula, correo, teléfono, nombres, fechas y horarios. Todo texto se escapa antes de mostrarlo en pantalla, para evitar la inyección de HTML.
- **Portada pública:** solo muestra cifras agregadas, sin nombres ni datos personales.
- **Documentos (reposos, vouchers, cartas de renuncia):** solo se aceptan fotos JPG, PNG o WEBP y PDF de hasta 5 MB, y el
  servidor comprueba el tipo real por los primeros bytes del archivo (no por la extensión). Se guardan en una carpeta
  privada del Google Drive de la empresa, sin enlaces públicos: el servidor los lee con la clave del script y solo se
  los muestra a quien tiene permiso. Los de Talento humano son **confidenciales**: solo los ve el gerente. El reposo lo
  ven el gerente, el supervisor que lo registró y la propia persona.
- **Aprobaciones con responsable:** cada permiso, reposo y reporte de proyecto guarda quién lo registró, quién lo aprobó o
  rechazó, cuándo y con qué nota (obligatoria). Solo el gerente aprueba; solo el supervisor registra.
- **Egresos:** al registrar un despido o una renuncia, la persona queda inactiva: su sesión abierta se cierra en la
  siguiente petición y ya no puede volver a entrar. Su historial se conserva; si se elimina la ficha, queda un
  expediente de texto en Talento humano.
- **Ubicación:** solo se pide al marcar la entrada o la salida, nunca en segundo plano. La ven el supervisor de la persona
  y el gerente, en un mapa de OpenStreetMap incrustado (la política CSP solo permite ese marco).

### Servidor y navegador
| Medida | Detalle |
|--------|---------|
| HTTPS | Netlify (página) y Render (servidor) solo atienden por HTTPS. HSTS obliga al navegador a usar siempre HTTPS. |
| CORS con lista blanca | La API solo responde a la página publicada (`construtrackfm.netlify.app`). |
| Política de seguridad de contenido (CSP) | La página solo ejecuta scripts propios: no hay scripts en línea ni de otros sitios. Solo se conecta con su propia API. Eso frena un ataque XSS aunque se colara texto malicioso. |
| Otras cabeceras de seguridad | `X-Frame-Options: DENY` (impide meter la app dentro de otra página), `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `Permissions-Policy` y `Cache-Control: no-store` en la API, para que los datos no queden en la caché. En Netlify se definen en `vista/web/_headers`. |
| Límite de tamaño | Ninguna petición puede pesar más de 64 KB, salvo la subida de un documento (hasta 5 MB; las fotos se reducen en el teléfono antes de enviarse). |
| Errores sin detalles | Si algo falla, el usuario ve un mensaje genérico; el detalle técnico queda solo en el registro del servidor. |
| Secretos fuera del código | El token de Upstash, la clave del script de Gmail y la clave de la demo están en variables de entorno de Render, nunca en GitHub. |
| Sin librerías externas | La aplicación no depende de paquetes de terceros, lo que reduce el riesgo de recibir código malicioso por una dependencia. |
| Demo privada | Solo entra quien tiene la clave. Se compara en tiempo constante y cada error espera 800 ms. |

## 2. Cookies y sesión

**ConstruTrack no usa cookies.**
- **Dónde va la sesión:** el token viaja en la cabecera `Authorization` y se guarda en el `sessionStorage` del navegador, que se borra al cerrar la pestaña.
- **No hace falta aviso de cookies:** no hay cookies de publicidad ni de rastreo.
- **Ventaja:** como el navegador no envía el token por su cuenta, la aplicación es inmune a los ataques CSRF (falsificación de peticiones entre sitios).
- **Riesgo:** si un atacante lograra ejecutar un script en la página (XSS), podría leer el token. Lo frenan la CSP estricta y el escape de todo texto.

**Si en el futuro se quisieran cookies**, la configuración segura es esta:

```
Set-Cookie: __Host-sesion=<token>; Secure; HttpOnly; SameSite=Strict; Path=/; Max-Age=43200
```

| Atributo | Para qué sirve |
|----------|----------------|
| `HttpOnly` | Ningún script puede leer la cookie, ni siquiera con XSS. |
| `Secure` | La cookie solo viaja por HTTPS. |
| `SameSite=Strict` | El navegador no la envía desde otros sitios, lo que corta el CSRF. |
| Prefijo `__Host-` | La cookie queda atada al dominio exacto. |
| `Max-Age` | La cookie vence sola. |

Para usar cookies, la API y la página tienen que estar en el mismo dominio. Si no, el navegador las trata como cookies de terceros y Safari o Firefox las bloquean. Se logra con un proxy de Netlify en el archivo `_redirects`:

```
/api/*  https://construtrack-api.onrender.com/api/:splat  200
```

## 3. Lista de verificación para operar el sistema

**Cuentas de servicio**
- [ ] Verificación en dos pasos (2FA) en GitHub, Render, Netlify, Upstash y en la cuenta de Google del script de correo.
- [ ] Claves largas y distintas en cada servicio, guardadas en un gestor de contraseñas.

**Secretos**
- [ ] Si se sospecha que un secreto se filtró, cambiarlo en Render y en el script: `GMAIL_SCRIPT_CLAVE`, el token de Upstash y `DEMO_CLAVE`.
- [ ] Nunca subir archivos `.env` ni copias de datos a GitHub (ya están en `.gitignore`).

**Datos**
- [ ] Copia de seguridad semanal de los datos de Upstash, guardada fuera de internet.
- [ ] Probar de vez en cuando que la copia se puede restaurar.

**Repositorio y servidor**
- [ ] Proteger la rama `main` en GitHub: exigir un pull request con el CI en verde.
- [ ] Volver a desplegar en Render cada pocos meses, para tomar la versión de Java con los últimos parches.
- [ ] Revisar el registro de Render si hay muchos `401` o `429`, que pueden indicar un ataque.

**Personas**
- [ ] Cuando alguien deja la empresa, el gerente lo elimina para que su cuenta deje de dar acceso.
- [ ] Explicar al equipo que ConstruTrack nunca pide la clave por correo ni por teléfono.

**Privacidad**
- [ ] Informar a los trabajadores qué datos se guardan (cédula, teléfono, asistencia) y para qué.
- [ ] Pedir solo los datos necesarios.

**Ante un incidente**
- [ ] Cambiar los secretos.
- [ ] Cerrar las sesiones reiniciando el servidor.
- [ ] Revisar el registro.
- [ ] Avisar a las personas afectadas.

## 4. Mejoras recomendadas

| Prioridad | Mejora | Por qué |
|-----------|--------|---------|
| Hecho | ~~Verificar el correo al registrarse~~ | Ver sección 1. |
| Hecho | ~~Verificación en dos pasos para gerentes~~ | Ver sección 1. |
| Media | **Aviso por correo cuando la clave cambia** | La persona se entera si no fue ella. |
| Media | **Sesión en cookie HttpOnly con el proxy de Netlify** (sección 2) | Un XSS ya no podría robar la sesión. |
| Media | **Registro de auditoría**: quién entró, quién aprobó o cambió qué, y desde dónde | Permite investigar un problema después. |
| Media | **Límite de intentos también por dirección IP** | Frena ataques que prueban muchas cuentas a la vez. |
| Baja | **Base de datos con cifrado en reposo y respaldos automáticos** | Hoy la conexión con Upstash ya va cifrada con HTTPS. |

## 5. Asistencia comprobable

**Ya está implementada** la combinación recomendada más abajo: geocerca, código QR rotativo y confirmación del supervisor.

**Configurar el proyecto.** En **Proyectos → Editar**, en «Control de asistencia», el supervisor o el gerente pueden:
- marcar «Exigir el código QR del proyecto»;
- escribir la latitud y la longitud del proyecto, o tocar «Usar mi ubicación actual» estando en él;
- fijar el radio, por ejemplo 150 m.

Cada parte es opcional. Un proyecto sin nada configurado funciona como antes.

**En el proyecto.** El supervisor toca «Mostrar QR de asistencia»: aparece un QR, con sus 6 dígitos escritos, que cambia cada 30 segundos. Se puede poner en pantalla completa en un teléfono o una tablet.

**Al marcar.** El trabajador puede hacerlo de tres formas:
- escanear el QR desde «Asistencia», donde el navegador lo permite;
- escanearlo con la cámara del teléfono, que abre la aplicación y marca sola después de entrar;
- escribir los 6 dígitos.

Si el proyecto tiene ubicación, el teléfono la envía al marcar. El servidor compara la distancia con el radio y deja un margen según la precisión del GPS. Un código incorrecto o vencido se rechaza para que lo vuelva a intentar.

**Marcas dudosas.** Una marca sin código, sin ubicación o lejos del proyecto se registra igual, pero queda **por confirmar**, con su motivo (por ejemplo, «Entrada a 1,3 km del proyecto»). En **Supervisión → Marcas por confirmar**, el supervisor de esa persona o el gerente la confirman o la rechazan. Una jornada rechazada no cuenta como asistencia: sus horas no suman y el día queda como falta.

## Ideas que se evaluaron

La marca siempre usó la hora del servidor, así que el trabajador no puede cambiar la hora de su celular para llegar «a tiempo». Pero, antes de este control, cualquiera con la clave de otro podía marcar por él desde su casa. Estas fueron las opciones evaluadas, de la más sencilla a la más completa:

| Idea | Cómo funciona | A favor | En contra |
|------|---------------|---------|-----------|
| **1. Geocerca (GPS)** | Cada proyecto guarda su ubicación y un radio, por ejemplo 150 m. Al marcar, el celular envía su ubicación. Si está fuera del radio, la marca queda «por revisar». | No necesita equipos; los celulares ya traen GPS. | El GPS se puede falsificar con aplicaciones y bajo techo pierde precisión. Solo se toma la ubicación al marcar, no se sigue a la persona. |
| **2. Código QR rotativo del proyecto** | Un teléfono o tablet del supervisor en el proyecto muestra un QR que cambia cada 30 segundos. Para marcar, el trabajador lo escanea con su celular. | Demuestra que la persona estaba frente al código, en la obra. Una foto del QR deja de servir en segundos. | Hace falta una pantalla en la obra. |
| **3. Código de 6 dígitos del proyecto** | La misma idea que el QR, pero el trabajador escribe un número que cambia cada minuto. | No necesita cámara y funciona en cualquier celular. | Alguien podría dictar el número por teléfono, aunque solo dura un minuto. |
| **4. Confirmación del supervisor** | Las marcas quedan pendientes hasta que el supervisor las confirma en una lista del día, igual que ya se hace con las horas extra. | Sencillo y con responsabilidad humana. | Le suma trabajo al supervisor. |
| **5. Celular registrado** | Cada persona solo puede marcar desde el teléfono que registró la primera vez. | Evita que un compañero marque por otro con su clave. | Si cambia de teléfono, el gerente tiene que autorizar el nuevo. |
| **6. Foto al marcar** | El celular toma una foto al marcar y el supervisor puede verla. | Disuade la suplantación. | Ocupa almacenamiento, toca datos sensibles (la cara) y alguien tiene que revisarla. |
| **7. Kiosco en la entrada** | Una tablet fija en el proyecto donde cada trabajador marca con su cédula y un PIN. | Controla el lugar y el equipo con que se marca. | Hay que comprar la tablet y cuidarla. |
| **8. Alertas de anomalías** | Se guarda desde qué dispositivo y red se marca, y se avisa al supervisor si algo no cuadra (dos personas en el mismo celular, marcas desde lejos). | Detecta abusos sin molestar a quien cumple. | No los impide, solo los señala. |

**Elegida e implementada:** la **geocerca (1)** más el **código QR rotativo (2)**, con los 6 dígitos (3) para quien no pueda escanear. Las marcas que no cumplen pasan a la **confirmación del supervisor (4)**. Quedan para más adelante el celular registrado (5), la foto (6), el kiosco (7) y las alertas de anomalías (8).
