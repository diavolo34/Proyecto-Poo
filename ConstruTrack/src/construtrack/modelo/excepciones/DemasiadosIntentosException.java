package construtrack.modelo.excepciones;

public class DemasiadosIntentosException extends ConstruTrackException {
    private static final long serialVersionUID = 1L;

    public DemasiadosIntentosException(long minutos) {
        super("Demasiados intentos fallidos. Espera " + minutos + (minutos == 1 ? " minuto" : " minutos")
                + " o restablece tu clave con «¿Olvidaste tu clave?».");
    }
}
