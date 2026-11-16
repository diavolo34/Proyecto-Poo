package construtrack.controlador.web;

public class SesionRequeridaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SesionRequeridaException() {
        super("Tu sesión expiró o no has iniciado sesión. Vuelve a entrar con tu correo o cédula y tu clave.");
    }
}
