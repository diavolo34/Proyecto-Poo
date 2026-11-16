package construtrack.modelo.excepciones;

public abstract class ConstruTrackException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    protected ConstruTrackException(String mensaje) {
        super(mensaje);
    }
}
