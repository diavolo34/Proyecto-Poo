package construtrack.servicio;

import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.util.Json;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

public class NotificadorGmail extends NotificadorCorreo {
    private static final Logger LOG = Logger.getLogger(NotificadorGmail.class.getName());

    private final String urlScript;
    private final String clave;

    private final HttpClient cliente = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public NotificadorGmail(String urlScript, String clave, String urlSitio) {
        super(urlSitio);
        this.urlScript = urlScript;
        this.clave = clave;
    }

    @Override
    protected void enviar(Correo correo) {
        String cuerpo = "{\"clave\":" + Json.texto(clave)
                + ",\"para\":" + Json.texto(correo.para())
                + ",\"asunto\":" + Json.texto(correo.asunto())
                + ",\"html\":" + Json.texto(correo.html())
                + ",\"texto\":" + Json.texto(correo.texto()) + "}";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlScript))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        String respuesta;
        try {
            respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException e) {
            throw new OperacionInvalidaException("No se pudo conectar con el envío de correos de Gmail. Intenta de nuevo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperacionInvalidaException("Se interrumpió el envío del correo.");
        }
        Map<String, String> datos;
        try {
            datos = Json.leerObjeto(respuesta.trim());
        } catch (IllegalArgumentException e) {
            LOG.warning(() -> "Respuesta inesperada del script de Gmail: " + respuesta);
            throw new OperacionInvalidaException("El script de Gmail no respondió como se esperaba. "
                    + "Revisa que esté implementado como aplicación web con acceso para «Cualquier usuario».");
        }
        if (!"true".equals(datos.get("ok"))) {
            throw new OperacionInvalidaException("Gmail no envió el correo: " + datos.getOrDefault("error", "motivo desconocido"));
        }
    }
}
