"""Genera el informe de la entrega 2 en PDF a partir de la documentación del repositorio.

El PDF se arma con los mismos archivos que se ven en GitHub (docs/diseno, docs/scrum, docs/diagramas,
docs/capturas y docs/prompts.md), así el informe y el repositorio nunca se contradicen.

Uso:   python tools/generador/generar_pdf.py
Salida: tools/generador/salida/ConstruTrack_Entrega2.pdf
Requiere: pip install -r tools/generador/requirements.txt (markdown y playwright)
"""
import html
import json
import re
from pathlib import Path

import markdown
from playwright.sync_api import sync_playwright

AQUI = Path(__file__).resolve().parent
RAIZ = AQUI.parents[1]
DOCS = RAIZ / "docs"
SALIDA = AQUI / "salida"
DATOS = json.loads((AQUI / "datos.json").read_text(encoding="utf-8"))
e = html.escape


def md(archivo: Path, nivel_base: int = 1) -> str:
    """Convierte un Markdown a HTML: baja los títulos de nivel y resuelve las imágenes como rutas absolutas."""
    texto = archivo.read_text(encoding="utf-8")
    texto = re.sub(r"^(#+)", lambda m: "#" * min(6, len(m.group(1)) + nivel_base), texto, flags=re.M)
    cuerpo = markdown.markdown(texto, extensions=["tables", "fenced_code"])

    def absoluta(m):
        ruta = m.group(2)
        if ruta.startswith(("http", "file:", "data:")):
            return m.group(0)
        return f'{m.group(1)}"{(archivo.parent / ruta).resolve().as_uri()}"'
    return re.sub(r'(src=|href=)"([^"]+)"', absoluta, cuerpo)


def figura(ruta: Path, pie: str, clase: str = "") -> str:
    return f'<figure class="{clase}"><img src="{ruta.resolve().as_uri()}" alt="{e(pie)}"><figcaption>{e(pie)}</figcaption></figure>'


def abrir_navegador(p):
    """Usa Google Chrome si está instalado; si no, el Chromium que trae Playwright."""
    try:
        return p.chromium.launch(channel="chrome")
    except Exception:
        return p.chromium.launch()


CORRECCION = [
    ("Es Programación Orientada a Objetos, no Algoritmos III", "Materia corregida en todo el proyecto; el diseño se centra en POO: herencia, polimorfismo, interfaces, composición, objetos de valor y SOLID."),
    ("Darle profundidad al proyecto", "Se pasó de 13 a más de 100 clases en capas MVC: incidencias, historial, alertas, estadísticas, reportes, registro con correo, invitaciones, vista Swing, 26 pruebas y CI."),
    ("Sin gestores ni APIs en los diagramas de secuencia", "Los 6 diagramas de secuencia usan solo objetos de clases del modelo (Empleado, Historial, Incidencia, MonitorSupervision…)."),
    ("Página operativa, no solo modo demostración", "Se eliminó el modo demo. La página siempre usa el servidor Java: local en NetBeans o publicado en Render (Docker), con Netlify llamándolo por CORS."),
    ("¿Dónde está la clase Historial? ¿Dónde se registran las incidencias?", "Historial (composición en Usuario) guarda jornadas e incidencias. Incidencia es abstracta con 5 tipos (reposo médico, permiso, retardo, accidente, ausencia) y un ciclo de revisión."),
    ("Apoyar al supervisor: reportes, estadísticas e incidencias", "Alertas automáticas (retardos, faltas sin justificar, incidencias vencidas), estadísticas, tablero Kanban y reportes CSV/TXT."),
]

