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
import java.util.Map;
import java.util.logging.Logger;

/**
 * Envía la invitación desde el Gmail del jefe usando un Google Apps Script publicado como aplicación web
 * (tools/correo-gmail/Codigo.gs). No necesita cuentas de terceros ni tarjeta: Google permite unos 100 correos
 * al día. El script solo acepta pedidos que traen la clave secreta compartida.
 *
 * Otra implementación de Notificador: el servicio de invitaciones no cambia (inversión de dependencias).
 */
public class NotificadorGmail implements Notificador {

    private static final Logger LOG = Logger.getLogger(NotificadorGmail.class.getName());

    private final String urlScript;
    private final String clave;
    private final MensajeInvitacion mensaje;
    // Apps Script responde con una redirección (302) hacia el resultado: hay que seguirla
    private final HttpClient cliente = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * @param urlScript dirección /exec de la aplicación web de Apps Script
     * @param clave     clave secreta escrita también en el script
     * @param urlSitio  dirección pública de la página, para el botón «Unirme»
     */
    public NotificadorGmail(String urlScript, String clave, String urlSitio) {
        this.urlScript = urlScript;
        this.clave = clave;
        this.mensaje = new MensajeInvitacion(urlSitio);
    }

    @Override
    public boolean envioReal() {
        return true;
    }

    @Override
    public void enviarInvitacion(Invitacion invitacion, String correoDestino) {
        String cuerpo = "{\"clave\":" + Json.texto(clave)
                + ",\"para\":" + Json.texto(correoDestino)
                + ",\"asunto\":" + Json.texto(mensaje.asunto(invitacion))
                + ",\"html\":" + Json.texto(mensaje.html(invitacion, correoDestino))
                + ",\"texto\":" + Json.texto(mensaje.texto(invitacion, correoDestino)) + "}";
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
        LOG.info(() -> "Invitación " + invitacion.getCodigo() + " enviada por Gmail a " + correoDestino);
    }
}
