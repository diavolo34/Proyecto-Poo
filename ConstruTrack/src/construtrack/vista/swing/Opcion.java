package construtrack.vista.swing;

public record Opcion(String valor, String texto) {
    @Override
    public String toString() {
        return texto;
    }
}
