package construtrack.persistencia;

import java.util.Optional;

/**
 * Dónde se guarda el texto con los datos de la empresa (DIP: el repositorio no sabe si es un archivo o la nube).
 */
public interface Almacen {

    /** El contenido guardado, o vacío si todavía no hay datos. */
    Optional<String> leer();

    void escribir(String contenido);

    /** Texto para los mensajes de arranque, por ejemplo "archivo datos.txt". */
    String descripcion();
}
