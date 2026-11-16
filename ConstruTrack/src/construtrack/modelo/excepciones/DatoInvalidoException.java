package construtrack.modelo.excepciones;

public class DatoInvalidoException extends ConstruTrackException {
    private static final long serialVersionUID = 1L;

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
