package construtrack.persistencia;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlmacenArchivosDriveTest {
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private final Deque<String> respuestas = new ArrayDeque<>();
    private HttpServer servidor;

    private String levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/exec", x -> {
            cuerpo.set(new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            x.getResponseHeaders().add("Location", "/echo");
            x.sendResponseHeaders(302, -1);
            x.close();
        });
        servidor.createContext("/echo", x -> {
            byte[] r = respuestas.removeFirst().getBytes(StandardCharsets.UTF_8);
            x.sendResponseHeaders(200, r.length);
            x.getResponseBody().write(r);
            x.close();
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/exec";
    }

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("Sube el documento en base64 con la clave y lo vuelve a leer por su id de Drive")
    void subirYLeer() throws IOException {
        AlmacenArchivosDrive drive = new AlmacenArchivosDrive(levantar(), "frase-secreta");
        byte[] pdf = "%PDF-1.4 prueba".getBytes(StandardCharsets.ISO_8859_1);
        respuestas.add("{\"ok\":true,\"id\":\"1AbCdEfGhIjKlMnOp\"}");
        assertEquals("1AbCdEfGhIjKlMnOp", drive.guardar("reposo.pdf", "application/pdf", pdf));
        assertTrue(cuerpo.get().contains("\"accion\":\"subir\"") && cuerpo.get().contains("\"clave\":\"frase-secreta\""));
        assertTrue(cuerpo.get().contains(Base64.getEncoder().encodeToString(pdf)));
        respuestas.add("{\"ok\":true,\"datos\":\"" + Base64.getEncoder().encodeToString(pdf) + "\"}");
        assertArrayEquals(pdf, drive.leer("1AbCdEfGhIjKlMnOp"));
    }

    @Test
    @DisplayName("Con el script anterior (solo correo) explica que hay que actualizarlo y darle permiso de Drive")
    void scriptDesactualizado() throws IOException {
        AlmacenArchivosDrive drive = new AlmacenArchivosDrive(levantar(), "frase-secreta");
        respuestas.add("{\"ok\":false,\"error\":\"Correo de destino no válido\"}");
        OperacionInvalidaException e = assertThrows(OperacionInvalidaException.class,
                () -> drive.guardar("reposo.pdf", "application/pdf", new byte[] {1, 2, 3}));
        assertTrue(e.getMessage().contains("desactualizado") && e.getMessage().contains("autorizar"), e.getMessage());
    }
}
