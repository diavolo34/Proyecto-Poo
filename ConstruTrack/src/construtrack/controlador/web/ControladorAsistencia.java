package construtrack.controlador.web;

import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Usuario;
import construtrack.servicio.ServicioAsistencia;

public class ControladorAsistencia implements ControladorWeb {
    private final ServicioAsistencia asistencia;

    public ControladorAsistencia(ServicioAsistencia asistencia) {
        this.asistencia = asistencia;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.post("/api/asistencia/entrada", this::entrada)
                .post("/api/obra/checkin", this::entrada)
                .post("/api/asistencia/salida", this::salida);
    }

    private Respuesta entrada(Peticion p) {
        Usuario usuario = p.usuario();
        RegistroAsistencia registro = asistencia.marcarEntrada(usuario);
        String mensaje = "Entrada registrada a las " + registro.getHoraEntrada()
                + (registro.isTarde() ? " (llegada tarde)." : ".");
        return Respuesta.json(JsonMapper.mensaje(mensaje, JsonMapper.usuario(usuario, true)));
    }

    private Respuesta salida(Peticion p) {
        Usuario usuario = p.usuario();
        RegistroAsistencia registro = asistencia.marcarSalida(usuario);
        return Respuesta.json(JsonMapper.mensaje("Salida registrada a las " + registro.getHoraSalida() + ".",
                JsonMapper.usuario(usuario, true)));
    }
}
