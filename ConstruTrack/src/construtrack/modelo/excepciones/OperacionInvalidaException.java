package construtrack.modelo.excepciones;

public class OperacionInvalidaException extends ConstruTrackException {
    private static final long serialVersionUID = 1L;

    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
