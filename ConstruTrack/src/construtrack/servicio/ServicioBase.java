package construtrack.servicio;

import construtrack.modelo.Empresa;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.util.function.Supplier;

abstract class ServicioBase {
    protected final Plataforma plataforma;
    protected final EscritorDatos escritor;
    protected final Clock reloj;

    protected ServicioBase(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        this.plataforma = plataforma;
        this.escritor = escritor;
        this.reloj = reloj;
    }

    protected Empresa empresaDe(Usuario actual) {
        if (actual == null || actual.getEmpresa() == null) {
            throw new PermisoDenegadoException("Aún no perteneces a una empresa.");
        }
        return actual.getEmpresa();
    }

    protected <T> T leer(Supplier<T> consulta) {
        synchronized (plataforma) {
            return consulta.get();
        }
    }

    protected <T> T modificar(Supplier<T> cambio) {
        synchronized (plataforma) {
            T resultado = cambio.get();
            escritor.guardar(plataforma);
            return resultado;
        }
    }
}
