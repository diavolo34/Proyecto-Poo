package construtrack.controlador.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import construtrack.modelo.excepciones.CredencialesInvalidasException;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.DemasiadosIntentosException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManejadorApi implements HttpHandler {
    private static final Logger LOG = Logger.getLogger(ManejadorApi.class.getName());

    private final Enrutador enrutador = new Enrutador();
    private final GestorSesiones sesiones;
    private final String origenPermitido;
    private final String prefijo;

    public ManejadorApi(GestorSesiones sesiones, List<ControladorWeb> controladores, String origenPermitido) {
        this(sesiones, controladores, origenPermitido, "");
    }

    public ManejadorApi(GestorSesiones sesiones, List<ControladorWeb> controladores, String origenPermitido,
            String prefijo) {
        this.sesiones = sesiones;
        this.origenPermitido = origenPermitido;
        this.prefijo = prefijo;
        for (ControladorWeb controlador : controladores) {
            controlador.registrarRutas(enrutador);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CabecerasSeguridad.api(exchange);
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origenPara(exchange));
        exchange.getResponseHeaders().add("Vary", "Origin");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Access-Control-Expose-Headers", "Content-Disposition");
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        enviar(exchange, atender(new Peticion(exchange, sesiones, prefijo)));
    }

    private String origenPara(HttpExchange exchange) {
        if ("*".equals(origenPermitido)) {
            return "*";
        }
        String origen = exchange.getRequestHeaders().getFirst("Origin");
        for (String permitido : origenPermitido.split(",")) {
            if (permitido.trim().equals(origen)) {
                return origen;
            }
        }
        return origenPermitido.split(",")[0].trim();
    }

    private Respuesta atender(Peticion peticion) {
        try {
            return enrutador.atender(peticion);
        } catch (SesionRequeridaException | CredencialesInvalidasException e) {
            return Respuesta.error(401, e.getMessage());
        } catch (DemasiadosIntentosException e) {
            return Respuesta.error(429, e.getMessage());
        } catch (DatoInvalidoException | IllegalArgumentException e) {
            return Respuesta.error(400, e.getMessage());
        } catch (PermisoDenegadoException e) {
            return Respuesta.error(403, e.getMessage());
        } catch (NoEncontradoException e) {
            return Respuesta.error(404, e.getMessage());
        } catch (OperacionInvalidaException e) {
            return Respuesta.error(409, e.getMessage());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Error no controlado en " + peticion.ruta(), e);
            return Respuesta.error(500, "Ocurrió un error inesperado en el servidor. Intenta de nuevo.");
        }
    }

    private void enviar(HttpExchange exchange, Respuesta respuesta) throws IOException {
        byte[] bytes = respuesta.getCuerpo().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", respuesta.getTipo());
        if (respuesta.getNombreArchivo() != null) {
            exchange.getResponseHeaders().add("Content-Disposition",
                    "attachment; filename=\"" + respuesta.getNombreArchivo() + "\"");
        }
        exchange.sendResponseHeaders(respuesta.getCodigo(), bytes.length);
        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
