# Diagramas UML

Fuentes en [PlantUML](https://plantuml.com/) (`*.puml`) y su render en `img/` (SVG para la web, PNG para el PDF).

| # | Diagrama | Fuente | Imagen |
|---|----------|--------|--------|
| 1 | Casos de uso | [`01_casos_de_uso.puml`](01_casos_de_uso.puml) | [SVG](img/casos_de_uso.svg) |
| 2 | Clases — Modelo de dominio | [`02_clases_dominio.puml`](02_clases_dominio.puml) | [SVG](img/clases_dominio.svg) |
| 3 | Clases — Detallado (entidades del negocio) | [`03_clases_detallado_entidades.puml`](03_clases_detallado_entidades.puml) | [SVG](img/clases_detallado_entidades.svg) |
| 4 | Clases — Detallado (servidor, persistencia, web y chatbot) | [`04_clases_detallado_control.puml`](04_clases_detallado_control.puml) | [SVG](img/clases_detallado_control.svg) |
| 5 | Secuencia — Registrar usuario (Jefe) | [`05_secuencia_registrar_usuario.puml`](05_secuencia_registrar_usuario.puml) | [SVG](img/secuencia_registrar_usuario.svg) |
| 6 | Secuencia — Marcar entrada (Empleado) | [`06_secuencia_marcar_entrada.puml`](06_secuencia_marcar_entrada.puml) | [SVG](img/secuencia_marcar_entrada.svg) |
| 7 | Secuencia — Supervisar empleados (Supervisor) | [`07_secuencia_supervisar_empleados.puml`](07_secuencia_supervisar_empleados.puml) | [SVG](img/secuencia_supervisar_empleados.svg) |
| 8 | Secuencia — Consultar chatbot | [`08_secuencia_chatbot.puml`](08_secuencia_chatbot.puml) | [SVG](img/secuencia_chatbot.svg) |

## Regenerar las imágenes

```bash
# Requiere Java y plantuml.jar (https://plantuml.com/download)
java -jar plantuml.jar -tsvg -o img docs/diagramas/*.puml
java -jar plantuml.jar -tpng -o img docs/diagramas/*.puml
```

> El nombre de cada imagen sale de la etiqueta `@startuml <nombre>` del archivo fuente.
