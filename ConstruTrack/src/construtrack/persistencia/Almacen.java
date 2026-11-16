package construtrack.persistencia;

import java.util.Optional;

public interface Almacen {
    Optional<String> leer();

    void escribir(String contenido);

    String descripcion();
}
