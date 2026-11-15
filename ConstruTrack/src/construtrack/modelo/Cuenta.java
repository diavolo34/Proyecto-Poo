package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.time.LocalDateTime;

/**
 * Cuenta personal creada al registrarse con correo. Existe aunque la persona todavía no pertenezca
 * a la empresa; cuando canjea una invitación queda vinculada a su ficha de Usuario.
 *
 * La clave se guarda como resumen con el documento como sal (igual que Usuario), así ambos
 * comparten el mismo resumen y se puede iniciar sesión con correo o con cédula.
 */
public class Cuenta {

    private final String correo;
    private final String documento;
    private final LocalDateTime registrada;
    private String claveResumen;
    private String nombre;
    private String apellido;
    private String telefono;
    private Usuario usuario;

    public Cuenta(String correo, String clave, String nombre, String apellido, String documento, String telefono,
            LocalDateTime registrada) {
        this.correo = validarCorreo(correo);
        this.documento = Usuario.validarDocumento(documento);
        this.nombre = validarParteNombre(nombre, "nombre");
        this.apellido = validarParteNombre(apellido, "apellido");
        this.telefono = validarTelefono(telefono);
        this.registrada = registrada;
        Usuario.validarClave(clave);
        this.claveResumen = CifradoClave.resumir(this.documento, clave);
    }

    /** Correo en minúsculas y con formato válido. */
    public static String validarCorreo(String correo) {
        String limpio = correo == null ? "" : correo.trim().toLowerCase();
        if (!limpio.matches("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}") || limpio.length() > 80) {
            throw new DatoInvalidoException("Escribe un correo válido, por ejemplo nombre@empresa.com.");
        }
        return limpio;
    }

    /** Teléfono con 7 a 15 dígitos; se permiten +, espacios, guiones y paréntesis. */
    public static String validarTelefono(String telefono) {
        String limpio = telefono == null ? "" : telefono.trim();
        String digitos = limpio.replaceAll("[^0-9]", "");
        if (!limpio.matches("[+0-9 ()-]+") || digitos.length() < 7 || digitos.length() > 15) {
            throw new DatoInvalidoException("El teléfono debe tener entre 7 y 15 dígitos, por ejemplo +58 412 1234567.");
        }
        return limpio;
    }

    private static String validarParteNombre(String texto, String campo) {
        String limpio = texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
        if (!limpio.matches("[\\p{L} .'-]{2,40}")) {
            throw new DatoInvalidoException("El " + campo + " debe tener entre 2 y 40 letras.");
        }
        return limpio;
    }

    public boolean verificarClave(String clave) {
        return clave != null && claveResumen.equals(CifradoClave.resumir(documento, clave));
    }

    /** Une la cuenta con su ficha en la empresa (y viceversa). */
    void vincular(Usuario usuario) {
        this.usuario = usuario;
    }

    /** La ficha fue eliminada por el Jefe: la cuenta sigue existiendo, pero sin empresa. */
    void desvincular() {
        this.usuario = null;
    }

    /** Mantiene sincronizado el resumen de clave con el de la ficha de Usuario. */
    void sincronizarClave(String resumen) {
        this.claveResumen = resumen;
    }

    public void restaurarClaveResumen(String resumen) {
        if (resumen == null || !resumen.matches("[0-9a-f]{64}")) {
            throw new DatoInvalidoException("Resumen de clave inválido.");
        }
        this.claveResumen = resumen;
    }

    public boolean perteneceAEmpresa() {
        return usuario != null;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = validarTelefono(telefono);
    }

    public LocalDateTime getRegistrada() {
        return registrada;
    }

    public String getClaveResumen() {
        return claveResumen;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
