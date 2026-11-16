# Sprint 7 · De la prueba en vivo a la versión para clientes

**Objetivo:** corregir lo encontrado al usar la página publicada «como la profesora» y dejar ConstruTrack listo para que
cualquier constructora lo use.
**Historias:** HU-18 a HU-30.

## Sprint Backlog
- **Datos permanentes** (HU-20): `Almacen` con `AlmacenArchivo` y `AlmacenUpstash`; el plan gratuito de Render borraba
  el disco y con él las cuentas.
- **Errores encontrados en vivo:** números de incidencia repetidos, hora en UTC (las entradas salían como retardos),
  tabla que dejaba fuera a los usuarios nuevos y faltas contadas antes del ingreso.
- **Supervisión** (HU-21, HU-22, HU-23): horas trabajadas y esperadas, horas extra con aprobación, alertas por
  antigüedad, período de 7 días e Historial con incidencias.
- **Obras** (HU-19) y **cédula solo números** (HU-25).
- **Multiempresa** (HU-26): `Plataforma` como raíz del modelo; el jefe crea su empresa al registrarse y los servicios
  trabajan siempre dentro de la empresa de quien opera.
- **Correo real** (HU-18): `NotificadorGmail` con Google Apps Script (Brevo no aceptaba números de Venezuela).
- **Portada nueva** (HU-27), asistente **Arqui**, código sin comentarios.
- **Versión para clientes** (HU-28): arranque vacío, modo demostración opcional (`--demo`) y demo privada con clave
  en la misma página (`app.html#demo`).
- **Pulido final** (HU-29, HU-30): las incidencias aprobadas o rechazadas salen del tablero 3 horas después de
  revisarse y quedan en el reporte de incidencias (con «Todo el historial»); la asistencia se calcula desde la jornada
  abierta, lo que arregla la marca del jefe; el jefe y los supervisores aparecen primero y destacados en la tabla de
  puntualidad; y las opciones de rol del registro quedan alineadas en el celular.

## Incremento
PRs #3 a #10 fusionados en `main`, cada uno con el CI en verde, desplegados en Render y publicados en Netlify.
Copia de respaldo de la versión anterior (código y datos) entregada fuera del repositorio.

## Review
- Se probó la página publicada con los tres roles, en computadora y en celular, y se corrigió cada detalle encontrado.
- El aislamiento entre empresas se comprobó con pruebas JUnit específicas (un jefe no ve, revisa ni modifica nada de
  otra empresa) y con un recorrido en el navegador.
- Al quitar los comentarios se verificó que el Java compilado es idéntico byte a byte y que el JavaScript tiene el mismo
  árbol sintáctico.
- 57 pruebas JUnit y pruebas de humo de la API en modo demostración, producción y demo privada.

## Retrospectiva
- **Bien:** probar la página real como lo haría la profesora reveló errores que las pruebas unitarias no veían
  (zona horaria, datos que se borraban, columnas que no cabían en el celular).
- **Mejorar:** revisar antes las restricciones de los servicios externos (Render sin disco ni SMTP, Brevo sin Venezuela).
- **Para el futuro:** dominio propio para el correo y para la página, y una base de datos relacional si crecen los clientes.
