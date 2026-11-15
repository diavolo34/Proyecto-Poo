"""Toma las capturas de la web con el servidor Java corriendo (http://localhost:8080 por defecto).

Uso:   python tools/generador/capturas.py [URL]
Salida: docs/capturas/web_*.png
"""
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
SALIDA = Path(__file__).resolve().parents[2] / "docs" / "capturas"


def abrir_navegador(p):
    try:
        return p.chromium.launch(channel="chrome")
    except Exception:
        return p.chromium.launch()


def entrar(pagina, identificador, clave):
    pagina.goto(URL + "/app.html#login")
    pagina.fill("#identificador", identificador)
    pagina.fill("#clave", clave)
    pagina.click("#form-login button[type=submit]")
    pagina.wait_for_selector(".lateral")


def vista(pagina, hash_, archivo, completa=False):
    pagina.goto(f"{URL}/app.html#{hash_}")
    pagina.wait_for_function("document.querySelector('.titulo-vista') && !document.querySelector('#principal .girando')")
    pagina.screenshot(path=SALIDA / f"{archivo}.png", full_page=completa)


with sync_playwright() as p:
    navegador = abrir_navegador(p)
    nueva = lambda: navegador.new_context(viewport={"width": 1280, "height": 800}).new_page()
    pagina = nueva()
    pagina.goto(URL)
    pagina.wait_for_selector(".obra")
    pagina.screenshot(path=SALIDA / "web_01_portada.png")
    for identificador, clave, vistas in [
        ("30000001", "emp123", [("asistencia", "web_05_empleado_asistencia"), ("incidencias", "web_06_incidencias")]),
        ("20000001", "super123", [("tablero", "web_07_tablero"), ("supervision", "web_08_supervision")]),
        ("carlos.mendez@construtrack.demo", "jefe123", [("personal", "web_09_personal"), ("invitaciones", "web_11_invitaciones")]),
    ]:
        pagina = nueva()
        entrar(pagina, identificador, clave)
        for hash_, archivo in vistas:
            vista(pagina, hash_, archivo)
    navegador.close()
print("Capturas en", SALIDA)
