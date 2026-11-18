package construtrack.persistencia;

import construtrack.modelo.excepciones.NoEncontradoException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlmacenArchivosMemoria implements AlmacenArchivos {
    private final Map<String, byte[]> archivos = new ConcurrentHashMap<>();

    @Override
    public String guardar(String nombre, String tipo, byte[] datos) {
        String id = UUID.randomUUID().toString();
        archivos.put(id, datos.clone());
        return id;
    }

    @Override
    public byte[] leer(String id) {
        byte[] datos = archivos.get(id);
        if (datos == null) {
            throw new NoEncontradoException("El archivo ya no está disponible.");
        }
        return datos.clone();
    }

    @Override
    public String descripcion() {
        return "memoria";
    }
}
