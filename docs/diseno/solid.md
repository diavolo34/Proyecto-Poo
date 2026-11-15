# Principios SOLID en ConstruTrack

Cada principio se explica con **clases reales del proyecto** y, cuando aplica, con el **antes y después**
(la entrega 1 tenía todo en un paquete y varias clases «todólogas»). Al final, la sección del *arquitecto
pragmático* justifica dónde **no** se aplicaron abstracciones (YAGNI).

| Principio | Dónde se aplica | Beneficio en ConstruTrack |
|-----------|-----------------|---------------------------|
| **S** · Responsabilidad única | `JsonMapper`, controladores por recurso, `GeneradorDatosPrueba`, `Reporte` / `FormateadorReporte` | Cambiar el formato JSON o del CSV no toca el modelo |
| **O** · Abierto/cerrado | `ReglaAlerta`, `FormateadorReporte`, `FabricaIncidencias`, `ControladorWeb` | Nuevas alertas, formatos, tipos o rutas sin modificar lo existente |
| **L** · Sustitución de Liskov | `Usuario` → `Empleado`/`Supervisor`/`Jefe`; `Incidencia` → 5 tipos | Cualquier subtipo funciona donde se espera el tipo base |
| **I** · Segregación de interfaces | `RevisorIncidencias`, `Justificable`, `LectorDatos`/`EscritorDatos` | Ninguna clase implementa métodos que no le corresponden |
| **D** · Inversión de dependencias | Servicios ← `EscritorDatos`; `MonitorSupervision` ← reglas; `ServicioInvitaciones` ← `Notificador`; `Clock` | Pruebas con `RepositorioMemoria` y reloj fijo; correo real sin cambiar el servicio |

---

## S · Principio de responsabilidad única (SRP)

> «Una clase debe tener una, y solo una, razón para cambiar.»

**Antes (entrega 1):** cada entidad sabía convertirse a JSON (`Usuario.toJson()`, `Horario.toJson()`…). El modelo tenía
dos razones para cambiar: las reglas del negocio **y** el formato de la página web. Además, `ControladorApi`
(220 líneas) enrutaba, validaba sesiones, aplicaba permisos, serializaba y manejaba errores.

**Después:**

- `controlador/web/JsonMapper.java` es el único que conoce el formato JSON. Las entidades del modelo no importan nada de la web.
- `ControladorApi` se dividió: `Enrutador` (elige la ruta), `ManejadorApi` (CORS y traducción de excepciones a códigos HTTP)
  y un controlador por recurso (`ControladorSesion`, `ControladorIncidencias`, `ControladorReportes`…).
- `RepositorioArchivo` solo lee y escribe; los datos de prueba los crea `GeneradorDatosPrueba`.
- Igual que el ejemplo de la clase con `ReporteEmpleado`: **`Reporte` arma los datos** y **`FormateadorReporte` les da formato**.

```java
public abstract class Reporte {                        // responsabilidad: qué datos lleva el reporte
    public final Documento generar(List<Usuario> personas, Periodo periodo, LocalDate hoy) { ... }
}
public interface FormateadorReporte {                  // responsabilidad: cómo se ve (CSV, texto…)
    String formatear(Documento documento);
}
```

## O · Principio abierto/cerrado (OCP)

> «Abierto para la extensión, cerrado para la modificación.»

En lugar de una cadena de `if-else` (como el `ProcesadorDePagos` del material), se usan contratos:

```java
public interface ReglaAlerta {
    List<Alerta> evaluar(Usuario empleado, Periodo periodo, LocalDate hoy, LocalDateTime ahora);
}
// RetardosReiterados, AusenciaSinJustificar, IncidenciaPendienteVencida la implementan.
// MonitorSupervision recorre la lista de reglas y nunca pregunta "¿de qué tipo eres?".
```

Para agregar una alerta «horas extra excesivas» basta con una clase nueva y registrarla: `MonitorSupervision` no cambia.
Lo mismo con:

- `FormateadorReporte`: agregar XML o PDF es una clase más.
- `FabricaIncidencias`: un tipo nuevo («Vacaciones») se registra con `registrar(codigo, nombre, Vacaciones::new)`.
- `ControladorWeb`: cada controlador registra sus rutas en el `Enrutador`; agregar `/api/obras` no modifica el enrutador.

