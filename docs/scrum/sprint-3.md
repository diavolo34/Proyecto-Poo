# Sprint 3 · Apoyo automatizado al supervisor

**Objetivo:** que el sistema trabaje para el supervisor: alertas, estadísticas y reportes.
**Historias:** HU-06, HU-07, HU-08.

## Sprint Backlog
- `ReglaAlerta` + `RetardosReiterados`, `AusenciaSinJustificar`, `IncidenciaPendienteVencida`; `MonitorSupervision` con reglas inyectadas.
- `Estadisticas` (asistencia, puntualidad, ranking, incidencias por tipo).
- `Reporte` (Template Method) con 3 reportes y `FormateadorReporte` (CSV y texto).
- `ServicioSupervision` y rutas `/api/supervision/alertas`, `/api/estadisticas`, `/api/reportes/{tipo}`.

## Incremento
Alertas y reportes descargables según el rol. Datos de demo con un empleado con 3 retardos para mostrar la alerta.

## Review
Funcionó localmente, pero **el CI falló**: la regla `reportes/` del `.gitignore` también ignoraba el paquete
`modelo/reportes`, y esas clases no se subieron.

## Retrospectiva
- **Mal:** compilar solo en la máquina local escondió el error.
- **Cambio adoptado:** antes de cada push se compila **desde una copia limpia de git** (`git archive`) y se corre la prueba de humo. Se mantuvo en los siguientes sprints.
