package construtrack.modelo.excepciones;

public class PermisoDenegadoException extends ConstruTrackException {
    private static final long serialVersionUID = 1L;

    public PermisoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
