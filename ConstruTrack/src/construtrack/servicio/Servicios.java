package construtrack.servicio;

import construtrack.modelo.Empresa;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;

/**
 * Punto único de acceso a los casos de uso. Las dos vistas (Swing y web) reciben este objeto,
 * así comparten exactamente el mismo modelo y las mismas reglas.
 */
public class Servicios {

    private final ServicioAutenticacion autenticacion;
    private final ServicioAsistencia asistencia;
    private final ServicioPersonal personal;

    public Servicios(Empresa empresa, EscritorDatos escritor, Clock reloj) {
        this.autenticacion = new ServicioAutenticacion(empresa, escritor, reloj);
        this.asistencia = new ServicioAsistencia(empresa, escritor, reloj);
        this.personal = new ServicioPersonal(empresa, escritor, reloj);
    }

    public ServicioAutenticacion autenticacion() {
        return autenticacion;
    }

    public ServicioAsistencia asistencia() {
        return asistencia;
    }

    public ServicioPersonal personal() {
        return personal;
    }
}
