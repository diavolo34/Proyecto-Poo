package construtrack.controlador.web;

import construtrack.modelo.Empleado;
import construtrack.modelo.Horario;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Traduce los objetos del modelo a JSON para la vista web.
 * Antes cada entidad tenía su propio toJson(): el modelo dependía del formato de la vista.
 * Ahora esa responsabilidad vive aquí (principio de responsabilidad única).
 */
public final class JsonMapper {

    /** Máximo de jornadas que se envían en el historial detallado. */
    private static final int MAX_HISTORIAL = 30;

    private JsonMapper() {
    }

    public static String usuario(Usuario u, boolean detalle) {
        RegistroAsistencia hoy = u.getAsistenciaDeHoy();
        JsonObjeto json = new JsonObjeto()
                .texto("cedula", u.getCedula())
                .texto("nombre", u.getNombre())
                .texto("rol", u.getRol())
                .logico("presente", u.estaPresente())
                .logico("trabajaHoy", u.trabajaHoy())
                .crudo("horario", horario(u.getHorario()))
                .crudo("asistenciaHoy", hoy == null ? "null" : registro(hoy));
        if (u instanceof Empleado e) {
            Supervisor s = e.getSupervisor();
            json.crudo("supervisor", s == null ? "null"
                    : new JsonObjeto().texto("cedula", s.getCedula()).texto("nombre", s.getNombre()).toString());
        }
        if (u instanceof Supervisor s) {
            json.numero("empleadosACargo", s.verEmpleadosACargo().size());
        }
        if (detalle) {
            List<RegistroAsistencia> recientes = new ArrayList<>(u.getAsistencias());
            Collections.reverse(recientes);
            json.crudo("asistencias", Json.lista(recientes.subList(0, Math.min(MAX_HISTORIAL, recientes.size())),
                    JsonMapper::registro));
        }
        return json.toString();
    }

    public static String horario(Horario h) {
        return new JsonObjeto()
                .texto("dias", h.getDias())
                .texto("horaEntrada", h.getHoraEntrada())
                .texto("horaSalida", h.getHoraSalida())
                .toString();
    }

    public static String registro(RegistroAsistencia r) {
        return new JsonObjeto()
                .texto("fecha", r.getFecha())
                .texto("horaEntrada", r.getHoraEntrada())
                .texto("horaSalida", r.getHoraSalida())
                .logico("tarde", r.isTarde())
                .toString();
    }

    public static String usuarios(List<? extends Usuario> usuarios) {
        return Json.lista(usuarios, u -> usuario(u, false));
    }

    public static String mensaje(String mensaje, String datosJson) {
        return new JsonObjeto().texto("mensaje", mensaje).crudo("usuario", datosJson).toString();
    }

    public static String ok() {
        return "{\"status\":\"ok\"}";
    }

    public static String error(String mensaje) {
        return new JsonObjeto().texto("error", mensaje).toString();
    }
}
