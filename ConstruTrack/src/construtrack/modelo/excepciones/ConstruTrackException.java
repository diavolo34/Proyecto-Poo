package construtrack.modelo.excepciones;

/**
 * Raíz de las excepciones del dominio. Cada subclase representa un tipo de error de negocio
 * para que las vistas (Swing o web) decidan cómo mostrarlo sin conocer los detalles internos.
 */
public abstract class ConstruTrackException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected ConstruTrackException(String mensaje) {
        super(mensaje);
    }
}
