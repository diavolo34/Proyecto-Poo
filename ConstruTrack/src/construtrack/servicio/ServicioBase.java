package construtrack.servicio;

import construtrack.modelo.Empresa;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.util.function.Supplier;

/**
 * Comportamiento común de los servicios: acceso exclusivo a la empresa (Swing y la web la comparten
 * en la misma JVM) y guardado automático después de cada cambio.
 */
abstract class ServicioBase {

    protected final Plataforma plataforma;
    protected final EscritorDatos escritor;
    protected final Clock reloj;

    protected ServicioBase(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        this.plataforma = plataforma;
        this.escritor = escritor;
        this.reloj = reloj;
    }

    /**
     * Empresa de quien hace la operación: cada consulta y cambio queda dentro de ella,
     * así un jefe nunca ve ni toca los datos de otra empresa.
     */
    protected Empresa empresaDe(Usuario actual) {
        if (actual == null || actual.getEmpresa() == null) {
            throw new PermisoDenegadoException("Aún no perteneces a una empresa.");
        }
        return actual.getEmpresa();
    }

    /** Ejecuta una consulta sin modificar datos. */
    protected <T> T leer(Supplier<T> consulta) {
        synchronized (plataforma) {
            return consulta.get();
        }
    }

    /** Ejecuta un cambio y, si termina bien, lo guarda. */
    protected <T> T modificar(Supplier<T> cambio) {
        synchronized (plataforma) {
            T resultado = cambio.get();
            escritor.guardar(plataforma);
            return resultado;
        }
    }
}
