# Generador de documentación

Scripts en Python + [Playwright](https://playwright.dev/python/) que producen el informe de entrega en PDF y las capturas de pantalla.

| Script | Qué hace | Salida |
|--------|----------|--------|
| `generar_pdf.py` | Arma el informe (portada, prompts, requerimientos, diagramas, capturas) a partir de `datos.json`, `docs/diagramas`, `docs/capturas` y `docs/prompts.md` | `tools/generador/salida/` |
| `capturas.py [URL]` | Captura cada panel con el servidor Java corriendo (por defecto `http://localhost:8080/`) | `docs/capturas/` |
| `capturas_demo.py` | Levanta un servidor HTTP local y captura la página en modo demostración | `docs/capturas/demo_publicada.png` |

## Uso

```bash
pip install -r tools/generador/requirements.txt
python -m playwright install chromium   # solo si no tienes Google Chrome
python tools/generador/generar_pdf.py
```

Para cambiar integrantes, profesora o la URL publicada, edita `datos.json` y vuelve a generar.
La carpeta `salida/` está ignorada por git: el PDF es un artefacto generado.
