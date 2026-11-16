package construtrack.controlador.web;

import construtrack.servicio.ServicioPersonal;
import construtrack.util.Json;
import construtrack.modelo.Obra;
import java.time.LocalDate;
import java.util.List;

public class ControladorPublico implements ControladorWeb {
    private final ServicioPersonal personal;
    private final boolean mostrarObras;

    public ControladorPublico(ServicioPersonal personal) {
        this(personal, false);
    }

    public ControladorPublico(ServicioPersonal personal, boolean mostrarObras) {
        this.personal = personal;
        this.mostrarObras = mostrarObras;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/publico", p -> {
            LocalDate hoy = personal.hoy();
            ServicioPersonal.CifrasPublicas cifras = personal.cifrasPublicas();
            String obras = Json.lista(mostrarObras ? personal.listarObrasPublicas() : List.<Obra>of(), o -> new JsonObjeto()
                    .texto("nombre", o.getNombre())
                    .texto("ubicacion", o.getUbicacion())
                    .numero("avance", o.porcentajeTranscurrido(hoy))
                    .texto("finEstimado", o.getFechaFinEstimada())
                    .toString());
            return Respuesta.json(new JsonObjeto()
                    .numero("personal", cifras.personas())
                    .numero("presentes", cifras.presentes())
                    .numero("empresas", cifras.empresas())
                    .numero("obrasActivas", cifras.obras())
                    .numero("avancePromedio", cifras.avancePromedio())
                    .crudo("obras", obras)
                    .toString());
        });
    }
}
