package construtrack.controlador.web;

import construtrack.modelo.excepciones.NoEncontradoException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla de rutas de la API. Cada controlador registra sus propias rutas, así que agregar un recurso
 * nuevo no obliga a modificar esta clase (abierta a extensión, cerrada a modificación).
 */
public class Enrutador {

    private final List<Ruta> rutas = new ArrayList<>();

    public Enrutador get(String patron, Accion accion) {
        return agregar("GET", patron, accion);
    }

    public Enrutador post(String patron, Accion accion) {
        return agregar("POST", patron, accion);
    }

    public Enrutador put(String patron, Accion accion) {
        return agregar("PUT", patron, accion);
    }

    public Enrutador delete(String patron, Accion accion) {
        return agregar("DELETE", patron, accion);
    }

    private Enrutador agregar(String metodo, String patron, Accion accion) {
        rutas.add(new Ruta(metodo, patron, accion));
        return this;
    }

    public Respuesta atender(Peticion peticion) {
        for (Ruta ruta : rutas) {
            if (ruta.coincide(peticion)) {
                return ruta.getAccion().atender(peticion);
            }
        }
        throw new NoEncontradoException("Ruta no encontrada: " + peticion.metodo() + " " + peticion.ruta());
    }

    /** Una ruta con segmentos variables entre llaves, por ejemplo /api/usuarios/{cedula}. */
    private static final class Ruta {

        private final String metodo;
        private final String[] segmentos;
        private final Accion accion;

        Ruta(String metodo, String patron, Accion accion) {
            this.metodo = metodo;
            this.segmentos = patron.split("/");
            this.accion = accion;
        }

        boolean coincide(Peticion peticion) {
            String[] partes = peticion.ruta().split("/");
            if (!metodo.equals(peticion.metodo()) || partes.length != segmentos.length) {
                return false;
            }
            for (int i = 0; i < partes.length; i++) {
                if (!segmentos[i].startsWith("{") && !segmentos[i].equals(partes[i])) {
                    return false;
                }
            }
            for (int i = 0; i < partes.length; i++) {
                if (segmentos[i].startsWith("{")) {
                    peticion.agregarParametroRuta(segmentos[i].substring(1, segmentos[i].length() - 1), partes[i]);
                }
            }
            return true;
        }

        Accion getAccion() {
            return accion;
        }
    }
}
