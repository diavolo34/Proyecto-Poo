package construtrack.modelo.excepciones;

/** Un dato no cumple las reglas del negocio (cédula mal escrita, horario incoherente, etc.). */
public class DatoInvalidoException extends ConstruTrackException {

    private static final long serialVersionUID = 1L;

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
