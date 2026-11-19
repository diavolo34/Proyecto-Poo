package construtrack.servicio;

import construtrack.modelo.Catalogo;
import construtrack.modelo.Empresa;
import construtrack.modelo.Gerente;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;

public class ServicioCatalogo extends ServicioBase {
    public enum Tipo {
        PUESTOS;

        public static Tipo desdeTexto(String texto) {
            if ("puestos".equalsIgnoreCase(texto)) {
                return PUESTOS;
            }
            throw new DatoInvalidoException("El catálogo es de puestos; las áreas se gestionan en «Áreas».");
        }
    }

    public ServicioCatalogo(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        super(plataforma, escritor, reloj);
    }

    public Catalogo ver(Usuario actual) {
        return leer(() -> empresaDe(actual).getCatalogo());
    }

    public String agregar(Usuario actual, Tipo tipo, String nombre) {
        Empresa empresa = comoGerente(actual);
        return modificar(() -> empresa.getCatalogo().agregarPuesto(nombre));
    }

    public void quitar(Usuario actual, Tipo tipo, String nombre) {
        Empresa empresa = comoGerente(actual);
        modificar(() -> {
            empresa.quitarPuesto(nombre);
            return null;
        });
    }

    private Empresa comoGerente(Usuario actual) {
        if (!(actual instanceof Gerente)) {
            throw new PermisoDenegadoException("Solo el gerente administra el catálogo de puestos.");
        }
        return empresaDe(actual);
    }
}
