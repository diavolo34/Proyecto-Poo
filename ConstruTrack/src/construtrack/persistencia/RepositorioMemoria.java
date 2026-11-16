package construtrack.persistencia;

import construtrack.modelo.Empresa;
import construtrack.modelo.Plataforma;

/** Repositorio que no escribe en disco. Útil para pruebas y demostraciones. */
public class RepositorioMemoria implements LectorDatos, EscritorDatos {

    private final Plataforma plataforma;
    private int guardados;

    public RepositorioMemoria(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

    /** Atajo para una sola empresa: usa su plataforma. */
    public RepositorioMemoria(Empresa empresa) {
        this(empresa.getPlataforma());
    }

    @Override
    public Plataforma cargar() {
        return plataforma;
    }

    @Override
    public void guardar(Plataforma plataforma) {
        guardados++;
    }

    /** Cuántas veces se pidió guardar (permite comprobar en las pruebas que un servicio persiste sus cambios). */
    public int getGuardados() {
        return guardados;
    }
}
