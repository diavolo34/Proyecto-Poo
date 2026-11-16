package construtrack.controlador.web;

import construtrack.servicio.ServicioPersonal;
import construtrack.util.Json;
import java.time.LocalDate;

/** Ruta pública para la portada: obras y cifras generales. No expone datos personales. */
public class ControladorPublico implements ControladorWeb {

    private final ServicioPersonal personal;

    public ControladorPublico(ServicioPersonal personal) {
        this.personal = personal;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/publico", p -> {
            LocalDate hoy = personal.hoy();
            int[] cifras = personal.contarPersonalYPresentes();
            String obras = Json.lista(personal.listarObras(), o -> new JsonObjeto()
                    .texto("nombre", o.getNombre())
                    .texto("ubicacion", o.getUbicacion())
                    .numero("avance", o.porcentajeTranscurrido(hoy))
                    .texto("finEstimado", o.getFechaFinEstimada())
                    .toString());
            return Respuesta.json(new JsonObjeto()
                    .numero("personal", cifras[0])
                    .numero("presentes", cifras[1])
                    .crudo("obras", obras)
                    .toString());
        });
    }
}
