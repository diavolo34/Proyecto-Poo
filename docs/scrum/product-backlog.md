# Product Backlog · ConstruTrack

Prioridad: **A** (alta, pedido por la profesora o imprescindible) · **M** (media) · **B** (baja).

| ID | Historia de usuario | Prioridad | Sprint | Estado |
|----|---------------------|-----------|--------|--------|
| HU-01 | Como **profesora**, quiero que el sistema esté organizado en MVC para evaluar el diseño por capas. | A | 1 | Hecho |
| HU-02 | Como **profesora**, quiero que se apliquen y justifiquen los principios SOLID. | A | 1, 6 | Hecho |
| HU-03 | Como **empleado**, quiero que mi historial de asistencia quede registrado para consultarlo. | A | 2 | Hecho |
| HU-04 | Como **empleado**, quiero reportar una falta por enfermedad con su reposo médico para que no cuente como inasistencia. | A | 2 | Hecho |
| HU-05 | Como **supervisor**, quiero aprobar o rechazar las incidencias de mi equipo. | A | 2 | Hecho |
| HU-06 | Como **supervisor**, quiero recibir alertas automáticas de retardos y faltas sin justificar. | A | 3 | Hecho |
| HU-07 | Como **supervisor**, quiero ver estadísticas de asistencia y puntualidad de mi equipo. | A | 3 | Hecho |
| HU-08 | Como **supervisor**, quiero descargar reportes en Excel (CSV) o texto. | A | 3 | Hecho |
| HU-09 | Como **usuario**, quiero una aplicación de escritorio (Swing) además de la web. | A | 4 | Hecho |
| HU-10 | Como **supervisor**, quiero un tablero por estados para ver qué falta por revisar. | M | 4, 5 | Hecho |
| HU-11 | Como **persona nueva**, quiero registrarme con mi correo y mis datos. | A | 5A | Hecho |
| HU-12 | Como **jefe**, quiero invitar a alguien con un código para que se una con el rol y la obra correctos. | A | 5A | Hecho |
| HU-13 | Como **profesora**, quiero usar la página publicada con el servidor real, no en modo demostración. | A | 5 | Hecho |
| HU-14 | Como **visitante**, quiero una portada que explique el sistema y me lleve a registrarme. | M | 5 | Hecho |
| HU-15 | Como **profesora**, quiero diagramas UML actualizados, con secuencias que usen solo clases del modelo. | A | 6 | Hecho |
| HU-16 | Como **profesora**, quiero que se justifiquen los principios de UX/UI aplicados. | A | 6 | Hecho |
| HU-17 | Como **equipo**, queremos pruebas automáticas que eviten regresiones. | M | 6 | Hecho |
| HU-18 | Como **jefe**, quiero que las invitaciones lleguen por correo electrónico real. | B | 7 | Hecho (desde Gmail con Apps Script) |
| HU-19 | Como **jefe** o **supervisor**, quiero gestionar las obras (crear, editar, asignar personas) desde la aplicación. | M | 7 | Hecho |
| HU-20 | Como **usuario**, quiero que mis datos no se pierdan cuando el servidor se reinicia. | A | 7 | Hecho (Upstash) |
| HU-21 | Como **supervisor**, quiero ver horas trabajadas y aprobar las horas extra (diarias y de días libres). | A | 7 | Hecho |
| HU-22 | Como **supervisor**, quiero las alertas ordenadas de la más antigua a la más reciente. | M | 7 | Hecho |
| HU-23 | Como **supervisor**, quiero ver en el Historial de cada persona sus incidencias, no solo su asistencia. | A | 7 | Hecho |
| HU-24 | Como **empleado**, quiero que la hora de mis marcas sea la de Venezuela. | A | 7 | Hecho |
| HU-25 | Como **jefe**, quiero que la cédula solo admita números para no duplicar personas. | M | 7 | Hecho |
| HU-26 | Como **jefe**, quiero crear mi empresa al registrarme e invitar a mi equipo; cada empresa ve solo sus datos. | A | 7 | Hecho |
| HU-27 | Como **visitante**, quiero una portada atractiva que muestre el producto y las dos formas de empezar. | M | 7 | Hecho |
| HU-28 | Como **dueño del producto**, quiero una versión para clientes sin datos de prueba y una demo privada con clave. | A | 7 | Hecho |
| HU-29 | Como **supervisor**, quiero que las incidencias resueltas salgan del tablero a las 3 h y sigan en los reportes. | M | 7 | Hecho |
| HU-30 | Como **jefe**, quiero marcar mi asistencia y aparecer destacado, junto a los supervisores, en la tabla de puntualidad. | A | 7 | Hecho |
| HU-31 | Como **usuario**, quiero restablecer mi clave por correo si la olvido. | A | 8 | Hecho |
| HU-32 | Como **dueño del producto**, quiero que las claves, las sesiones y la página estén protegidas contra ataques comunes. | A | 8 | Hecho |
| HU-33 | Como **jefe**, quiero comprobar que la marca de asistencia se hizo en la obra (ubicación o código de la obra). | A | 9 | Hecho |
| HU-34 | Como **dueño del producto**, quiero que cada cuenta confirme su correo antes de entrar a una empresa. | A | 9 | Hecho |
| HU-35 | Como **jefe**, quiero proteger mi cuenta con verificación en dos pasos. | A | 9 | Hecho |

## Criterios de aceptación (historias principales)

**HU-04 · Reposo médico**
- Puedo elegir «Reposo médico» y el formulario me pide el centro médico (y opcionalmente el certificado).
- Sin centro médico o sin descripción, el sistema no lo acepta y explica por qué.
- Mientras está pendiente, el día sigue contando como falta; al aprobarse, deja de contar.

**HU-05 · Revisar incidencias**
- Solo el supervisor del empleado o un jefe pueden revisar; otro supervisor recibe «No tienes autoridad…».
- Rechazar exige un motivo, que el trabajador ve.
- Una incidencia aprobada o rechazada ya no cambia de estado.

**HU-06 · Alertas**
- Con 3 o más llegadas tarde en el período aparece una alerta para ese empleado.
- Cada día laborable sin jornada y sin justificativo aprobado genera una alerta (alta si son 2 o más).
- Una incidencia sin resolver por más de 48 h genera una alerta.
- El supervisor solo ve alertas de su equipo; el empleado no tiene acceso.

**HU-12 · Invitaciones**
- El código tiene el formato CT-XXXX-XXXX, sirve una sola vez y vence en los días indicados (1 a 30).
- Al usarlo se crea la ficha con el rol, el supervisor y la obra de la invitación.
- Un código usado, cancelado o vencido muestra un mensaje que indica pedir uno nuevo.

**HU-13 · Página operativa**
- La página publicada nunca entra en «modo demostración».
- Si el servidor tarda, muestra que se está conectando y reintenta solo.
- Desde un dominio no autorizado, la API no responde (CORS).
