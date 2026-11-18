package construtrack.controlador.web;

import construtrack.modelo.Cuenta;
import construtrack.modelo.Jefe;
import construtrack.modelo.Usuario;
import construtrack.servicio.Ingreso;
import construtrack.servicio.ServicioRegistro;

public class ControladorRegistro implements ControladorWeb {
    private final ServicioRegistro registro;
    private final GestorSesiones sesiones;

    public ControladorRegistro(ServicioRegistro registro, GestorSesiones sesiones) {
        this.registro = registro;
        this.sesiones = sesiones;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.post("/api/registro", this::registrar)
                .post("/api/unirse", this::unirse)
                .post("/api/empresas", this::crearEmpresa)
                .post("/api/verificacion", this::confirmarCorreo)
                .post("/api/verificacion/reenviar", this::reenviarCodigo);
    }

    private Respuesta confirmarCorreo(Peticion p) {
        Cuenta cuenta = p.cuenta();
        Usuario usuario = registro.confirmarCorreo(cuenta, p.cuerpo().get("codigo"));
        String mensaje = usuario != null && usuario.getRol().equals(Jefe.ROL)
                ? "¡Correo confirmado! Tu empresa «" + usuario.getEmpresa().getNombre() + "» está lista."
                : "¡Correo confirmado!";
        return Respuesta.json(new JsonObjeto().texto("mensaje", mensaje)
                .crudo("cuenta", JsonMapper.cuenta(cuenta))
                .crudo("usuario", usuario == null ? "null" : JsonMapper.usuario(usuario, true)).toString());
    }

    private Respuesta reenviarCodigo(Peticion p) {
        registro.reenviarCodigo(p.cuenta());
        return Respuesta.json(new JsonObjeto().texto("mensaje", "Te enviamos un código nuevo. Revisa tu correo y la carpeta de spam.")
                .toString());
    }

    private Respuesta registrar(Peticion p) {
        Cuenta cuenta = registro.registrar(p.cuerpo());
        Ingreso ingreso = new Ingreso(cuenta, cuenta.getUsuario());
        return Respuesta.creado(JsonMapper.sesion(sesiones.iniciarSesion(ingreso), ingreso));
    }

    private Respuesta unirse(Peticion p) {
        Usuario usuario = registro.unirse(p.cuenta(), p.cuerpo().get("codigo"));
        return Respuesta.json(JsonMapper.mensaje("¡Te damos la bienvenida a " + usuario.getEmpresa().getNombre()
                + "! Ya eres " + usuario.getRol().toLowerCase() + ".", JsonMapper.usuario(usuario, true)));
    }

    private Respuesta crearEmpresa(Peticion p) {
        Usuario jefe = registro.crearEmpresa(p.cuenta(), p.cuerpo().get("nombre"));
        return Respuesta.creado(JsonMapper.mensaje("Tu empresa «" + jefe.getEmpresa().getNombre()
                + "» está lista. Ahora invita a tu equipo.", JsonMapper.usuario(jefe, true)));
    }
}
