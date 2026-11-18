package construtrack.controlador.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

final class CabecerasSeguridad {
    static final String CSP_PAGINAS = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data: blob:; connect-src 'self' https://construtrack-api.onrender.com; font-src 'self'; "
            + "frame-src https://www.openstreetmap.org; "
            + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    private CabecerasSeguridad() {
    }

    static void api(HttpExchange exchange) {
        Headers cabeceras = comunes(exchange);
        cabeceras.set("Cache-Control", "no-store");
        cabeceras.set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
    }

    static void pagina(HttpExchange exchange) {
        comunes(exchange).set("Content-Security-Policy", CSP_PAGINAS);
    }

    private static Headers comunes(HttpExchange exchange) {
        Headers cabeceras = exchange.getResponseHeaders();
        cabeceras.set("X-Content-Type-Options", "nosniff");
        cabeceras.set("X-Frame-Options", "DENY");
        cabeceras.set("Referrer-Policy", "no-referrer");
        cabeceras.set("Permissions-Policy", "camera=(self), microphone=(), geolocation=(self), payment=()");
        if ("https".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("X-Forwarded-Proto"))) {
            cabeceras.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        return cabeceras;
    }
}
