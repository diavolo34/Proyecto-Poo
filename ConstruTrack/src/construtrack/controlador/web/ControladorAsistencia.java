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
        RegistroAsistencia registro = asistencia.marcarEntrada(usuario, p.cuerpo().get("codigo"), numero(p, "latitud"),
                numero(p, "longitud"), numero(p, "precision"));
        String mensaje = "Entrada registrada a las " + registro.getHoraEntrada()
                + (registro.isTarde() ? " (llegada tarde)." : ".") + verificacion(registro);
        return Respuesta.json(JsonMapper.mensaje(mensaje, JsonMapper.usuario(usuario, true)));
    }

    private Respuesta salida(Peticion p) {
        Usuario usuario = p.usuario();
        RegistroAsistencia registro = asistencia.marcarSalida(usuario, p.cuerpo().get("codigo"), numero(p, "latitud"),
                numero(p, "longitud"), numero(p, "precision"));
        return Respuesta.json(JsonMapper.mensaje("Salida registrada a las " + registro.getHoraSalida() + "."
                + verificacion(registro), JsonMapper.usuario(usuario, true)));
    }

    private static String verificacion(RegistroAsistencia registro) {
        return switch (registro.getVerificacion()) {
            case VERIFICADA -> " Verificada en el proyecto.";
            case POR_CONFIRMAR -> " Quedó por confirmar (" + registro.getMotivoVerificacion() + "): tu supervisor la revisará.";
            default -> "";
        };
    }

    private static Double numero(Peticion p, String campo) {
        String valor = p.cuerpo().get(campo);
        if (valor == null || valor.isBlank() || "null".equals(valor)) {
            return null;
        }
        try {
            return Double.valueOf(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La ubicación enviada no es válida.");
        }
    }
}
