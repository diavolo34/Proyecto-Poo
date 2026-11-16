# ConstruTrack · proyecto NetBeans

Proyecto Java SE (Ant) sin dependencias externas. La clase principal es `construtrack.App`.

## Ejecutar

1. Abrir esta carpeta en NetBeans (`File → Open Project…`).
2. **Run** (F6). Se abre la ventana de escritorio y el servidor queda en http://localhost:8080
   (portada en `/`, sistema en `/app.html`).
3. Los datos se guardan en `datos_construtrack.txt` (en esta carpeta). Si lo borras, se regeneran los datos de prueba.

Variables de entorno opcionales: `PORT` (puerto, 8080), `DATOS` (archivo de datos) y `CORS_ORIGEN` (dominios autorizados,
separados por coma). Argumento `--sin-ventana` para levantar solo el servidor.

## Pruebas

`test/construtrack/` contiene 48 pruebas JUnit 5. En NetBeans: *Test Project* (Alt+F6) con la biblioteca **JUnit 5**.
Por terminal:

```bash
javac --release 17 -encoding UTF-8 -d out $(find src -name "*.java")
javac --release 17 -encoding UTF-8 -cp out:junit-platform-console-standalone.jar -d out-test $(find test -name "*.java")
java -jar junit-platform-console-standalone.jar execute -cp out:out-test --scan-class-path
```

## Paquetes

| Paquete | Contenido |
|---------|-----------|
| `construtrack` | `App`: arma las dependencias y arranca las dos vistas |
| `modelo` | `Usuario` (abstracta), `Empleado`, `Supervisor`, `Jefe`, `Horario`, `Historial`, `RegistroAsistencia`, `Empresa`, `Obra`, `Periodo`, `Cuenta`, `Invitacion`, `CodigoInvitacion`, `Estadisticas` |
| `modelo.incidencias` | `Incidencia` (abstracta) y 5 tipos, `EstadoIncidencia`, `Gravedad`, `Justificable`, `FabricaIncidencias` |
| `modelo.alertas` | `ReglaAlerta` y 3 reglas, `Alerta`, `MonitorSupervision` |
| `modelo.reportes` | `Reporte` (Template Method) y 3 reportes, `Documento`, `FormateadorReporte` (CSV y texto) |
| `modelo.excepciones` | Errores del negocio (dato inválido, permiso denegado, no encontrado, operación inválida…) |
| `servicio` | Casos de uso: autenticación, asistencia, personal, incidencias, supervisión, registro, invitaciones |
| `persistencia` | `LectorDatos`, `EscritorDatos`, `RepositorioTexto`, `Almacen` (`AlmacenArchivo`, `AlmacenUpstash`), `RepositorioMemoria`, `GeneradorDatosPrueba` |
| `controlador.swing` | Una `Controladora` por pantalla y `AplicacionSwing` |
| `controlador.web` | `ServidorWeb`, `ManejadorApi`, `Enrutador`, controladores por recurso, `JsonMapper` |
| `vista.swing` | `Tema`, ventanas, paneles, diálogos y gráfico de barras |
| `vista/web` | `index.html` (portada), `app.html` (sistema), `css/estilos.css`, `js/` |
