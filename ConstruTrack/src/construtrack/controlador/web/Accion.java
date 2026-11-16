package construtrack.controlador.web;

@FunctionalInterface
public interface Accion {
    Respuesta atender(Peticion peticion);
}
