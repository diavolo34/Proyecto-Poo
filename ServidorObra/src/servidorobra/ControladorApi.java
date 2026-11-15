package servidorobra;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Atiende las peticiones /api/... que hace la página web y aplica los permisos de cada rol.
 *
 *   GET    /api/estado                 estado del servidor (sin sesión)
 *   POST   /api/login                  {cedula, clave}
 *   POST   /api/logout
 *   GET    /api/yo                     datos e historial del usuario que inició sesión
 *   POST   /api/asistencia/entrada     marcar entrada (también /api/obra/checkin)
 *   POST   /api/asistencia/salida      marcar salida
 *   GET    /api/usuarios               Jefe: todo el personal; Supervisor: sus empleados
 *   GET    /api/usuarios/{cedula}      detalle e historial de asistencia
 *   POST   /api/usuarios               Jefe: registrar usuario
 *   PUT    /api/usuarios/{cedula}      Jefe: modificar usuario
 *   DELETE /api/usuarios/{cedula}      Jefe: eliminar usuario
 */
public class ControladorApi implements HttpHandler {

    private final Empresa empresa;
    private final BaseDatos baseDatos;
    private final GestorSesiones sesiones;

    public ControladorApi(Empresa empresa, BaseDatos baseDatos, GestorSesiones sesiones) {
        this.empresa = empresa;
        this.baseDatos = baseDatos;
        this.sesiones = sesiones;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Cabeceras CORS para que la página funcione aunque se abra desde otro origen (por ejemplo file://)
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        int codigo = 200;
        String respuesta;
        try {
            synchronized (empresa) {
                respuesta = enrutar(exchange);
            }
        } catch (ErrorApi e) {
            codigo = e.codigo;
            respuesta = error(e.getMessage());
        } catch (IllegalArgumentException e) {
            codigo = 400;
            respuesta = error(e.getMessage());
        } catch (IllegalStateException e) {
            codigo = 409;
            respuesta = error(e.getMessage());
        } catch (SecurityException e) {
            codigo = 403;
            respuesta = error(e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            codigo = 500;
            respuesta = error("Error interno del servidor.");
        }
        enviar(exchange, codigo, respuesta);
    }

    private String enrutar(HttpExchange exchange) throws IOException {
        String metodo = exchange.getRequestMethod();
        String ruta = exchange.getRequestURI().getPath().replaceAll("/+$", "");

        if (ruta.equals("/api/estado") && metodo.equals("GET")) {
            return "{\"status\":\"ok\",\"servidor\":\"Java\",\"empresa\":" + Json.texto(empresa.getNombre()) + "}";
        }
        if (ruta.equals("/api/login") && metodo.equals("POST")) {
            Map<String, String> datos = Json.leerObjeto(leerCuerpo(exchange));
            Usuario usuario = empresa.autenticar(datos.get("cedula"), datos.get("clave"));
            if (usuario == null) {
                throw new ErrorApi(401, "Cédula o clave incorrecta.");
            }
            return "{\"token\":" + Json.texto(sesiones.iniciarSesion(usuario)) + ",\"usuario\":" + usuario.toJson(true) + "}";
        }

        // Todas las demás rutas necesitan una sesión iniciada
        Usuario actual = sesiones.obtenerUsuario(token(exchange), empresa);
        if (actual == null) {
            throw new ErrorApi(401, "Debes iniciar sesión.");
        }

        if (ruta.equals("/api/logout") && metodo.equals("POST")) {
            sesiones.cerrarSesion(token(exchange));
            return "{\"status\":\"ok\"}";
        }
        if (ruta.equals("/api/yo") && metodo.equals("GET")) {
            return actual.toJson(true);
        }
        if ((ruta.equals("/api/asistencia/entrada") || ruta.equals("/api/obra/checkin")) && metodo.equals("POST")) {
            RegistroAsistencia registro = actual.marcarEntrada();
            baseDatos.guardar(empresa);
            String mensaje = "Entrada registrada a las " + registro.getHoraEntrada() + (registro.isTarde() ? " (llegada tarde)." : ".");
            return "{\"mensaje\":" + Json.texto(mensaje) + ",\"usuario\":" + actual.toJson(true) + "}";
        }
        if (ruta.equals("/api/asistencia/salida") && metodo.equals("POST")) {
            RegistroAsistencia registro = actual.marcarSalida();
            baseDatos.guardar(empresa);
            String mensaje = "Salida registrada a las " + registro.getHoraSalida() + ".";
            return "{\"mensaje\":" + Json.texto(mensaje) + ",\"usuario\":" + actual.toJson(true) + "}";
        }

        if (ruta.equals("/api/usuarios")) {
            if (metodo.equals("GET")) {
                return listarUsuarios(actual);
            }
            if (metodo.equals("POST")) {
                Usuario nuevo = comoJefe(actual).registrarUsuario(Json.leerObjeto(leerCuerpo(exchange)));
                baseDatos.guardar(empresa);
                return nuevo.toJson(false);
            }
        }
        if (ruta.startsWith("/api/usuarios/")) {
            String cedula = ruta.substring("/api/usuarios/".length());
            switch (metodo) {
                case "GET":
                    return verUsuario(actual, cedula);
                case "PUT":
                    Usuario modificado = comoJefe(actual).modificarUsuario(cedula, Json.leerObjeto(leerCuerpo(exchange)));
                    baseDatos.guardar(empresa);
                    return modificado.toJson(false);
                case "DELETE":
                    comoJefe(actual).eliminarUsuario(cedula);
                    baseDatos.guardar(empresa);
                    return "{\"status\":\"ok\"}";
                default:
            }
        }
        throw new ErrorApi(404, "Ruta no encontrada: " + metodo + " " + ruta);
    }

    /** El Jefe ve a todo el personal; el Supervisor solo a sus empleados; el Empleado no tiene acceso. */
    private String listarUsuarios(Usuario actual) {
        List<? extends Usuario> lista;
        if (actual instanceof Jefe) {
            lista = empresa.listarUsuarios();
        } else if (actual instanceof Supervisor) {
            lista = ((Supervisor) actual).verEmpleadosACargo();
        } else {
            throw new SecurityException("Tu rol no permite ver la lista de personal.");
        }
        StringBuilder json = new StringBuilder("[");
        for (Usuario u : lista) {
            json.append(json.length() == 1 ? "" : ",").append(u.toJson(false));
        }
        return json.append("]").toString();
    }

    private String verUsuario(Usuario actual, String cedula) {
        Usuario usuario = empresa.buscarUsuario(cedula);
        if (usuario == null) {
            throw new ErrorApi(404, "No existe un usuario con la cédula " + cedula + ".");
        }
        if (actual instanceof Supervisor && usuario instanceof Empleado) {
            ((Supervisor) actual).consultarAsistencia((Empleado) usuario); // verifica que sea su empleado
        } else if (!(actual instanceof Jefe) && usuario != actual) {
            throw new SecurityException("No tienes permiso para ver a este usuario.");
        }
        return usuario.toJson(true);
    }

    private Jefe comoJefe(Usuario actual) {
        if (!(actual instanceof Jefe)) {
            throw new SecurityException("Solo el Jefe puede registrar, modificar o eliminar usuarios.");
        }
        return (Jefe) actual;
    }

    private String token(HttpExchange exchange) {
        String cabecera = exchange.getRequestHeaders().getFirst("Authorization");
        return cabecera != null && cabecera.startsWith("Bearer ") ? cabecera.substring(7) : null;
    }

    private String leerCuerpo(HttpExchange exchange) throws IOException {
        try (InputStream entrada = exchange.getRequestBody()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String error(String mensaje) {
        return "{\"error\":" + Json.texto(mensaje) + "}";
    }

    private void enviar(HttpExchange exchange, int codigo, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }

    /** Error con un código HTTP específico (401, 404...). */
    private static class ErrorApi extends RuntimeException {

        private static final long serialVersionUID = 1L;
        private final int codigo;

        ErrorApi(int codigo, String mensaje) {
            super(mensaje);
            this.codigo = codigo;
        }
    }
}