## L · Principio de sustitución de Liskov (LSP)

> «Los subtipos deben poder sustituir a su tipo base sin alterar la correctitud del programa.»

- Cualquier `Usuario` (Empleado, Supervisor o Jefe) puede marcar asistencia y reportar sus propias incidencias:
  `Usuario.marcarEntrada` y `Usuario.reportar` funcionan igual para los tres. Las subclases **amplían** lo que la base
  promete (por ejemplo, `Supervisor.puedeReportarPara` también acepta a su equipo), nunca lo restringen ni lanzan
  excepciones inesperadas.
- Toda `Incidencia` sigue el mismo ciclo (`registrarRevision` + `EstadoIncidencia`), así que `Historial`, el tablero y los
  reportes tratan las cinco subclases sin distinguirlas.

Se evitó la «herencia engañosa» del ejemplo `Cuadrado extends Rectangulo`: por eso `Ausencia` y `Retardo` **no** heredan
un método `justificaAusencia()` que tendrían que anular, sino que la capacidad vive en una interfaz aparte (ver ISP).

## I · Principio de segregación de interfaces (ISP)

> «Ningún cliente debe depender de métodos que no usa.»

- `RevisorIncidencias` (`puedeRevisar`, `revisar`) la implementan **solo** `Supervisor` y `Jefe`. `Empleado` no hereda
  métodos de revisión vacíos. La interfaz además decide qué pestañas ve cada quien (`AplicacionSwing`: `usuario instanceof RevisorIncidencias`).
- `Justificable` la implementan solo `ReposoMedico`, `PermisoPersonal` y `AccidenteLaboral`: son las únicas incidencias que
  pueden justificar una falta. `Historial.estaJustificado` pregunta por esa interfaz.
- La persistencia se separó en `LectorDatos` (cargar) y `EscritorDatos` (guardar): los servicios solo necesitan guardar.

## D · Principio de inversión de dependencias (DIP)

> «Los módulos de alto nivel dependen de abstracciones, no de detalles.»

```java
abstract class ServicioBase {
    protected final EscritorDatos escritor;   // abstracción, no RepositorioArchivo
    protected final Clock reloj;              // abstracción del tiempo
    protected ServicioBase(Empresa empresa, EscritorDatos escritor, Clock reloj) { ... }  // inyección por constructor
}
```

- En producción `App` inyecta `RepositorioArchivo`; en las pruebas, `RepositorioMemoria` (`ServiciosTest` comprueba que
  se guardó sin tocar el disco).
- `MonitorSupervision(List<ReglaAlerta> reglas)` recibe sus reglas; las pruebas le pasan reglas con otros umbrales.
- `ServicioInvitaciones` depende de `Notificador`. Hoy la implementación es `NotificadorConsola`; un `NotificadorCorreo`
  (SMTP) se conectaría sin cambiar el servicio, igual que el `IMensajero` del material.
- El reloj (`java.time.Clock`) también se inyecta: las pruebas usan `Clock.fixed` y obtienen siempre el mismo resultado.

`App.main` es la **raíz de composición**: el único lugar donde se eligen las clases concretas.

---

## El arquitecto pragmático: dónde NO se abstrajo (YAGNI)

Los principios son una guía, no una ley. Se evitó la «explosión de clases» en estos casos, con su justificación:

| Decisión | Por qué no se abstrajo más |
|----------|----------------------------|
| `Usuario.crear(rol, …)` usa un `switch` y no una fábrica registrable | Los roles son tres y forman parte del dominio; no se espera agregar roles en este proyecto. |
| No hay interfaz `RepositorioUsuarios` / `RepositorioIncidencias` por entidad | Todo se guarda en un solo archivo de la `Empresa`; separar por entidad no aporta hoy. |
| `Horario`, `Periodo` y `CodigoInvitacion` son clases concretas | Son objetos de valor simples; una interfaz no tendría una segunda implementación. |
| Un solo tipo de base de datos (archivo de texto) | Suficiente para la entrega; `LectorDatos`/`EscritorDatos` dejan abierta la puerta a otra sin preparar nada más. |

Cuando la necesidad sea real (por ejemplo, cientos de obras), se introducirán las abstracciones; no antes.
