package construtrack.servicio;

import construtrack.modelo.Invitacion;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.util.Json;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Logger;

public class NotificadorBrevo implements Notificador {
    public static final String API = "https://api.brevo.com/v3/smtp/email";
    private static final Logger LOG = Logger.getLogger(NotificadorBrevo.class.getName());

    private final String api;
    private final String clave;
    private final String remitente;
    private final MensajeInvitacion mensaje;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public NotificadorBrevo(String api, String clave, String remitente, String urlSitio) {
        this.api = api;
        this.clave = clave;
        this.remitente = remitente;
        this.mensaje = new MensajeInvitacion(urlSitio);
    }

    @Override
    public boolean envioReal() {
        return true;
    }

    @Override
    public void enviarInvitacion(Invitacion invitacion, String correoDestino) {
        String cuerpo = "{\"sender\":{\"name\":\"ConstruTrack\",\"email\":" + Json.texto(remitente) + "},"
                + "\"to\":[{\"email\":" + Json.texto(correoDestino) + "}],"
                + "\"subject\":" + Json.texto(mensaje.asunto(invitacion)) + ","
                + "\"htmlContent\":" + Json.texto(mensaje.html(invitacion, correoDestino)) + ","
                + "\"textContent\":" + Json.texto(mensaje.texto(invitacion, correoDestino)) + "}";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(api))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", clave)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() / 100 != 2) {
                LOG.warning(() -> "Brevo respondió " + respuesta.statusCode() + ": " + respuesta.body());
                throw new OperacionInvalidaException("El servicio de correo rechazó el envío ("
                        + respuesta.statusCode() + "). Revisa la configuración de Brevo.");
            }
            LOG.info(() -> "Invitación " + invitacion.getCodigo() + " enviada a " + correoDestino);
        } catch (IOException e) {
            throw new OperacionInvalidaException("No se pudo conectar con el servicio de correo. Intenta de nuevo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperacionInvalidaException("Se interrumpió el envío del correo.");
        }
    }
}
