package construtrack.controlador.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ArchivosEstaticos implements HttpHandler {
    private static final String CARPETA = "/construtrack/vista/web";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CabecerasSeguridad.pagina(exchange);
        String ruta = exchange.getRequestURI().getPath();
        if (ruta.equals("/") || ruta.isEmpty()) {
            ruta = "/index.html";
        }
        InputStream archivo = ruta.contains("..") ? null : getClass().getResourceAsStream(CARPETA + ruta);
        if (archivo == null) {
            byte[] mensaje = "404 - Archivo no encontrado".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, mensaje.length);
            try (OutputStream salida = exchange.getResponseBody()) {
                salida.write(mensaje);
            }
            return;
        }
        try (InputStream entrada = archivo; OutputStream salida = exchange.getResponseBody()) {
            byte[] bytes = entrada.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", tipoContenido(ruta));
            exchange.sendResponseHeaders(200, bytes.length);
            salida.write(bytes);
        }
    }

    private String tipoContenido(String ruta) {
        if (ruta.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (ruta.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (ruta.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (ruta.endsWith(".png")) {
            return "image/png";
        }
        if (ruta.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (ruta.endsWith(".ico")) {
            return "image/x-icon";
        }
        if (ruta.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
