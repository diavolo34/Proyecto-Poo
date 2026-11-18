package construtrack.modelo;

public record Firma(String cedula, String nombre) {
    public static final Firma DESCONOCIDA = new Firma("-", "—");

    public static Firma de(Usuario usuario) {
        return usuario == null ? null : new Firma(usuario.getCedula(), usuario.getNombre());
    }

    public boolean esDe(Usuario usuario) {
        return usuario != null && usuario.getCedula().equals(cedula);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