DIAGRAMAS = [
    ("casos_de_uso", "Diagrama de casos de uso"),
    ("clases_dominio", "Diagrama de clases · Modelo de dominio"),
    ("clases_nucleo", "Modelo detallado 1/4 · Núcleo"),
    ("clases_incidencias", "Modelo detallado 2/4 · Incidencias"),
    ("clases_supervision", "Modelo detallado 3/4 · Alertas, estadísticas y reportes"),
    ("clases_servicio_persistencia", "Modelo detallado 4/4 · Servicios y persistencia"),
    ("paquetes_mvc", "Paquetes · Arquitectura MVC"),
    ("secuencia_marcar_entrada", "Secuencia · Marcar entrada"),
    ("secuencia_reportar_incidencia", "Secuencia · Reportar incidencia"),
    ("secuencia_revisar_incidencia", "Secuencia · Revisar incidencia"),
    ("secuencia_evaluar_alertas", "Secuencia · Evaluar alertas"),
    ("secuencia_generar_reporte", "Secuencia · Generar reporte"),
    ("secuencia_unirse_empresa", "Secuencia · Unirse con invitación"),
    ("secuencia_crear_empresa", "Secuencia · Un jefe crea su empresa"),
]

CAPTURAS = [
    ("web_01_portada", "Portada pública"), ("web_02_login_error", "Inicio de sesión con mensaje de error"),
    ("web_03_registro", "Crear cuenta"), ("web_04_unirse", "Unirse con código de invitación"),
    ("web_05_empleado_asistencia", "Empleado: Mi asistencia"), ("web_06_incidencias", "Reportar incidencia (reposo médico)"),
    ("web_07_tablero", "Supervisor: tablero de incidencias"), ("web_08_supervision", "Supervisor: alertas, estadísticas y reportes"),
    ("web_09_personal", "Jefe: personal"), ("web_11_invitaciones", "Jefe: invitaciones"),
    ("swing_02_supervision", "Escritorio (Swing): supervisión"), ("swing_03_tablero", "Escritorio (Swing): tablero"),
]


