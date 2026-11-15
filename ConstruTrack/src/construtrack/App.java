package construtrack;

import construtrack.controlador.web.ServidorWeb;
import construtrack.modelo.Empresa;
import construtrack.modelo.incidencias.FabricaIncidencias;
import construtrack.persistencia.GeneradorDatosPrueba;
import construtrack.persistencia.RepositorioArchivo;
import construtrack.servicio.Servicios;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Punto de entrada de ConstruTrack. Arma todas las piezas (composición de dependencias) y
 * levanta el servidor web.
 *
 * Variables de entorno opcionales:
 *   PORT          puerto HTTP (por defecto 8080; los hostings como Render lo definen solos)
 *   DATOS         archivo de datos (por defecto datos_construtrack.txt)
 *   CORS_ORIGEN   dominio autorizado a usar la API (por defecto *)
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) throws Exception {
        int puerto = Integer.parseInt(entorno("PORT", "8080"));
        Clock reloj = Clock.systemDefaultZone();

        FabricaIncidencias fabrica = FabricaIncidencias.conTiposPorDefecto();
        RepositorioArchivo repositorio = new RepositorioArchivo(
                Path.of(entorno("DATOS", "datos_construtrack.txt")), new GeneradorDatosPrueba(reloj, fabrica), fabrica);
        Empresa empresa = repositorio.cargar();
        Servicios servicios = new Servicios(empresa, repositorio, reloj, repositorio.siguienteIdIncidencia());

        ServidorWeb web = new ServidorWeb(servicios, puerto, entorno("CORS_ORIGEN", "*"));
        web.iniciar();
        System.out.println("ConstruTrack listo en " + web.getUrl()
                + " (" + empresa.listarUsuarios().size() + " usuarios)");
    }

    private static String entorno(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
