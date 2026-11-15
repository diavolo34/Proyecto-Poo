package construtrack.modelo.excepciones;

/** El usuario intenta una acción que su rol no permite. */
public class PermisoDenegadoException extends ConstruTrackException {

    private static final long serialVersionUID = 1L;

    public PermisoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
