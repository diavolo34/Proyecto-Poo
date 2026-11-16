package construtrack.controlador.web;

import construtrack.modelo.Invitacion;
import construtrack.servicio.ServicioInvitaciones;
import construtrack.util.Json;
import java.util.Map;

/** Rutas del Jefe para crear, listar y cancelar invitaciones. */
public class ControladorInvitaciones implements ControladorWeb {

    private final ServicioInvitaciones invitaciones;

    public ControladorInvitaciones(ServicioInvitaciones invitaciones) {
        this.invitaciones = invitaciones;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/invitaciones", p -> Respuesta.json(Json.lista(invitaciones.listar(p.usuario()),
                        v -> JsonMapper.invitacion(v, invitaciones.ahora()))))
                .post("/api/invitaciones", this::crear)
                .delete("/api/invitaciones/{codigo}", p -> {
                    invitaciones.cancelar(p.usuario(), p.parametro("codigo"));
                    return Respuesta.json(JsonMapper.ok());
                });
    }

    private Respuesta crear(Peticion p) {
        Map<String, String> d = p.cuerpo();
        int dias;
        try {
            dias = Integer.parseInt(d.getOrDefault("dias", "7"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Los días de vigencia deben ser un número entre 1 y 30.");
        }
        Invitacion v = invitaciones.crear(p.usuario(), d.get("rol"), d.get("supervisor"), d.get("obra"), dias,
                d.get("correo"));
        return Respuesta.creado(JsonMapper.invitacion(v, invitaciones.ahora()));
    }
}
