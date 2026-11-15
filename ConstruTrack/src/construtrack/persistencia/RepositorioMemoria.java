package construtrack.persistencia;

import construtrack.modelo.Empresa;

/** Repositorio que no escribe en disco. Útil para pruebas y demostraciones. */
public class RepositorioMemoria implements LectorDatos, EscritorDatos {

    private final Empresa empresa;
    private int guardados;

    public RepositorioMemoria(Empresa empresa) {
        this.empresa = empresa;
    }

    @Override
    public Empresa cargar() {
        return empresa;
    }

    @Override
    public void guardar(Empresa empresa) {
        guardados++;
    }

    /** Cuántas veces se pidió guardar (permite comprobar en las pruebas que un servicio persiste sus cambios). */
    public int getGuardados() {
        return guardados;
    }
}
