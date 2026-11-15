package construtrack.vista.swing;

/**
 * Elemento de una lista desplegable: guarda un valor interno (cédula, código) y muestra un texto legible.
 * Así el usuario reconoce opciones en vez de recordar códigos (heurística 6 de Nielsen).
 */
public record Opcion(String valor, String texto) {

    @Override
    public String toString() {
        return texto;
    }
}
