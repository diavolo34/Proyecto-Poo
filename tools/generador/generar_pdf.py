"""Genera el informe de entrega (PDF) a partir de datos.json, los diagramas, las capturas y docs/prompts.md.

Uso:  python tools/generador/generar_pdf.py
Salida: tools/generador/salida/ConstruTrack_Entrega_UML.pdf
(Editar datos.json para cambiar integrantes, URL de publicación, etc. y volver a ejecutar.)
"""
import html
import json
import re
from pathlib import Path
from playwright.sync_api import sync_playwright

AQUI = Path(__file__).resolve().parent
RAIZ = AQUI.parents[1]
DOCS = RAIZ / "docs"
SALIDA = AQUI / "salida"
DATOS = json.loads((AQUI / "datos.json").read_text(encoding="utf-8"))
PROMPTS = re.findall(r"```text\n(.*?)```", (DOCS / "prompts.md").read_text(encoding="utf-8"), re.S)

e = html.escape


def abrir_navegador(p):
    """Usa Google Chrome si está instalado; si no, el Chromium que trae Playwright."""
    try:
        return p.chromium.launch(channel="chrome")
    except Exception:
        return p.chromium.launch()


def figura(archivo, pie):
    return f'<figure><img src="{(DOCS / "diagramas" / "img" / archivo).as_uri()}" alt="{e(pie)}"><figcaption>{e(pie)}</figcaption></figure>'


def captura(archivo, pie):
    return f'<figure class="captura"><img src="{(DOCS / "capturas" / archivo).as_uri()}" alt="{e(pie)}"><figcaption>{e(pie)}</figcaption></figure>'


def tabla(encabezados, filas):
    cabeza = "".join(f"<th>{h}</th>" for h in encabezados)
    cuerpo = "".join("<tr>" + "".join(f"<td>{c}</td>" for c in fila) + "</tr>" for fila in filas)
    return f"<table><thead><tr>{cabeza}</tr></thead><tbody>{cuerpo}</tbody></table>"


url = DATOS["url_publicacion"].strip()
url_html = (f'<a href="{e(url)}">{e(url)}</a>' if url
            else '<span class="pendiente">PENDIENTE: colocar aquí la URL pública (GitHub Pages o Netlify) y volver a generar el PDF</span>')
integrantes = "".join(f"<li>{e(i)}</li>" for i in DATOS["integrantes"])
profesora = f'<p>Profesora: {e(DATOS["profesora"])}</p>' if DATOS["profesora"] else ""

requerimientos = tabla(["Requerimiento", "Cómo se cumple"], [
    ["PDF con Diagrama de Casos de Uso, Diagrama de Clases (Modelo de Dominio + Modelo Detallado) y Diagrama de Secuencia",
     "Secciones 4 a 7 de este documento (un diagrama de casos de uso, modelo de dominio, modelo detallado en dos partes y cuatro diagramas de secuencia)."],
    ["Dirección de publicación de la página web con el chatbot", "Sección 2. El chatbot «Ayuda RRHH» aparece en todas las pantallas (botón inferior derecho)."],
    ["Herramienta IA empleada y prompt utilizado", "Sección 3, con los dos prompts copiados textualmente."],
    ["Registro de usuarios (empleados): nombre, cédula, horario, rol y si está presente en la empresa",
     "Formulario «Registrar usuario» del Jefe. Clases <code>Usuario</code>, <code>Horario</code> y <code>RegistroAsistencia</code>; el estado <i>presente</i> cambia al marcar entrada o salida."],
    ["El Supervisor supervisa a los empleados: ve su horario y si entró / está activo en el trabajo",
     "«Panel de Supervisión»: horario, estado Presente/Ausente, hora de entrada de hoy, puntualidad e historial. Métodos <code>Supervisor.verEmpleadosACargo()</code> y <code>consultarAsistencia(e)</code>."],
    ["El Jefe es el editor y maneja a empleados y supervisores",
     "«Dashboard Gerencial»: registrar, editar (rol, horario, supervisor, presencia), eliminar y ver historial. Métodos <code>Jefe.registrarUsuario()</code>, <code>modificarUsuario()</code>, <code>eliminarUsuario()</code> y <code>asignarSupervisor()</code>."],
    ["Permisos por rol", "Validados en el servidor Java (<code>ControladorApi</code>): el Empleado no ve la lista de personal, el Supervisor solo ve a sus empleados y solo el Jefe puede modificar datos."],
])

