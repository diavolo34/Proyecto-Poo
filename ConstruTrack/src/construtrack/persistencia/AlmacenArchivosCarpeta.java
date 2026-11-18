package construtrack.persistencia;

import construtrack.modelo.excepciones.NoEncontradoException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class AlmacenArchivosCarpeta implements AlmacenArchivos {
    private final Path carpeta;

    public AlmacenArchivosCarpeta(Path carpeta) {
        this.carpeta = carpeta;
    }

    @Override
    public String guardar(String nombre, String tipo, byte[] datos) {
        String id = UUID.randomUUID().toString();
        try {
            Files.createDirectories(carpeta);
            Files.write(carpeta.resolve(id), datos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return id;
    }

    @Override
    public byte[] leer(String id) {
        if (!id.matches("[0-9a-f-]{36}")) {
            throw new NoEncontradoException("El archivo no existe.");
        }
        Path archivo = carpeta.resolve(id);
        try {
            return Files.readAllBytes(archivo);
        } catch (java.nio.file.NoSuchFileException e) {
            throw new NoEncontradoException("El archivo ya no está disponible.");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String descripcion() {
        return "carpeta " + carpeta.toAbsolutePath();
    }
}
