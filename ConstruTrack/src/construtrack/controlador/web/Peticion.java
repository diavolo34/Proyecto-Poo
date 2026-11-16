package construtrack.controlador.web;

import com.sun.net.httpserver.HttpExchange;
import construtrack.modelo.Usuario;
import construtrack.util.Json;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Datos de una petición HTTP ya interpretados: ruta, parámetros, cuerpo JSON y usuario de la sesión. */
public class Peticion {

    private final HttpExchange exchange;
    private final GestorSesiones sesiones;
    private final Map<String, String> parametrosRuta = new HashMap<>();
    private Map<String, String> cuerpo;

    Peticion(HttpExchange exchange, GestorSesiones sesiones) {
        this.exchange = exchange;
        this.sesiones = sesiones;
    }

    public String metodo() {
        return exchange.getRequestMethod();
    }

    public String ruta() {
        return exchange.getRequestURI().getPath().replaceAll("/+$", "");
    }

    void agregarParametroRuta(String nombre, String valor) {
        parametrosRuta.put(nombre, valor);
    }

    /** Valor de un segmento variable de la ruta, por ejemplo {cedula} en /api/usuarios/{cedula}. */
    public String parametro(String nombre) {
        return parametrosRuta.get(nombre);
    }

    /** Valor de un parámetro de consulta (?clave=valor), o el valor por defecto. */
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

    /** Cuerpo JSON como mapa de textos (se lee una sola vez). */
    public Map<String, String> cuerpo() {
        if (cuerpo == null) {
            try (InputStream entrada = exchange.getRequestBody()) {
                String texto = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
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

    /** Cuenta personal con sesión iniciada (haya o no canjeado una invitación); lanza 401 si no hay. */
    public construtrack.modelo.Cuenta cuenta() {
        construtrack.modelo.Cuenta cuenta = sesiones.obtenerCuenta(token());
        if (cuenta == null) {
            throw new SesionRequeridaException();
        }
        return cuenta;
    }

    /** Persona de la empresa con sesión iniciada; lanza 401 si no hay sesión. */
    public Usuario usuario() {
        Usuario usuario = sesiones.obtenerUsuario(token());
        if (usuario == null) {
            if (sesiones.obtenerCuenta(token()) != null) {
                throw new construtrack.modelo.excepciones.PermisoDenegadoException(
                        "Aún no perteneces a la empresa. Ingresa el código de invitación que te dio tu jefe.");
            }
            throw new SesionRequeridaException();
        }
        return usuario;
    }
}
