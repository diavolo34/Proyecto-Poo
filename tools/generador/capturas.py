"""Toma las capturas de la web con el servidor Java en modo demostración.

Uso:   java -cp out construtrack.App --sin-ventana --demo      (con DEMO_CLAVE=... para la demo privada)
       python tools/generador/capturas.py [URL] [CLAVE_DEMO]
Salida: docs/capturas/web_*.png
"""
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
CLAVE_DEMO = sys.argv[2] if len(sys.argv) > 2 else ""
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
    pagina.wait_for_timeout(400)
    pagina.screenshot(path=SALIDA / f"{archivo}.png", full_page=completa)


def guardar(pagina, archivo, completa=False):
    pagina.wait_for_timeout(500)
    pagina.screenshot(path=SALIDA / f"{archivo}.png", full_page=completa)


with sync_playwright() as p:
    navegador = abrir_navegador(p)

    def nueva(**opciones):
        return navegador.new_context(**({"viewport": {"width": 1366, "height": 800}} | opciones)).new_page()

    pagina = nueva()
    pagina.goto(URL)
    pagina.wait_for_selector(".lp-obra")
    guardar(pagina, "web_01_portada")
    pagina.locator("#empezar").scroll_into_view_if_needed()
    guardar(pagina, "web_14_dos_formas")

    pagina.goto(URL + "/app.html#login")
    pagina.fill("#identificador", "10000001")
    pagina.fill("#clave", "mala")
    pagina.click("#form-login button[type=submit]")
    pagina.wait_for_selector("#form-login .mensaje-error:not(.oculto)")
    guardar(pagina, "web_02_login_error")

    pagina.goto(URL + "/app.html#registro?tipo=jefe")
    pagina.wait_for_selector("#r-empresa")
    pagina.fill("#r-empresa", "Constructora Los Andes")
    guardar(pagina, "web_03_registro")

    pagina.goto(URL + "/app.html#login")
    pagina.fill("#identificador", "rosa.gutierrez@correo.com")
    pagina.fill("#clave", "rosa1234")
    pagina.click("#form-login button[type=submit]")
    pagina.wait_for_selector("#form-unirse")
    guardar(pagina, "web_04_unirse")

    pagina = nueva()
    entrar(pagina, "30000001", "emp123")
    vista(pagina, "asistencia", "web_05_empleado_asistencia")
    vista(pagina, "incidencias", "web_06_incidencias")

    pagina = nueva()
    entrar(pagina, "20000001", "super123")
    vista(pagina, "tablero", "web_07_tablero")
    vista(pagina, "supervision", "web_08_supervision", completa=True)

    pagina = nueva()
    entrar(pagina, "carlos.mendez@construtrack.demo", "jefe123")
    vista(pagina, "personal", "web_09_personal")
    pagina.click("text=+ Registrar persona")
    pagina.wait_for_selector(".modal")
    guardar(pagina, "web_10_registrar_persona")
    pagina.keyboard.press("Escape")
    pagina.locator("tr", has_text="José García").get_by_text("Historial").click()
    pagina.wait_for_selector(".modal [data-pestana=h-incidencias]")
    pagina.click("[data-pestana=h-incidencias]")
    guardar(pagina, "web_15_historial")
    pagina.keyboard.press("Escape")
    vista(pagina, "obras", "web_16_obras")
    vista(pagina, "invitaciones", "web_11_invitaciones")
    pagina.click(".chat-boton")
    pagina.fill(".chat-formulario input", "¿Quién llegó tarde hoy?")
    pagina.press(".chat-formulario input", "Enter")
    guardar(pagina, "web_12_chatbot")

    movil = navegador.new_context(**p.devices["iPhone 13"]).new_page()
    movil.goto(URL)
    movil.wait_for_selector(".lp-obra")
    guardar(movil, "web_13_movil")

    if CLAVE_DEMO:
        pagina = nueva()
        pagina.goto(URL + "/app.html#demo")
        pagina.wait_for_selector("#clave-demo")
        guardar(pagina, "web_17_demo_privada")
    navegador.close()
print("Capturas en", SALIDA)
