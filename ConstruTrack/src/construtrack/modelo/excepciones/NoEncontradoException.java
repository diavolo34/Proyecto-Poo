package construtrack.modelo.excepciones;

/** Se busca un usuario, una obra o una incidencia que no existe. */
public class NoEncontradoException extends ConstruTrackException {

    private static final long serialVersionUID = 1L;

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
