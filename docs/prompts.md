# Prompts de IA utilizados

Herramienta: **Claude (Anthropic)** a través de **Claude Code**.

Este archivo documenta, de forma transparente, los prompts empleados en la entrega.
`tools/generador/generar_pdf.py` lee los bloques ` ```text ` de este archivo para incluirlos en el PDF.

## Prompt 1 — Modificación de la página web (chatbot y registro de empleados)

```text
Actúa como desarrollador Java y web. Te adjunto mi proyecto de NetBeans "ServidorObra":
un servidor Java nativo (com.sun.net.httpserver.HttpServer, puerto 8080, sin librerías
externas) y una página index.html (Tailwind por CDN) de la empresa constructora
"ConstruTrack", con un chatbot de Recursos Humanos y un botón que registra la asistencia
llamando a POST /api/obra/checkin. Modifica el proyecto para cumplir lo siguiente,
manteniendo el estilo de la página y el chatbot:

1) REGISTRO DE USUARIOS (EMPLEADOS)
Cada usuario guarda: nombre, cédula (única, 6 a 9 dígitos), clave, horario (días de la
semana + hora de entrada + hora de salida), rol en la empresa (Empleado, Supervisor o
Jefe), si está presente en la empresa y su historial de asistencia (fecha, hora de
entrada, hora de salida y si llegó tarde, con 10 minutos de tolerancia). Cada Empleado
puede tener un Supervisor asignado.

2) ROLES Y PERMISOS (validados en el servidor, no solo en la página)
- Reemplaza el "Simulador de Rol" por un inicio de sesión con cédula y clave.
- EMPLEADO: ve sus datos, su horario y su historial; marca su ENTRADA y su SALIDA
  (esto actualiza "presente").
- SUPERVISOR: además supervisa a sus empleados: ve su horario, si están presentes,
  la hora en que entraron hoy y si llegaron puntuales o tarde; filtra presentes y
  ausentes y puede ver el historial de cada uno. No puede editar.
- JEFE: es el editor. Registra, modifica y elimina empleados y supervisores, cambia su
  rol y su horario, asigna supervisor y ve a todo el personal con su asistencia.
  No puede editar a otro Jefe. Pide confirmación antes de eliminar.

3) SERVIDOR JAVA (paquete servidorobra, programación orientada a objetos)
Clases: Usuario (abstracta) con subclases Empleado, Supervisor y Jefe; Horario;
RegistroAsistencia; Empresa (contiene a los usuarios); BaseDatos (guarda todo en un
archivo de texto datos_obra.txt y crea datos de prueba si no existe: 1 Jefe,
2 Supervisores y 6 Empleados); GestorSesiones (token por sesión); ControladorApi
(rutas REST en JSON con códigos 400/401/403/404/409); Json (utilidad para leer y
escribir JSON sin librerías); ArchivosEstaticos (sirve la página en
http://localhost:8080). Métodos clave: marcarEntrada(), marcarSalida(), estaPresente(),
esLlegadaTarde(hora), verEmpleadosACargo(), consultarAsistencia(empleado),
registrarUsuario(datos), modificarUsuario(cedula, datos), eliminarUsuario(cedula),
asignarSupervisor(empleado, cedulaSupervisor). Mantén compatible /api/obra/checkin.

4) PÁGINA WEB (carpeta src/servidorobra/web)
Separa el código en js/app.js (interfaz), js/api.js (acceso a datos), js/chatbot.js y
js/servidorLocal.js. Si la página no encuentra el servidor Java (por ejemplo, publicada
en GitHub Pages o Netlify), debe funcionar en "modo demostración": servidorLocal.js
reproduce en el navegador las mismas rutas y reglas del servidor, guardando los datos
en localStorage. Muestra una tabla con los usuarios de prueba en la página de inicio.
Diseño responsive (debe verse bien en celular) y todo en español.

5) CHATBOT
Conserva las respuestas de nómina y permisos y agrega seguridad, horario de oficina,
contacto y registro, con botones de preguntas sugeridas. Con sesión iniciada responde
según el rol: "mi horario", "marcar entrada", "marcar salida"; para Supervisor y Jefe
también "¿quién está presente?", "¿quién llegó tarde hoy?", "¿quién falta hoy?" y
"¿cuántos empleados hay?".

6) ENTREGA
Dame el código completo de cada archivo, los usuarios de prueba (cédula, clave, rol),
cómo ejecutarlo en NetBeans y cómo publicar la carpeta web para obtener la URL pública.
```

## Prompt 2 — Diagramas UML y PDF

```text
Con base en el código que acabas de generar (mismas clases, atributos, métodos y
rutas), elabora los diagramas UML del sistema en código PlantUML y genera un PDF con
ellos. Sigue las reglas de la materia (guías de Diagrama de Clases y de Secuencia):

A) DIAGRAMA DE CASOS DE USO
- Frontera del sistema con su nombre. Actores como roles (sustantivos): Visitante,
  Empleado, Supervisor y Jefe, con generalización entre actores (Empleado hereda de
  Visitante, Supervisor de Empleado y Jefe de Supervisor). La base de datos y el
  propio sistema NO son actores.
- Casos de uso en infinitivo; actor y caso de uso unidos con línea simple; entre casos
  de uso solo <<include>> (obligatorio, ej. Marcar entrada incluye Iniciar sesión y
  Verificar puntualidad) y <<extend>> (opcional, apuntando al caso base y con su
  PUNTO DE EXTENSIÓN, ej. Asignar supervisor extiende Registrar usuario {rol = Empleado}).

B) DIAGRAMA DE CLASES — MODELO DE DOMINIO
- Solo nombres de clases, relaciones, nombre/rol de la relación y cardinalidad.
- Agregación Empresa–Usuario; composición Usuario–Horario y Usuario–RegistroAsistencia;
  generalización Usuario {abstract} → Empleado, Supervisor, Jefe; asociaciones
  Supervisor "supervisa" Empleado, Jefe "administra" Empresa, Chatbot "atiende" Usuario.
  Justifica cada tipo de relación.

C) DIAGRAMA DE CLASES — MODELO DETALLADO (dos partes)
- Parte 1: clases del negocio. Parte 2: servidor, persistencia, interfaz web y chatbot.
- Atributos: modoAcceso nombre: Tipo [multiplicidad] = valorInicial.
- Métodos: modoAcceso nombre(param: Tipo): tipoRetorno (void si no retorna).
- Visibilidad - private, + public, # protected (atributos heredados), ~ paquete;
  constructores, getters y setters; static subrayado y abstract en cursiva.

D) DIAGRAMAS DE SECUENCIA (uno por caso de uso, con el nombre del caso en el título)
  1. Registrar usuario (Jefe)   2. Marcar entrada (Empleado)
  3. Supervisar empleados (Supervisor)   4. Consultar chatbot (Visitante/usuarios)
- Objetos con la forma objeto:Clase, todos presentes en el diagrama de clases;
  actor, líneas de vida, barras de activación, mensajes síncronos con parámetros y sus
  respuestas, <<create>> al crear objetos y fragmentos alt / opt / loop con la condición
  entre corchetes, cubriendo el escenario positivo y el negativo.

E) PDF DE ENTREGA
Portada; dirección de publicación de la página con el chatbot; herramienta IA y los
prompts usados; requerimientos cubiertos; los diagramas con una breve explicación;
capturas de la página y cómo ejecutarla con los usuarios de prueba.
```
