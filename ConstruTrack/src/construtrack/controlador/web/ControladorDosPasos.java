package construtrack.controlador.web;

import construtrack.modelo.Usuario;
import construtrack.servicio.ServicioDosPasos;
import construtrack.util.Json;
import java.util.List;

public class ControladorDosPasos implements ControladorWeb {
    private final ServicioDosPasos dosPasos;

    public ControladorDosPasos(ServicioDosPasos dosPasos) {
        this.dosPasos = dosPasos;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/dos-pasos", this::estado)
                .post("/api/dos-pasos/iniciar", this::iniciar)
                .post("/api/dos-pasos/activar", this::activar)
                .post("/api/dos-pasos/desactivar", this::desactivar);
    }

    private Respuesta estado(Peticion p) {
        Usuario usuario = p.usuario();
        return Respuesta.json(new JsonObjeto()
                .logico("disponible", usuario.admiteDosPasos())
                .logico("activa", usuario.tieneDosPasos())
                .numero("respaldosRestantes", usuario.getRespaldosDosPasos().size())
                .toString());
    }

    private Respuesta iniciar(Peticion p) {
        ServicioDosPasos.Inscripcion inscripcion = dosPasos.iniciar(p.usuario());
        return Respuesta.json(new JsonObjeto().texto("secreto", inscripcion.secreto()).texto("enlace", inscripcion.enlace())
                .toString());
    }

    private Respuesta activar(Peticion p) {
        List<String> respaldos = dosPasos.activar(p.usuario(), p.cuerpo().get("codigo"));
        return Respuesta.json(new JsonObjeto()
                .texto("mensaje", "Listo: la verificación en dos pasos quedó activa.")
                .crudo("respaldos", Json.lista(respaldos, Json::texto))
                .toString());
    }

    private Respuesta desactivar(Peticion p) {
        dosPasos.desactivar(p.usuario(), p.cuerpo().get("clave"), p.cuerpo().get("codigo"));
        return Respuesta.json(new JsonObjeto().texto("mensaje", "La verificación en dos pasos quedó desactivada.").toString());
    }
}