actores = tabla(["Actor", "Descripción", "Casos de uso (además de los heredados)"], [
    ["Visitante", "Cualquier persona que abre la página.", "Consultar información de la empresa, Consultar chatbot."],
    ["Empleado", "Trabajador de la obra. Hereda de Visitante.", "Marcar entrada, Marcar salida, Consultar mi horario e historial (incluyen Iniciar sesión)."],
    ["Supervisor", "Hereda de Empleado.", "Supervisar empleados; extensión Consultar asistencia de un empleado {ver historial}."],
    ["Jefe", "Editor del sistema. Hereda de Supervisor (también ve la asistencia de todo el personal).",
     "Registrar, Modificar y Eliminar usuario (incluyen Validar datos; extensiones Asignar supervisor {rol = Empleado} y Liberar empleados a cargo {es Supervisor})."],
])

relaciones = tabla(["Relación", "Tipo", "Justificación"], [
    ["Empresa ◇— Usuario (1 a 0..*)", "Agregación", "La empresa registra a su personal, pero las personas existen aunque la empresa desaparezca del sistema."],
    ["Usuario ◆— Horario (1 a 1)", "Composición", "El horario pertenece a un único usuario y no tiene sentido sin él."],
    ["Usuario ◆— RegistroAsistencia (1 a 0..*)", "Composición", "Las jornadas de asistencia se eliminan junto con el usuario."],
    ["Usuario ◁— Empleado, Supervisor, Jefe", "Generalización", "Los tres «son un» Usuario: comparten cédula, nombre, horario y asistencia; cada uno agrega su comportamiento."],
    ["Supervisor — Empleado (0..1 a 0..*)", "Asociación «supervisa»", "Un supervisor tiene cero o más empleados a cargo; un empleado tiene cero o un supervisor."],
    ["Jefe — Empresa (1..* a 1)", "Asociación «administra»", "El jefe gestiona al personal de la empresa."],
    ["Chatbot — Usuario (0..* a 0..1)", "Asociación «atiende»", "El chatbot responde al visitante y, si hay sesión, al usuario según su rol."],
])

usuarios_prueba = tabla(["Rol", "Nombre", "Cédula", "Clave"], [
    ["Jefe", "Carlos Méndez", "10000001", "jefe123"],
    ["Supervisor", "Ana Rodríguez", "20000001", "super123"],
    ["Supervisor", "Luis Pérez", "20000002", "super123"],
    ["Empleado", "José García (y 5 más: 30000002 … 30000006)", "30000001", "emp123"],
])

