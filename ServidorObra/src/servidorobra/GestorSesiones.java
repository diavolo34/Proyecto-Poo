package servidorobra;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controla qué usuario inició sesión. Cada sesión se identifica con un token aleatorio.
 */
public class GestorSesiones {

    private final Map<String, String> sesiones = new HashMap<>();

    /** Crea una sesión para el usuario y devuelve su token. */
    public String iniciarSesion(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        sesiones.put(token, usuario.getCedula());
        return token;
    }

    /** Devuelve el usuario dueño del token, o null si el token no es válido. */
    public Usuario obtenerUsuario(String token, Empresa empresa) {
        String cedula = token == null ? null : sesiones.get(token);
        return cedula == null ? null : empresa.buscarUsuario(cedula);
    }

    public void cerrarSesion(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }
}
