package construtrack.modelo.excepciones;

/** La cédula o la clave no coinciden con ningún usuario. */
public class CredencialesInvalidasException extends ConstruTrackException {

    private static final long serialVersionUID = 1L;

    public CredencialesInvalidasException() {
        super("Correo, cédula o clave incorrectos. Revisa los datos e intenta de nuevo.");
    }
}
