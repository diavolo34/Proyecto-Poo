package construtrack.controlador.web;

import com.sun.net.httpserver.HttpExchange;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.util.Json;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Peticion {
    public static final int TAMANO_MAXIMO = 64 * 1024;

    private final HttpExchange exchange;
    private final GestorSesiones sesiones;
    private final String prefijo;
    private final Map<String, String> parametrosRuta = new HashMap<>();
    private Map<String, String> cuerpo;

    Peticion(HttpExchange exchange, GestorSesiones sesiones) {
        this(exchange, sesiones, "");
    }

    Peticion(HttpExchange exchange, GestorSesiones sesiones, String prefijo) {
        this.exchange = exchange;
        this.sesiones = sesiones;
        this.prefijo = prefijo;
    }

    public String metodo() {
        return exchange.getRequestMethod();
    }

    public String ruta() {
        String ruta = exchange.getRequestURI().getPath().replaceAll("/+$", "");
        return !prefijo.isEmpty() && ruta.startsWith(prefijo + "/") ? ruta.substring(prefijo.length()) : ruta;
    }

    void agregarParametroRuta(String nombre, String valor) {
        parametrosRuta.put(nombre, valor);
    }

    public String parametro(String nombre) {
        return parametrosRuta.get(nombre);
    }

    public String consulta(String nombre, String porDefecto) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query != null) {
            for (String par : query.split("&")) {
                String[] kv = par.split("=", 2);
                if (kv[0].equals(nombre) && kv.length == 2) {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        }
        return porDefecto;
    }

    public static final int TAMANO_MAXIMO_ARCHIVOS = 8 * 1024 * 1024;

    public Map<String, String> cuerpo() {
        return cuerpo(TAMANO_MAXIMO);
    }

    public Map<String, String> cuerpo(int limite) {
        if (cuerpo == null) {
            try (InputStream entrada = exchange.getRequestBody()) {
                byte[] bytes = entrada.readNBytes(limite + 1);
                if (bytes.length > limite) {
                    throw new DatoInvalidoException(limite > TAMANO_MAXIMO
                            ? "El archivo es demasiado grande: el máximo es 5 MB." : "La petición es demasiado grande.");
                }
                String texto = new String(bytes, StandardCharsets.UTF_8);
                cuerpo = texto.isBlank() ? new HashMap<>() : Json.leerObjeto(texto);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return cuerpo;
    }

    public String token() {
        String cabecera = exchange.getRequestHeaders().getFirst("Authorization");
        return cabecera != null && cabecera.startsWith("Bearer ") ? cabecera.substring(7) : null;
    }

    public construtrack.modelo.Cuenta cuenta() {
        construtrack.modelo.Cuenta cuenta = sesiones.obtenerCuenta(token());
        if (cuenta == null) {
            throw new SesionRequeridaException();
        }
        return cuenta;
    }

    public Usuario usuario() {
        Usuario usuario = sesiones.obtenerUsuario(token());
        if (usuario != null && !usuario.estaActivo()) {
            sesiones.cerrarSesion(token());
            throw new SesionRequeridaException();
        }
        if (usuario == null) {
            if (sesiones.obtenerCuenta(token()) != null) {
                throw new construtrack.modelo.excepciones.PermisoDenegadoException(
                        "Aún no perteneces a la empresa. Ingresa el código de invitación que te dio tu gerente.");
            }
            throw new SesionRequeridaException();
        }
        return usuario;
    }
}
