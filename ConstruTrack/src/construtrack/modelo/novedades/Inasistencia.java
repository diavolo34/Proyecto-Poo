package construtrack.modelo.novedades;

public class Inasistencia extends ReporteObra {
    public static final String CODIGO = "INASISTENCIA";
    public static final String CODIGO_ANTERIOR = "AUSENCIA";

    public Inasistencia(DatosNovedad datos) {
        super(datos);
    }

    @Override
    public String getCodigoTipo() {
        return CODIGO;
    }

    @Override
    public String getNombreTipo() {
        return "Inasistencia injustificada";
    }
}
