package construtrack.modelo.novedades;

public class PermisoLaboral extends Licencia {
    public static final String CODIGO = "PERMISO_LABORAL";
    public static final String CODIGO_ANTERIOR = "PERMISO_PERSONAL";

    public PermisoLaboral(DatosNovedad datos) {
        super(datos);
    }

    @Override
    public String getCodigoTipo() {
        return CODIGO;
    }

    @Override
    public String getNombreTipo() {
        return "Permiso laboral";
    }
}
