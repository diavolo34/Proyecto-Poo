package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.time.LocalDateTime;

public class Cuenta {
    private final String correo;
    private final String documento;
    private final LocalDateTime registrada;
    private String claveResumen;
    private String nombre;
    private String apellido;
    private String telefono;
    private Usuario usuario;

    private Cuenta(String correo, String nombre, String apellido, String documento, String telefono,
            LocalDateTime registrada) {
        this.correo = validarCorreo(correo);
        this.documento = Usuario.validarDocumento(documento);
        this.nombre = validarParteNombre(nombre, "nombre");
        this.apellido = validarParteNombre(apellido, "apellido");
        this.telefono = validarTelefono(telefono);
        this.registrada = registrada;
    }

    public Cuenta(String correo, String clave, String nombre, String apellido, String documento, String telefono,
            LocalDateTime registrada) {
        this(correo, nombre, apellido, documento, telefono, registrada);
        Usuario.validarClave(clave);
        this.claveResumen = CifradoClave.resumir(clave);
    }

    public static Cuenta restaurar(String correo, String claveResumen, String nombre, String apellido, String documento,
            String telefono, LocalDateTime registrada) {
        Cuenta cuenta = new Cuenta(correo, nombre, apellido, documento, telefono, registrada);
        cuenta.restaurarClaveResumen(claveResumen);
        return cuenta;
    }

    public static String validarCorreo(String correo) {
        String limpio = correo == null ? "" : correo.trim().toLowerCase();
        if (!limpio.matches("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}") || limpio.length() > 80) {
            throw new DatoInvalidoException("Escribe un correo válido, por ejemplo nombre@empresa.com.");
        }
        return limpio;
    }

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
        return CifradoClave.verificar(claveResumen, documento, clave);
    }

    public boolean tieneCifradoAntiguo() {
        return CifradoClave.esAntiguo(claveResumen);
    }

    public void cambiarClave(String clave) {
        Usuario.validarClave(clave);
        renovarCifrado(clave);
    }

    public void renovarCifrado(String clave) {
        if (usuario != null) {
            usuario.renovarCifrado(clave);
        } else {
            claveResumen = CifradoClave.resumir(clave);
        }
    }

    void vincular(Usuario usuario) {
        this.usuario = usuario;
    }

    void desvincular() {
        this.usuario = null;
    }

    void sincronizarClave(String resumen) {
        this.claveResumen = resumen;
    }

    public void restaurarClaveResumen(String resumen) {
        if (!CifradoClave.esValido(resumen)) {
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
