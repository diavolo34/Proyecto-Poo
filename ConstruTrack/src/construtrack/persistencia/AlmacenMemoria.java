package construtrack.persistencia;

import java.util.Optional;

public class AlmacenMemoria implements Almacen {
    private String contenido;

    @Override
    public Optional<String> leer() {
        return Optional.ofNullable(contenido);
    }

    @Override
    public void escribir(String contenido) {
        this.contenido = contenido;
    }

    @Override
    public String descripcion() {
        return "memoria";
    }
}
