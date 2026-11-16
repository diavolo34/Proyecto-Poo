package construtrack.servicio;

import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.modelo.excepciones.CredencialesInvalidasException;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.persistencia.RepositorioMemoria;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    @Test
    @DisplayName("Una cédula nueva solo admite números (sin V, puntos ni guiones)")
    void cedulaSoloNumeros() {
        for (String mala : new String[] {"V28555111", "28.555.111", "28-555-111", "12345"}) {
            assertThrows(DatoInvalidoException.class, () -> servicios.registro().registrar(Map.of("correo", "x@correo.com",
                    "clave", "clave123", "confirmacion", "clave123", "nombre", "Ana", "apellido", "Paz",
                    "documento", mala, "telefono", "04121234567")), mala);
        }
        assertThrows(DatoInvalidoException.class, () -> servicios.personal().registrar(d.jefe, Map.of("rol", "Empleado",
                "cedula", "V30000099", "nombre", "Pepe Prueba", "clave", "pepe123", "dias", "LU,MA",
                "horaEntrada", "07:00", "horaSalida", "16:00")));
    }

    @Test
    @DisplayName("El período por defecto son los últimos 7 días")
    void periodoPorDefecto() {
        var periodo = ServicioSupervision.periodo(null, HOY);
        assertEquals(HOY.minusDays(6), periodo.getDesde());
        assertEquals(HOY, periodo.getHasta());
    }

    @Test
    @DisplayName("Obras: supervisor y jefe las gestionan; no se elimina una obra con personas")
    void gestionDeObras() {
        Map<String, String> datos = Map.of("nombre", "Edificio Las Acacias", "ubicacion", "Caracas",
                "inicio", HOY.toString(), "fin", HOY.plusMonths(6).toString());
        assertThrows(PermisoDenegadoException.class, () -> servicios.obras().crear(d.jose, datos));
        var obra = servicios.obras().crear(d.ana, datos);
        assertEquals("OB-01", obra.getCodigo());
        assertEquals("OB-02", servicios.obras().crear(d.jefe, Map.of("nombre", "Puente Norte", "ubicacion", "",
                "inicio", HOY.toString(), "fin", HOY.plusMonths(2).toString())).getCodigo());

        servicios.obras().asignar(d.ana, "30000001", "OB-01");
        assertEquals(obra, d.jose.getObra());
        assertThrows(PermisoDenegadoException.class, () -> servicios.obras().asignar(d.ana, "30000004", "OB-01"),
                "Laura no es de su equipo");
        assertThrows(OperacionInvalidaException.class, () -> servicios.obras().eliminar(d.jefe, "OB-01"));
        servicios.obras().asignar(d.jefe, "30000001", "");
        servicios.obras().eliminar(d.jefe, "OB-01");
        assertEquals(1, servicios.obras().listar(d.jefe).size());
        assertThrows(DatoInvalidoException.class, () -> servicios.obras().crear(d.jefe, Map.of("nombre", "Mal",
                "inicio", HOY.toString(), "fin", HOY.minusDays(1).toString())));
    }
}
