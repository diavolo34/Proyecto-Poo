package construtrack.modelo.excepciones;

public class NoEncontradoException extends ConstruTrackException {
    private static final long serialVersionUID = 1L;

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
