package construtrack.persistencia;

import construtrack.modelo.Empresa;
import construtrack.modelo.Plataforma;

public class RepositorioMemoria implements LectorDatos, EscritorDatos {
    private final Plataforma plataforma;
    private int guardados;

    public RepositorioMemoria(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

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

    public int getGuardados() {
        return guardados;
    }
}
