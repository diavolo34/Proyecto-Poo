package construtrack.servicio;

import construtrack.modelo.Plataforma;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Empresa;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.CredencialesInvalidasException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;

public class ServicioAutenticacion extends ServicioBase {
    public ServicioAutenticacion(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        super(plataforma, escritor, reloj);
    }

    public Usuario autenticar(String cedula, String clave) {
        return leer(() -> {
            Usuario usuario = plataforma.autenticar(cedula, clave);
            if (usuario == null) {
                throw new CredencialesInvalidasException();
            }
            return usuario;
        });
    }

    public Ingreso ingresar(String identificador, String clave) {
        return leer(() -> {
            String id = identificador == null ? "" : identificador.trim();
            if (id.contains("@")) {
                Cuenta cuenta = plataforma.buscarCuenta(id);
                if (cuenta == null || !cuenta.verificarClave(clave)) {
                    throw new CredencialesInvalidasException();
                }
                return new Ingreso(cuenta, cuenta.getUsuario());
            }
            Usuario usuario = plataforma.autenticar(id, clave);
            if (usuario == null) {
                throw new CredencialesInvalidasException();
            }
            return new Ingreso(usuario.getCuenta(), usuario);
        });
    }

    public Cuenta buscarCuenta(String correo) {
        return leer(() -> plataforma.buscarCuenta(correo));
    }

    public Usuario buscar(String cedula) {
        return leer(() -> plataforma.buscarUsuario(cedula));
    }

    public String getNombreEmpresa() {
        Empresa demo = plataforma.empresaDemo();
        return demo == null ? "ConstruTrack" : demo.getNombre();
    }
}
