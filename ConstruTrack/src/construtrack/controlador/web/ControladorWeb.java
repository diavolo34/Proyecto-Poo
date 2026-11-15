package construtrack.controlador.web;

/** Un controlador de la API web: sabe qué rutas atiende y las registra en el enrutador. */
public interface ControladorWeb {

    void registrarRutas(Enrutador enrutador);
}
