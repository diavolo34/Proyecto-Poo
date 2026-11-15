package construtrack.modelo.excepciones;

/**
 * La operación es válida en general, pero no en el estado actual del sistema
 * (marcar entrada dos veces, aprobar una incidencia ya cerrada, cédula repetida...).
 */
public class OperacionInvalidaException extends ConstruTrackException {

    private static final long serialVersionUID = 1L;

    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
