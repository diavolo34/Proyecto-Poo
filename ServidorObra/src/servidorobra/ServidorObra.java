package servidorobra;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

/**
 * Punto de entrada: carga los datos, levanta el servidor en el puerto 8080 y publica la página y la API.
 */
public class ServidorObra {

    public static void main(String[] args) throws Exception {
        // El puerto se puede cambiar con la variable de entorno PORT (útil al publicarlo en un hosting)
        String puertoEntorno = System.getenv("PORT");
        int puerto = puertoEntorno == null ? 8080 : Integer.parseInt(puertoEntorno);

        BaseDatos baseDatos = new BaseDatos("datos_obra.txt");
        Empresa empresa = baseDatos.cargar();
        GestorSesiones sesiones = new GestorSesiones();

        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);
        // Rutas que la página web consume
        server.createContext("/api/", new ControladorApi(empresa, baseDatos, sesiones));
        // La propia página web
        server.createContext("/", new ArchivosEstaticos());
        server.setExecutor(null);
        server.start();

        System.out.println("✅ Servidor Java nativo corriendo en el puerto " + puerto + ".");
        System.out.println("   Abre http://localhost:" + puerto + " en el navegador.");
        System.out.println("   Usuarios: " + empresa.listarUsuarios().size() + " (datos en datos_obra.txt)");
    }
}
