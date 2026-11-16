package construtrack.persistencia;

import construtrack.util.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Guarda los datos en una base Redis de Upstash mediante su API REST (sin librerías: java.net.http).
 * Sirve en servidores sin disco permanente, como el plan gratuito de Render, que borra los archivos al reiniciar.
 */
public class AlmacenUpstash implements Almacen {

    private static final Duration ESPERA = Duration.ofSeconds(15);

    private final String url;
    private final String token;
    private final String clave;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(ESPERA).build();

    public AlmacenUpstash(String url, String token, String clave) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.token = token;
        this.clave = URLEncoder.encode(clave, StandardCharsets.UTF_8);
    }

    @Override
    public Optional<String> leer() {
        HttpRequest peticion = base("/get/" + clave).GET().build();
        String respuesta = enviar(peticion);
        return Optional.ofNullable(Json.leerObjeto(respuesta).get("result"));
    }

    @Override
    public void escribir(String contenido) {
        // POST /set/clave con el valor en el cuerpo: admite cualquier texto, incluidos | y saltos de línea
        enviar(base("/set/" + clave).POST(HttpRequest.BodyPublishers.ofString(contenido, StandardCharsets.UTF_8)).build());
    }

    @Override
    public String descripcion() {
        return "Upstash Redis (" + URI.create(url).getHost() + ")";
    }

    private HttpRequest.Builder base(String ruta) {
        return HttpRequest.newBuilder(URI.create(url + ruta)).timeout(ESPERA).header("Authorization", "Bearer " + token);
    }

    private String enviar(HttpRequest peticion) {
        try {
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() != 200) {
                throw new IllegalStateException("Upstash respondió " + respuesta.statusCode() + ": " + respuesta.body());
            }
            return respuesta.body();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo conectar con Upstash", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió la conexión con Upstash", e);
        }
    }
}
