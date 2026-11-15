package construtrack.controlador.web;

import construtrack.modelo.Usuario;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.servicio.ServicioIncidencias;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Rutas de incidencias: listar, reportar y revisar (aprobar o rechazar). */
public class ControladorIncidencias implements ControladorWeb {

    private final ServicioIncidencias incidencias;

    public ControladorIncidencias(ServicioIncidencias incidencias) {
        this.incidencias = incidencias;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/incidencias/opciones", this::opciones)
                .get("/api/incidencias", this::listar)
                .post("/api/incidencias", this::reportar)
                .put("/api/incidencias/{id}/revision", this::revisar);
    }

    private Respuesta listar(Peticion p) {
        Usuario actual = p.usuario();
        List<Incidencia> lista = incidencias.listarVisibles(actual);
        lista.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        return Respuesta.json(Json.lista(lista, i -> JsonMapper.incidencia(i, incidencias.puedeRevisar(actual, i))));
    }

    /** Tipos de incidencia y personas para las que el usuario puede reportar (llenan los selectores). */
    private Respuesta opciones(Peticion p) {
        Usuario actual = p.usuario();
        JsonObjeto tipos = new JsonObjeto();
        incidencias.tipos().forEach(tipos::texto);
        String personas = Json.lista(incidencias.personasReportables(actual), u -> new JsonObjeto()
                .texto("cedula", u.getCedula())
                .texto("nombre", u == actual ? u.getNombre() + " (yo)" : u.getNombre() + " · " + u.getRol())
                .toString());
        return Respuesta.json(new JsonObjeto().crudo("tipos", tipos.toString()).crudo("personas", personas).toString());
    }

    private Respuesta reportar(Peticion p) {
        Usuario actual = p.usuario();
        Map<String, String> datos = p.cuerpo();
        Map<String, String> extras = new HashMap<>(datos);
        for (String clave : List.of("codigoTipo", "cedula", "desde", "hasta", "descripcion")) {
            extras.remove(clave);
        }
        Incidencia nueva = incidencias.reportar(actual, datos.get("codigoTipo"), datos.get("cedula"),
                fecha(datos.get("desde")), fecha(datos.get("hasta")), datos.get("descripcion"), extras);
        return Respuesta.creado(JsonMapper.incidencia(nueva, false));
    }

    private Respuesta revisar(Peticion p) {
        Map<String, String> datos = p.cuerpo();
        EstadoIncidencia estado;
        try {
            estado = EstadoIncidencia.valueOf(datos.get("estado"));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Indica un estado válido: APROBADA o RECHAZADA.");
        }
        incidencias.revisar(p.usuario(), Integer.parseInt(p.parametro("id")), estado, datos.get("comentario"));
        return Respuesta.json(JsonMapper.ok());
    }

    private static LocalDate fecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null; // la fecha final es opcional: si falta, la incidencia dura un día
        }
        try {
            return LocalDate.parse(texto);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("La fecha debe tener el formato AAAA-MM-DD.");
        }
    }
}
