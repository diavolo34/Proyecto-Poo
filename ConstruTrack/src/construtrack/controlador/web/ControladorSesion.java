package construtrack.controlador.web;

import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.servicio.Ingreso;
import construtrack.servicio.ServicioAutenticacion;

public class ControladorSesion implements ControladorWeb {
    private final ServicioAutenticacion autenticacion;
    private final GestorSesiones sesiones;
    private final boolean modoDemo;
    private final boolean demoPrivada;
    private final boolean permitirIngreso;

    public ControladorSesion(ServicioAutenticacion autenticacion, GestorSesiones sesiones) {
        this(autenticacion, sesiones, false, false, true);
    }

    public ControladorSesion(ServicioAutenticacion autenticacion, GestorSesiones sesiones, boolean modoDemo,
            boolean demoPrivada, boolean permitirIngreso) {
        this.autenticacion = autenticacion;
        this.sesiones = sesiones;
        this.modoDemo = modoDemo;
        this.demoPrivada = demoPrivada;
        this.permitirIngreso = permitirIngreso;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/estado", this::estado)
                .post("/api/login", this::iniciarSesion)
                .post("/api/logout", this::cerrarSesion)
                .get("/api/yo", this::yo);
    }

    private Respuesta yo(Peticion p) {
        Usuario usuario = sesiones.obtenerUsuario(p.token());
        if (usuario != null) {
            return Respuesta.json(JsonMapper.usuario(usuario, true));
        }
        return Respuesta.json(new JsonObjeto().logico("pendiente", true)
                .crudo("cuenta", JsonMapper.cuenta(p.cuenta())).toString());
    }

    private Respuesta estado(Peticion p) {
        return Respuesta.json(new JsonObjeto()
                .texto("status", "ok")
                .texto("servidor", "Java")
                .texto("empresa", autenticacion.getNombreEmpresa())
                .logico("demo", modoDemo)
                .logico("demoPrivada", demoPrivada)
                .toString());
    }

    private Respuesta iniciarSesion(Peticion p) {
        if (!permitirIngreso) {
            throw new PermisoDenegadoException("En la demostración privada se entra con la clave de demostración.");
        }
        String identificador = p.cuerpo().getOrDefault("identificador", p.cuerpo().get("cedula"));
        Ingreso ingreso = autenticacion.ingresar(identificador, p.cuerpo().get("clave"));
        return Respuesta.json(JsonMapper.sesion(sesiones.iniciarSesion(ingreso), ingreso));
    }

    private Respuesta cerrarSesion(Peticion p) {
        sesiones.cerrarSesion(p.token());
        return Respuesta.json(JsonMapper.ok());
    }
}