documento = f"""<!DOCTYPE html>
<html lang="es"><head><meta charset="utf-8"><title>{e(DATOS['titulo'])}</title>
<style>
  @page {{ size: A4; margin: 18mm 16mm 18mm 16mm; }}
  @page horizontal {{ size: A4 landscape; margin: 12mm 12mm 16mm 12mm; }}
  * {{ box-sizing: border-box; }}
  body {{ font-family: "Segoe UI", Arial, sans-serif; color: #1e293b; font-size: 10.5pt; line-height: 1.45; margin: 0; }}
  h1 {{ font-size: 24pt; color: #1e3a8a; margin: 0 0 6pt; line-height: 1.15; }}
  h2 {{ font-size: 15pt; color: #1e3a8a; border-bottom: 2px solid #1e40af; padding-bottom: 3pt; margin: 0 0 10pt; }}
  h3 {{ font-size: 12pt; color: #1e40af; margin: 14pt 0 6pt; break-after: avoid; }}
  table {{ break-inside: avoid; }}
  p {{ margin: 0 0 7pt; }}
  section {{ break-before: page; }}
  .horizontal {{ page: horizontal; }}
  table {{ width: 100%; border-collapse: collapse; margin: 6pt 0 10pt; font-size: 9.5pt; }}
  th {{ background: #1e40af; color: white; text-align: left; padding: 5pt 6pt; }}
  td {{ border-bottom: 1px solid #cbd5e1; padding: 5pt 6pt; vertical-align: top; }}
  tr:nth-child(even) td {{ background: #f1f5f9; }}
  code {{ font-family: Consolas, monospace; font-size: 9pt; background: #eef2ff; padding: 0 2pt; border-radius: 2pt; }}
  figure {{ margin: 6pt 0; text-align: center; break-inside: avoid; }}
  figure img {{ max-width: 100%; max-height: 225mm; }}
  .horizontal figure img {{ max-height: 160mm; }}
  figcaption {{ font-size: 9pt; color: #475569; margin-top: 4pt; font-style: italic; }}
  .captura img {{ border: 1px solid #cbd5e1; max-height: 72mm; }}
  .casos img {{ max-height: 170mm; }}
  pre {{ font-family: Consolas, monospace; font-size: 7.6pt; line-height: 1.32; white-space: pre-wrap; background: #f8fafc;
         border: 1px solid #cbd5e1; border-left: 4px solid #1e40af; padding: 7pt 9pt; margin: 4pt 0 10pt; }}
  .portada {{ height: 255mm; display: flex; flex-direction: column; justify-content: space-between; }}
  .portada .institucion {{ font-size: 12pt; color: #334155; }}
  .portada .franja {{ height: 6pt; background: #1e40af; margin: 14pt 0; width: 70mm; }}
  .portada .sub {{ font-size: 13pt; color: #475569; }}
  .portada ul {{ list-style: none; padding: 0; margin: 4pt 0; font-size: 11pt; }}
  .dato {{ border: 1px solid #cbd5e1; border-radius: 6pt; padding: 8pt 10pt; margin: 6pt 0 10pt; background: #f8fafc; }}
  .dato b {{ color: #1e3a8a; }}
  .pendiente {{ background: #fef3c7; color: #92400e; padding: 1pt 4pt; font-weight: 600; }}
  .rejilla {{ display: grid; grid-template-columns: 1fr 1fr; gap: 8pt; }}
  .nota {{ font-size: 9pt; color: #475569; }}
</style></head><body>

<div class="portada">
  <div>
    <div class="institucion">{e(DATOS['universidad'])}<br>{e(DATOS['materia'])}</div>
    <div class="franja"></div>
    <h1>{e(DATOS['titulo'])}</h1>
    <p class="sub">{e(DATOS['subtitulo'])}</p>
    <p class="sub">Diagramas UML: Casos de Uso, Clases (Modelo de Dominio y Modelo Detallado) y Secuencia</p>
  </div>
  <div>
    <p><b>{'Integrante' if len(DATOS['integrantes']) == 1 else 'Integrantes'}</b></p><ul>{integrantes}</ul>
    {profesora}
    <p>{e(DATOS['fecha'])}</p>
  </div>
</div>

<section>
  <h2>1. Descripción del sistema</h2>
  <p>ConstruTrack es la página web de una empresa de construcción que gestiona al personal de sus obras. Está formada por un
  <b>servidor Java</b> (proyecto de NetBeans <code>ServidorObra</code>, sin librerías externas) y una <b>página web</b> con un
  <b>chatbot de Recursos Humanos</b>. El sistema registra a los empleados con su nombre, cédula, horario, rol y si están presentes en
  la empresa, controla la asistencia (entrada, salida y puntualidad) y ofrece un panel distinto para cada rol: Empleado, Supervisor y Jefe.</p>
  <h3>Requerimientos y cómo se cumplen</h3>
  {requerimientos}

  <h2 style="margin-top:16pt">2. Dirección de publicación de la página web</h2>
  <div class="dato"><b>Página publicada (con el chatbot):</b> {url_html}</div>
  <p>La página publicada funciona en <b>modo demostración</b>: como en internet no está disponible el servidor Java, el archivo
  <code>servidorLocal.js</code> aplica en el navegador las mismas reglas y rutas del servidor (los datos quedan en ese navegador).
  Al ejecutar el proyecto en NetBeans, la página se abre en <code>http://localhost:8080</code> y trabaja con el servidor Java
  (los datos se guardan en <code>datos_obra.txt</code>). Los usuarios de prueba están en la sección 9.</p>
</section>

<section>
  <h2>3. Herramienta IA y prompts utilizados</h2>
  <div class="dato"><b>Herramienta IA:</b> {e(DATOS['herramienta_ia'])}</div>
  <h3>Prompt 1: modificación de la página web (chatbot y registro de empleados)</h3>
  <pre>{e(PROMPTS[0].strip())}</pre>
  <h3>Prompt 2: diagramas UML y PDF</h3>
  <pre>{e(PROMPTS[1].strip())}</pre>
</section>

<section>
  <h2>4. Diagrama de Casos de Uso</h2>
  <div class="casos">{figura('casos_de_uso.svg', 'Figura 1. Diagrama de Casos de Uso del sistema ConstruTrack')}</div>
  {actores}
</section>

<section>
  <h2>5. Diagrama de Clases: Modelo de Dominio</h2>
  <p>Muestra solo las clases, sus relaciones, el nombre de cada relación y su cardinalidad.</p>
  {figura('clases_dominio.svg', 'Figura 2. Modelo de Dominio')}
  {relaciones}
</section>

<section class="horizontal">
  <h2>6. Diagrama de Clases: Modelo Detallado (1/2) · Clases del negocio</h2>
  {figura('clases_detallado_entidades.svg', 'Figura 3. Modelo Detallado: Usuario, Empleado, Supervisor, Jefe, Horario, RegistroAsistencia y Empresa')}
  <p class="nota">Notación: <code>-</code> private, <code>+</code> public, <code>#</code> protected, <code>~</code> paquete; subrayado = static; cursiva = abstract.</p>
</section>

<section class="horizontal">
  <h2>6. Diagrama de Clases: Modelo Detallado (2/2) · Servidor, persistencia, interfaz web y chatbot</h2>
  {figura('clases_detallado_control.svg', 'Figura 4. Modelo Detallado: clases del servidor Java y de la página web')}
  <p class="nota">Las clases marcadas «ver modelo 1/2» se detallan en la figura 3. <code>InterfazWeb</code>, <code>Api</code>, <code>Chatbot</code> y <code>ServidorLocal</code> corresponden a los archivos JavaScript de la página.</p>
</section>

<section>
  <h2>7. Diagramas de Secuencia</h2>
  {figura('secuencia_registrar_usuario.svg', 'Figura 5. Caso de uso Registrar usuario (Jefe)')}
</section>
<section>{figura('secuencia_marcar_entrada.svg', 'Figura 6. Caso de uso Marcar entrada (Empleado), que incluye Verificar puntualidad')}</section>
<section>{figura('secuencia_supervisar_empleados.svg', 'Figura 7. Caso de uso Supervisar empleados (Supervisor) con la extensión Consultar asistencia')}</section>
<section>{figura('secuencia_chatbot.svg', 'Figura 8. Caso de uso Consultar chatbot')}</section>

<section>
  <h2>8. Capturas de la página</h2>
  <div class="rejilla">
    {captura('01_inicio.png', 'Inicio: información, usuarios de prueba e inicio de sesión')}
    {captura('03_registro.png', 'Jefe: formulario de registro de usuario')}
    {captura('02_jefe.png', 'Jefe: Dashboard Gerencial con todo el personal')}
    {captura('04_supervisor.png', 'Supervisor: empleados a cargo, horario y presencia')}
    {captura('06_empleado.png', 'Empleado: marcar entrada/salida e historial')}
    {captura('05_chatbot.png', 'Chatbot respondiendo según el rol (Supervisor)')}
  </div>
</section>

<section>
  <h2>9. Ejecución y usuarios de prueba</h2>
  <h3>Cómo ejecutar el proyecto</h3>
  <p>1) Abrir el proyecto <code>ServidorObra</code> en NetBeans y ejecutarlo (Run / F6). 2) Abrir <code>http://localhost:8080</code> en el navegador.
  3) Entrar con un usuario de prueba de la sección 2. Para publicar la página basta con subir la carpeta <code>src/servidorobra/web</code>
  a GitHub Pages o Netlify.</p>
  <h3>Usuarios de prueba</h3>
  {usuarios_prueba}
  <p class="nota">En el modo demostración, el botón «Restablecer datos de prueba» devuelve los datos a este estado inicial.
  Con el servidor Java, basta con borrar el archivo <code>datos_obra.txt</code> y volver a ejecutar.</p>
</section>
</body></html>"""

SALIDA.mkdir(exist_ok=True)
ruta_html = SALIDA / "informe.html"
ruta_html.write_text(documento, encoding="utf-8")
ruta_pdf = SALIDA / "ConstruTrack_Entrega_UML.pdf"

pie = ('<div style="width:100%;font-size:8px;color:#64748b;font-family:Arial;padding:0 12mm;display:flex;justify-content:space-between">'
       f'<span>{e(DATOS["titulo"])}</span><span>Página <span class="pageNumber"></span> de <span class="totalPages"></span></span></div>')
with sync_playwright() as p:
    navegador = abrir_navegador(p)
    pagina = navegador.new_page()
    pagina.goto(ruta_html.as_uri())
    pagina.wait_for_load_state("networkidle")
    pagina.pdf(path=str(ruta_pdf), prefer_css_page_size=True, print_background=True,
               display_header_footer=True, header_template="<span></span>", footer_template=pie)
    navegador.close()
print("PDF generado:", ruta_pdf)
