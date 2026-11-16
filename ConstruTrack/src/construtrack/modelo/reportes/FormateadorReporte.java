package construtrack.modelo.reportes;

public interface FormateadorReporte {
    String formatear(Documento documento);

    String mime();

    String extension();
}
