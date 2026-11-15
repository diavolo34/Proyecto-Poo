package construtrack.controlador.web;

/** Lo que se ejecuta cuando una ruta coincide con la petición. */
@FunctionalInterface
public interface Accion {

    Respuesta atender(Peticion peticion);
}
