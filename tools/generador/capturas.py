"""Toma capturas de la página ConstruTrack con el servidor Java corriendo.

Uso:  python tools/generador/capturas.py [URL]   (por defecto http://localhost:8080/)
Salida: docs/capturas/
"""
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080/"
SALIDA = Path(__file__).resolve().parents[2] / "docs" / "capturas"
SALIDA.mkdir(exist_ok=True)

ENTRAR = """async ([c, k]) => { await Api.cerrarSesion(); estado.usuario = await Api.iniciarSesion(c, k); await recargar(); }"""


def abrir_navegador(p):
    """Usa Google Chrome si está instalado; si no, el Chromium que trae Playwright."""
    try:
        return p.chromium.launch(channel="chrome")
    except Exception:
        return p.chromium.launch()


with sync_playwright() as p:
    navegador = abrir_navegador(p)
    pagina = navegador.new_page(viewport={"width": 1280, "height": 800}, device_scale_factor=1.5)
    pagina.goto(URL)
    pagina.wait_for_function("typeof Api !== 'undefined' && Api.modo !== null")
    pagina.wait_for_timeout(1200)  # carga de Tailwind

    pagina.evaluate("async () => { await Api.cerrarSesion(); estado.usuario = null; dibujar(); }")
    pagina.screenshot(path=SALIDA / "01_inicio.png")

    pagina.evaluate(ENTRAR, ["10000001", "jefe123"])
    pagina.screenshot(path=SALIDA / "02_jefe.png", full_page=True)

    pagina.evaluate("abrirFormulario(null)")
    pagina.fill("#form-usuario [name=nombre]", "Rosa Gutiérrez")
    pagina.fill("#form-usuario [name=cedula]", "25123456")
    pagina.fill("#form-usuario [name=clave]", "rosa1234")
    pagina.select_option("#form-usuario [name=supervisor]", "20000001")
    pagina.screenshot(path=SALIDA / "03_registro.png")
    pagina.evaluate("cerrarModal()")

    pagina.evaluate(ENTRAR, ["20000001", "super123"])
    pagina.screenshot(path=SALIDA / "04_supervisor.png", full_page=True)

    pagina.evaluate("""async () => {
        ChatUI.alternar();
        await ChatUI.enviar('¿Quién está presente?');
        await new Promise(r => setTimeout(r, 400));
        await ChatUI.enviar('¿Cuándo pagan la nómina?');
        await new Promise(r => setTimeout(r, 400));
    }""")
    pagina.screenshot(path=SALIDA / "05_chatbot.png")
    pagina.evaluate("ChatUI.alternar()")

    pagina.evaluate(ENTRAR, ["30000002", "emp123"])
    pagina.screenshot(path=SALIDA / "06_empleado.png", full_page=True)

    pagina.evaluate("async () => { await Api.cerrarSesion(); estado.usuario = null; dibujar(); }")
    navegador.close()
print("Capturas en", SALIDA)
