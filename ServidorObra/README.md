# ConstruTrack · ServidorObra

Página web de gestión de personal en obra con chatbot, registro de empleados y control de asistencia por roles.

## Ejecutar (NetBeans)

1. Abrir esta carpeta como proyecto en NetBeans y ejecutar (Run / F6).
2. Abrir **http://localhost:8080** en el navegador.
3. Los datos se guardan en `datos_obra.txt` (en la carpeta del proyecto). Si se borra, al iniciar se crean de nuevo los datos de prueba.

## Usuarios de prueba

| Rol        | Nombre        | Cédula   | Clave    |
|------------|---------------|----------|----------|
| Jefe       | Carlos Méndez | 10000001 | jefe123  |
| Supervisor | Ana Rodríguez | 20000001 | super123 |
| Supervisor | Luis Pérez    | 20000002 | super123 |
| Empleado   | José García   | 30000001 | emp123   |

Hay 6 empleados (cédulas 30000001 a 30000006, clave `emp123`).

## Publicar la página (URL pública)

Subir la carpeta `src/servidorobra/web` a **Netlify Drop** (app.netlify.com/drop) o a un repositorio de **GitHub Pages**.
Sin el servidor Java la página funciona en *modo demostración*: `js/servidorLocal.js` aplica las mismas reglas del servidor
y guarda los datos en el navegador.

## Estructura

```
src/servidorobra/
  ServidorObra.java        Punto de entrada: levanta el servidor en el puerto 8080
  ControladorApi.java      Rutas /api/... y permisos por rol
  ArchivosEstaticos.java   Sirve la página web
  GestorSesiones.java      Sesiones (token)
  BaseDatos.java           Lectura/escritura de datos_obra.txt y datos de prueba
  Json.java                Lectura/escritura de JSON sin librerías
  Usuario.java (abstracta), Empleado.java, Supervisor.java, Jefe.java
  Horario.java, RegistroAsistencia.java, Empresa.java
  web/
    index.html
    js/app.js              Interfaz (paneles de Empleado, Supervisor y Jefe)
    js/api.js              Acceso a datos (servidor Java o modo demostración)
    js/chatbot.js          Chatbot de RRHH
    js/servidorLocal.js    Modo demostración
```

## API

| Método | Ruta | Quién |
|--------|------|-------|
| GET | /api/estado | público |
| POST | /api/login · /api/logout | público / con sesión |
| GET | /api/yo | con sesión |
| POST | /api/asistencia/entrada (también /api/obra/checkin) · /api/asistencia/salida | con sesión |
| GET | /api/usuarios | Jefe (todos) · Supervisor (sus empleados) |
| GET | /api/usuarios/{cedula} | Jefe · Supervisor (sus empleados) · el propio usuario |
| POST · PUT · DELETE | /api/usuarios[/{cedula}] | Jefe |

> Las claves se guardan en texto plano porque es un proyecto académico de demostración.