def construir() -> str:
    url = DATOS["url_publicacion"]
    filas = "".join(f"<tr><td>{e(a)}</td><td>{e(b)}</td></tr>" for a, b in CORRECCION)
    scrum = md(DOCS / "scrum" / "README.md") + md(DOCS / "scrum" / "product-backlog.md") + "".join(
        md(DOCS / "scrum" / f"sprint-{n}.md", 2) for n in range(1, 7))
    prompts = md(DOCS / "prompts.md").split("<h2>Entrega 1</h2>")[0]
    diagramas = "".join(figura(DOCS / "diagramas" / "img" / f"{a}.svg", p, "diagrama") for a, p in DIAGRAMAS)
    capturas = "".join(figura(DOCS / "capturas" / f"{a}.png", p, "captura") for a, p in CAPTURAS)
    return f"""<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"><title>{e(DATOS['titulo'])}</title>
<style>
  @page {{ size: A4; margin: 18mm 16mm 20mm; }}
  body {{ font-family: Arial, sans-serif; font-size: 10.5pt; color: #172b4d; line-height: 1.45; }}
  h1 {{ font-size: 19pt; color: #0c66e4; border-bottom: 2px solid #0c66e4; padding-bottom: 4px; page-break-before: always; }}
  h1.sin-salto {{ page-break-before: avoid; }}
  h2 {{ font-size: 14pt; margin-top: 18px; }} h3 {{ font-size: 12pt; }} h4 {{ font-size: 11pt; }}
  table {{ border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 9.5pt; page-break-inside: auto; }}
  th, td {{ border: 1px solid #dcdfe4; padding: 5px 7px; vertical-align: top; text-align: left; }}
  th {{ background: #f1f2f4; }}
  code {{ background: #f1f2f4; padding: 0 3px; border-radius: 3px; font-size: 9pt; }}
  pre {{ background: #f7f8f9; border: 1px solid #dcdfe4; padding: 8px; white-space: pre-wrap; font-size: 8.5pt; }}
  img {{ max-width: 100%; }} p img {{ max-height: 520px; display: block; margin: 8px auto; border: 1px solid #dcdfe4; }}
  figure {{ margin: 12px 0; text-align: center; page-break-inside: avoid; }}
  figure.diagrama img {{ max-height: 235mm; }} figure.captura img {{ max-height: 120mm; border: 1px solid #dcdfe4; }}
  figcaption {{ font-size: 9pt; color: #44546f; margin-top: 4px; }}
  .portada {{ height: 250mm; display: flex; flex-direction: column; justify-content: center; text-align: center; }}
  .portada h1 {{ border: 0; font-size: 28pt; color: #172b4d; page-break-before: avoid; }}
  .portada .sub {{ font-size: 13pt; color: #44546f; }}
  .destacado {{ background: #e9f2ff; border-left: 4px solid #0c66e4; padding: 10px 14px; margin: 10px 0; }}
</style></head><body>
<section class="portada">
  <p>{e(DATOS['universidad'])}<br>{e(DATOS['materia'])}</p>
  <h1>{e(DATOS['titulo'])}</h1>
  <p class="sub">{e(DATOS['subtitulo'])}</p>
  <p><b>Integrante</b><br>{'<br>'.join(e(i) for i in DATOS['integrantes'])}</p>
  <p>{e(DATOS['fecha'])}</p>
</section>

<h1>1. Corrección de la entrega 1 y cómo se atendió</h1>
<table><tr><th>Observación de la profesora</th><th>Solución en la entrega 2</th></tr>{filas}</table>

<h2>Dirección de publicación y código</h2>
<div class="destacado">
  <p><b>Página publicada (con el chatbot):</b> <a href="{e(url)}">{e(url)}</a><br>
  <b>Repositorio:</b> <a href="{e(DATOS['repositorio'])}">{e(DATOS['repositorio'])}</a></p>
  <p>La página de Netlify llama al servidor Java publicado en Render. En local, al ejecutar el proyecto en NetBeans,
  se abren la ventana de escritorio y la web en http://localhost:8080. No existe modo demostración.</p>
</div>

<h2>Herramienta de IA</h2>
<p><b>{e(DATOS['herramienta_ia'])}</b></p>
{prompts}

<h1>2. Arquitectura MVC</h1>{md(DOCS / 'diseno' / 'mvc.md')}
<h1>3. Principios SOLID</h1>{md(DOCS / 'diseno' / 'solid.md')}
<h1>4. Principios UX/UI</h1>{md(DOCS / 'diseno' / 'ux-ui.md')}
<h1>5. Scrum</h1>{scrum}
<h1>6. Diagramas UML</h1>
<p>Los diagramas de secuencia usan solo objetos de clases del modelo, como pidió la corrección.</p>{diagramas}
<h1>7. Capturas del sistema</h1>{capturas}
<h1>8. Ejecución y usuarios de prueba</h1>
<p><b>NetBeans:</b> abrir el proyecto <code>ConstruTrack</code> → Run (F6). Requiere JDK 17 o superior.</p>
<table><tr><th>Rol</th><th>Correo</th><th>Cédula</th><th>Clave</th></tr>
<tr><td>Jefe</td><td>carlos.mendez@construtrack.demo</td><td>10000001</td><td>jefe123</td></tr>
<tr><td>Supervisora</td><td>ana.rodriguez@construtrack.demo</td><td>20000001</td><td>super123</td></tr>
<tr><td>Empleado</td><td>jose.garcia@construtrack.demo</td><td>30000001</td><td>emp123</td></tr>
<tr><td>Registrada sin empresa</td><td>rosa.gutierrez@correo.com</td><td>—</td><td>rosa1234</td></tr></table>
<p>Código de invitación de prueba: <b>CT-RQ7K-4M2P</b>.</p>
</body></html>"""


def main():
    SALIDA.mkdir(exist_ok=True)
    ruta_html = SALIDA / "informe.html"
    ruta_html.write_text(construir(), encoding="utf-8")
    ruta_pdf = SALIDA / "ConstruTrack_Entrega2.pdf"
    pie = ('<div style="width:100%;font-size:8px;color:#64748b;font-family:Arial;padding:0 12mm;display:flex;'
           f'justify-content:space-between"><span>{e(DATOS["titulo"])}</span>'
           '<span>Página <span class="pageNumber"></span> de <span class="totalPages"></span></span></div>')
    with sync_playwright() as p:
        navegador = abrir_navegador(p)
        pagina = navegador.new_page()
        pagina.goto(ruta_html.as_uri())
        pagina.wait_for_load_state("networkidle")
        pagina.pdf(path=str(ruta_pdf), prefer_css_page_size=True, print_background=True,
                   display_header_footer=True, header_template="<span></span>", footer_template=pie)
        navegador.close()
    print("PDF generado:", ruta_pdf)


if __name__ == "__main__":
    main()
