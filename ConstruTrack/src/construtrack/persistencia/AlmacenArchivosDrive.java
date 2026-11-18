package construtrack.persistencia;

import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.util.Json;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

public class AlmacenArchivosDrive implements AlmacenArchivos {
    private static final Logger LOG = Logger.getLogger(AlmacenArchivosDrive.class.getName());

    private final String urlScript;
    private final String clave;
    private final HttpClient cliente = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AlmacenArchivosDrive(String urlScript, String clave) {
        this.urlScript = urlScript;
        this.clave = clave;
    }

    @Override
    public String guardar(String nombre, String tipo, byte[] datos) {
        Map<String, String> respuesta = llamar("{\"clave\":" + Json.texto(clave) + ",\"accion\":\"subir\",\"nombre\":"
                + Json.texto(nombre) + ",\"tipo\":" + Json.texto(tipo) + ",\"datos\":"
                + Json.texto(Base64.getEncoder().encodeToString(datos)) + "}");
        String id = respuesta.get("id");
        if (id == null || !id.matches("[\\w-]{10,100}")) {
            throw new OperacionInvalidaException("Google Drive no devolvió el identificador del archivo.");
        }
        return id;
    }

    @Override
    public byte[] leer(String id) {
        if (!id.matches("[\\w-]{10,100}")) {
            throw new NoEncontradoException("El archivo no existe.");
        }
        Map<String, String> respuesta = llamar("{\"clave\":" + Json.texto(clave) + ",\"accion\":\"leer\",\"id\":" + Json.texto(id) + "}");
        try {
            return Base64.getDecoder().decode(respuesta.getOrDefault("datos", ""));
        } catch (IllegalArgumentException e) {
            throw new OperacionInvalidaException("Google Drive devolvió un archivo dañado.");
        }
    }

    private Map<String, String> llamar(String cuerpo) {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlScript))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        String texto;
        try {
            texto = cliente.send(peticion, HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException e) {
            throw new OperacionInvalidaException("No se pudo conectar con Google Drive. Intenta de nuevo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperacionInvalidaException("Se interrumpió la conexión con Google Drive.");
        }
        Map<String, String> datos;
        try {
            datos = Json.leerObjeto(texto.trim());
        } catch (IllegalArgumentException e) {
            LOG.warning(() -> "Respuesta inesperada del script de archivos (" + texto.length() + " caracteres)");
            throw new OperacionInvalidaException("El script de Google no respondió como se esperaba. Revisa que esté "
                    + "publicado como aplicación web con acceso para «Cualquier usuario».");
        }
        if (!"true".equals(datos.get("ok"))) {
            String error = datos.getOrDefault("error", "motivo desconocido");
            if (error.contains("No encontrado")) {
                throw new NoEncontradoException("El archivo ya no está en Google Drive.");
            }
            if (error.contains("Correo de destino") || error.contains("Acción desconocida")) {
                throw new OperacionInvalidaException("El script de Google está desactualizado: reemplázalo por la versión nueva de "
                        + "tools/correo-gmail/Codigo.gs, ejecuta «autorizar» para darle permiso de Drive y publica una nueva versión.");
            }
            throw new OperacionInvalidaException("Google Drive no guardó el archivo: " + error);
        }
        return datos;
    }

    @Override
    public String descripcion() {
        return "Google Drive";
    }
}
