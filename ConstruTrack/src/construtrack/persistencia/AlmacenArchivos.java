package construtrack.persistencia;

public interface AlmacenArchivos {
    String guardar(String nombre, String tipo, byte[] datos);

    byte[] leer(String id);

    String descripcion();
}
