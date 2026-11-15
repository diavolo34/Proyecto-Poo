package construtrack.modelo.reportes;

/**
 * Convierte un documento en un formato de salida. Cada formato es una clase aparte:
 * agregar XML o PDF no obliga a tocar los reportes ni los formatos existentes (principio abierto/cerrado).
 */
public interface FormateadorReporte {

    String formatear(Documento documento);

    /** Tipo MIME para la descarga, por ejemplo text/csv. */
    String mime();

    /** Extensión del archivo, sin punto. */
    String extension();
}
