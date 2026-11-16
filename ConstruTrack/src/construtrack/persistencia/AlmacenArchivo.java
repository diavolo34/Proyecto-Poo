package construtrack.persistencia;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class AlmacenArchivo implements Almacen {
    private final Path archivo;

    public AlmacenArchivo(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public Optional<String> leer() {
        if (!Files.exists(archivo)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(archivo, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo.toAbsolutePath(), e);
        }
    }

    @Override
    public void escribir(String contenido) {
        try {
            Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + archivo.toAbsolutePath(), e);
        }
    }

    @Override
    public String descripcion() {
        return "archivo " + archivo.toAbsolutePath();
    }
}
