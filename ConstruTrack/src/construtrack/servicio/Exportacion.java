package construtrack.servicio;

/** Contenido listo para descargar: el texto, su tipo MIME y el nombre sugerido del archivo. */
public record Exportacion(String contenido, String mime, String nombreArchivo) {
}
