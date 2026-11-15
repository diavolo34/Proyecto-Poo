package construtrack.servicio;

import construtrack.modelo.Cuenta;
import construtrack.modelo.Empresa;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.CredencialesInvalidasException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;

/** Caso de uso: iniciar sesión con cédula y clave. */
public class ServicioAutenticacion extends ServicioBase {

    public ServicioAutenticacion(Empresa empresa, EscritorDatos escritor, Clock reloj) {
        super(empresa, escritor, reloj);
    }

    public Usuario autenticar(String cedula, String clave) {
        return leer(() -> {
            Usuario usuario = empresa.autenticar(cedula, clave);
            if (usuario == null) {
                throw new CredencialesInvalidasException();
            }
            return usuario;
        });
    }

    /**
     * Inicia sesión con correo (si contiene @) o con cédula, y la clave.
     * Devuelve la cuenta y, si ya pertenece a la empresa, su ficha.
     */
    public Ingreso ingresar(String identificador, String clave) {
        return leer(() -> {
            String id = identificador == null ? "" : identificador.trim();
            if (id.contains("@")) {
                Cuenta cuenta = empresa.buscarCuenta(id);
                if (cuenta == null || !cuenta.verificarClave(clave)) {
                    throw new CredencialesInvalidasException();
                }
                return new Ingreso(cuenta, cuenta.getUsuario());
            }
            Usuario usuario = empresa.autenticar(id, clave);
            if (usuario == null) {
                throw new CredencialesInvalidasException();
            }
            return new Ingreso(usuario.getCuenta(), usuario);
        });
    }

    public Cuenta buscarCuenta(String correo) {
        return leer(() -> empresa.buscarCuenta(correo));
    }

    public Usuario buscar(String cedula) {
        return leer(() -> empresa.buscarUsuario(cedula));
    }

    public String getNombreEmpresa() {
        return empresa.getNombre();
    }
}
