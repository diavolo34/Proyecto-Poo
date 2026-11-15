package construtrack.modelo.excepciones;

/** La cédula o la clave no coinciden con ningún usuario. */
public class CredencialesInvalidasException extends ConstruTrackException {

    private static final long serialVersionUID = 1L;

    public CredencialesInvalidasException() {
        super("Cédula o clave incorrecta. Revisa los datos e intenta de nuevo.");
    }
}
