# Principios SOLID en ConstruTrack

Cada principio se explica con **clases reales del proyecto** y, cuando aplica, con el **antes y después**
(la entrega 1 tenía todo en un paquete y varias clases «todólogas»). Al final, la sección del *arquitecto
pragmático* justifica dónde **no** se aplicaron abstracciones (YAGNI).

| Principio | Dónde se aplica | Beneficio en ConstruTrack |
|-----------|-----------------|---------------------------|
| **S** · Responsabilidad única | `JsonMapper`, controladores por recurso, `GeneradorDatosPrueba`, `Reporte` / `FormateadorReporte` | Cambiar el formato JSON o del CSV no toca el modelo |
| **O** · Abierto/cerrado | `ReglaAlerta`, `FormateadorReporte`, `FabricaNovedades`, `AlmacenArchivos`, `ControladorWeb` | Nuevas alertas, formatos, tipos o rutas sin modificar lo existente |
| **L** · Sustitución de Liskov | `Usuario` → `Empleado`/`Supervisor`/`Gerente`; `Novedad` → `Licencia`/`ReporteObra` → 5 tipos | Cualquier subtipo funciona donde se espera el tipo base |
| **I** · Segregación de interfaces | `RevisorNovedades`, `Justificable`, `LectorDatos`/`EscritorDatos` | Ninguna clase implementa métodos que no le corresponden |
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
  y un controlador por recurso (`ControladorSesion`, `ControladorNovedades`, `ControladorInventario`…).
- `RepositorioTexto` solo convierte la empresa a texto y de vuelta; **dónde** se guarda ese texto lo decide un `Almacen` (`AlmacenArchivo` o `AlmacenUpstash`), y los datos de prueba los crea `GeneradorDatosPrueba`.
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
// RetardosReiterados, AusenciaSinJustificar, FaltasConsecutivas y NovedadPendienteVencida la implementan.
// MonitorSupervision recorre la lista de reglas y nunca pregunta "¿de qué tipo eres?".
```

Para agregar una alerta «horas extra excesivas» basta con una clase nueva y registrarla: `MonitorSupervision` no cambia.
Así se sumó `FaltasConsecutivas` (dos faltas laborables seguidas sin justificar) sin tocar el monitor.
Lo mismo con:

- `FormateadorReporte`: agregar XML o PDF es una clase más.
- `FabricaNovedades`: un tipo nuevo se registra con `registrar(codigo, nombre, categoria, Clase::new)`; los códigos
  anteriores (`PERMISO_PERSONAL`, `AUSENCIA`) se leen con `alias`, sin modificar las clases.
- `AlmacenArchivos`: los documentos van a Google Drive (`AlmacenArchivosDrive`) o a una carpeta local; cambiar de
  proveedor es una clase nueva.
- `ControladorWeb`: cada controlador registra sus rutas en el `Enrutador`; agregar `/api/obras` no modifica el enrutador.

## L · Principio de sustitución de Liskov (LSP)

> «Los subtipos deben poder sustituir a su tipo base sin alterar la correctitud del programa.»

- Cualquier `Usuario` (Empleado, Supervisor o Gerente) puede marcar asistencia: `Usuario.marcarEntrada` funciona igual
  para los tres. Las subclases **ajustan** permisos con métodos pensados para eso (`puedeRegistrarNovedadPara`,
  `exigeUbicacionAlMarcar`), sin lanzar excepciones inesperadas.
- Toda `Novedad` sigue el mismo ciclo (`revisar` + `EstadoNovedad`), así que `Historial`, el tablero y los reportes
  tratan las cinco subclases sin distinguirlas.

Se evitó la «herencia engañosa» del ejemplo `Cuadrado extends Rectangulo`: por eso `Inasistencia` y `Retardo` **no** heredan
un método `justificaAusencia()` que tendrían que anular, sino que la capacidad vive en una interfaz aparte (ver ISP).

## I · Principio de segregación de interfaces (ISP)

> «Ningún cliente debe depender de métodos que no usa.»

- `RevisorNovedades` (`puedeRevisar`, `revisar`) la implementa **solo** `Gerente`: es quien aprueba o rechaza con nota.
  `Empleado` y `Supervisor` no heredan métodos de aprobación vacíos.
- `Justificable` la implementan solo `Licencia` (permiso laboral y reposo médico) y `AccidenteLaboral`: son los únicos
  registros que pueden justificar una falta. `Historial.justificacionDel` pregunta por esa interfaz, y la etiqueta del día
  («Incapacidad» para un reposo) la da cada clase con `etiquetaJustificacion()`.
- La persistencia se separó en `LectorDatos` (cargar) y `EscritorDatos` (guardar): los servicios solo necesitan guardar.

## D · Principio de inversión de dependencias (DIP)

> «Los módulos de alto nivel dependen de abstracciones, no de detalles.»

```java
abstract class ServicioBase {
    protected final EscritorDatos escritor;   // abstracción, no RepositorioTexto
    protected final Clock reloj;              // abstracción del tiempo
    protected ServicioBase(Empresa empresa, EscritorDatos escritor, Clock reloj) { ... }  // inyección por constructor
}
```

- En producción `App` inyecta `RepositorioTexto` con un `Almacen` (archivo en NetBeans, Upstash en Render); en las pruebas, `RepositorioMemoria` (`ServiciosTest` comprueba que
  se guardó sin tocar el disco).
- `MonitorSupervision(List<ReglaAlerta> reglas)` recibe sus reglas; las pruebas le pasan reglas con otros umbrales.
- `ServicioInvitaciones` depende de `Notificador`, igual que el `IMensajero` del material. Hay dos implementaciones:
  `NotificadorConsola` (en NetBeans, solo registra), `NotificadorGmail` (envía desde el Gmail del gerente con un
  Apps Script) y `NotificadorBrevo` (API de Brevo). Las dos reales comparten el contenido en `MensajeInvitacion` (SRP). Se agregó el correo real **sin cambiar una línea del servicio**: `App` elige cuál inyectar
  según la configuración (DIP y, a la vez, OCP).
- El reloj (`java.time.Clock`) también se inyecta: las pruebas usan `Clock.fixed` y obtienen siempre el mismo resultado.

`App.main` es la **raíz de composición**: el único lugar donde se eligen las clases concretas.

---

## El arquitecto pragmático: dónde NO se abstrajo (YAGNI)

Los principios son una guía, no una ley. Se evitó la «explosión de clases» en estos casos, con su justificación:

| Decisión | Por qué no se abstrajo más |
|----------|----------------------------|
| `Usuario.crear(rol, …)` usa un `switch` y no una fábrica registrable | Los roles son tres y forman parte del dominio; no se espera agregar roles en este proyecto. |
| No hay interfaz `RepositorioUsuarios` / `RepositorioNovedades` por entidad | Todo se guarda en un solo archivo de la `Empresa`; separar por entidad no aporta hoy. |
| `Horario`, `Periodo` y `CodigoInvitacion` son clases concretas | Son objetos de valor simples; una interfaz no tendría una segunda implementación. |
| Un solo tipo de base de datos (archivo de texto) | Suficiente para la entrega; `LectorDatos`/`EscritorDatos` dejan abierta la puerta a otra sin preparar nada más. |

Cuando la necesidad sea real (por ejemplo, cientos de obras), se introducirán las abstracciones; no antes.
