package construtrack.servicio;

import construtrack.modelo.Empresa;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.util.function.Supplier;

/**
 * Comportamiento común de los servicios: acceso exclusivo a la empresa (Swing y la web la comparten
 * en la misma JVM) y guardado automático después de cada cambio.
 */
abstract class ServicioBase {

    protected final Empresa empresa;
    protected final EscritorDatos escritor;
    protected final Clock reloj;

    protected ServicioBase(Empresa empresa, EscritorDatos escritor, Clock reloj) {
        this.empresa = empresa;
        this.escritor = escritor;
        this.reloj = reloj;
    }

    /** Ejecuta una consulta sin modificar datos. */
    protected <T> T leer(Supplier<T> consulta) {
        synchronized (empresa) {
            return consulta.get();
        }
    }

    /** Ejecuta un cambio y, si termina bien, lo guarda. */
    protected <T> T modificar(Supplier<T> cambio) {
        synchronized (empresa) {
            T resultado = cambio.get();
            escritor.guardar(empresa);
            return resultado;
        }
    }
}
