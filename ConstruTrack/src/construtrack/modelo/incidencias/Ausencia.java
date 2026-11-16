package construtrack.modelo.incidencias;

public class Ausencia extends Incidencia {
    public static final String CODIGO = "AUSENCIA";

    public Ausencia(DatosIncidencia datos) {
        super(datos);
    }

    @Override
    public String getCodigoTipo() {
        return CODIGO;
    }

    @Override
    public String getNombreTipo() {
        return "Ausencia injustificada";
    }

    @Override
    public Gravedad getGravedad() {
        return getDias() > 1 ? Gravedad.ALTA : Gravedad.MEDIA;
    }
}
