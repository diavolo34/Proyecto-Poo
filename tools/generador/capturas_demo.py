"""Captura la página publicada en modo demostración (sin servidor Java) para la guía de despliegue."""
import functools
import http.server
import threading
from pathlib import Path
from playwright.sync_api import sync_playwright

RAIZ = Path(__file__).resolve().parents[2]
WEB = RAIZ / "ServidorObra" / "src" / "servidorobra" / "web"
SALIDA = RAIZ / "docs" / "capturas"

manejador = functools.partial(http.server.SimpleHTTPRequestHandler, directory=str(WEB))
manejador.log_message = lambda *args: None
servidor = http.server.ThreadingHTTPServer(("127.0.0.1", 8091), manejador)
threading.Thread(target=servidor.serve_forever, daemon=True).start()


def abrir_navegador(p):
    """Usa Google Chrome si está instalado; si no, el Chromium que trae Playwright."""
    try:
        return p.chromium.launch(channel="chrome")
    except Exception:
        return p.chromium.launch()


with sync_playwright() as p:
    navegador = abrir_navegador(p)
    pagina = navegador.new_page(viewport={"width": 1280, "height": 800}, device_scale_factor=1.5)
    pagina.goto("http://127.0.0.1:8091/")
    pagina.wait_for_function("typeof Api !== 'undefined' && Api.modo !== null")
    pagina.wait_for_timeout(1200)
    pagina.evaluate("ChatUI.alternar()")
    pagina.screenshot(path=SALIDA / "demo_publicada.png")
    navegador.close()
servidor.shutdown()
print("Captura en", SALIDA / "demo_publicada.png")
