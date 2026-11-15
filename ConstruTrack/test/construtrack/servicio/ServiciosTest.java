package construtrack.servicio;

import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.modelo.excepciones.CredencialesInvalidasException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.persistencia.RepositorioMemoria;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Los servicios se prueban con un repositorio en memoria gracias a la inversión de dependencias. */
class ServiciosTest {

    private final Datos d = new Datos();
    private final RepositorioMemoria repositorio = new RepositorioMemoria(d.empresa);
    private final Servicios servicios = new Servicios(d.empresa, repositorio, Datos.RELOJ, 1);

    @Test
    @DisplayName("Reportar una incidencia asigna números consecutivos y guarda los cambios")
    void reportarGuarda() {
        Incidencia a = servicios.incidencias().reportar(d.jose, "PERMISO_PERSONAL", "30000001", HOY, null, "Trámite bancario", Map.of());
        Incidencia b = servicios.incidencias().reportar(d.ana, "AUSENCIA", "30000001", HOY, null, "No se presentó", Map.of());
        assertEquals(1, a.getId());
        assertEquals(2, b.getId());
        assertEquals(2, repositorio.getGuardados());
    }

    @Test
    @DisplayName("Cada rol ve solo lo que le corresponde")
    void permisosPorRol() {
        assertThrows(PermisoDenegadoException.class, () -> servicios.personal().listarVisibles(d.jose));
        assertEquals(1, servicios.personal().listarVisibles(d.ana).size());
        assertEquals(5, servicios.personal().listarVisibles(d.jefe).size());
        assertThrows(PermisoDenegadoException.class, () -> servicios.supervision().alertas(d.jose, "semana"));
        assertThrows(PermisoDenegadoException.class, () -> servicios.invitaciones().listar(d.ana));
    }

    @Test
    @DisplayName("Registro con correo, unión con código e inicio de sesión con correo o cédula")
    void registroYUnion() {
        var cuenta = servicios.registro().registrar(Map.of("correo", "pablo@correo.com", "clave", "pablo123",
                "confirmacion", "pablo123", "nombre", "Pablo", "apellido", "Rivas", "documento", "28555111",
                "telefono", "04121234567"));
        assertTrue(servicios.autenticacion().ingresar("pablo@correo.com", "pablo123").pendienteDeUnirse());
        var inv = servicios.invitaciones().crear(d.jefe, "Empleado", "20000001", "", 7, "");
        servicios.registro().unirse(cuenta, inv.getCodigo().toString().toLowerCase());
        assertEquals("Empleado", servicios.autenticacion().ingresar("28555111", "pablo123").usuario().getRol());
        assertThrows(CredencialesInvalidasException.class, () -> servicios.autenticacion().ingresar("pablo@correo.com", "mala"));
    }
}
