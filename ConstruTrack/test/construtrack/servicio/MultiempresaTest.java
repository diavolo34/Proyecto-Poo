package construtrack.servicio;

import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Empresa;
import construtrack.modelo.EstadoHorasExtra;
import construtrack.modelo.Jefe;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.persistencia.RepositorioMemoria;
import construtrack.persistencia.RepositorioTexto;
import construtrack.modelo.incidencias.FabricaIncidencias;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiempresaTest {
    private final Datos d = new Datos();
    private final Servicios servicios = new Servicios(d.empresa, new RepositorioMemoria(d.empresa), Datos.RELOJ, 1);

    @TempDir
    Path carpeta;

    private Map<String, String> datos(String correo, String documento, String tipo, String empresa) {
        Map<String, String> m = new HashMap<>(Map.of("correo", correo, "clave", "clave123", "confirmacion", "clave123",
                "nombre", "Marta", "apellido", "Rivas", "documento", documento, "telefono", "04121234567"));
        if (tipo != null) {
            m.put("tipo", tipo);
        }
        if (empresa != null) {
            m.put("empresa", empresa);
        }
        return m;
    }

    private Jefe registrarJefaNorte() {
        Cuenta cuenta = servicios.registro().registrar(datos("marta@correo.com", "15111222", "jefe", "Constructora Norte"));
        assertTrue(cuenta.perteneceAEmpresa(), "El jefe queda dentro de su empresa al registrarse");
        return (Jefe) cuenta.getUsuario();
    }

    @Test
    @DisplayName("Un jefe se registra con su empresa: entra sin código y es Jefe de una empresa nueva")
    void jefeCreaSuEmpresa() {
        Jefe marta = registrarJefaNorte();
        assertEquals("Constructora Norte", marta.getEmpresa().getNombre());
        assertEquals(2, d.empresa.getPlataforma().listarEmpresas().size());
        assertEquals(HOY, marta.getFechaIngreso());
        assertFalse(servicios.autenticacion().ingresar("marta@correo.com", "clave123").pendienteDeUnirse());
        assertSame(marta, servicios.autenticacion().ingresar("15111222", "clave123").usuario());
    }

    @Test
    @DisplayName("Las empresas están aisladas: el jefe de una no ve ni toca nada de la otra")
    void aislamiento() {
        Jefe marta = registrarJefaNorte();
        assertEquals(1, servicios.personal().listarVisibles(marta).size(), "Solo se ve a sí misma");
        assertEquals(5, servicios.personal().listarVisibles(d.jefe).size(), "Carlos no ve a Marta");
        assertThrows(NoEncontradoException.class, () -> servicios.personal().ver(marta, "30000001"),
                "Una cédula de otra empresa «no existe»");

        var reposo = servicios.incidencias().reportar(d.jose, "PERMISO_PERSONAL", "30000001", HOY, null, "Trámite", Map.of());
        assertTrue(servicios.incidencias().listarVisibles(marta).isEmpty());
        assertThrows(RuntimeException.class, () -> servicios.incidencias().revisar(marta, reposo.getId(),
                EstadoIncidencia.APROBADA, "ok"), "No puede aprobar incidencias de otra empresa");
        assertFalse(marta.puedeRevisar(reposo));
        assertFalse(marta.puedeAprobarHorasDe(d.jose));
        assertThrows(PermisoDenegadoException.class, () -> marta.revisarHorasExtra(d.jose, HOY, EstadoHorasExtra.APROBADAS));
        assertThrows(NoEncontradoException.class, () -> servicios.obras().asignar(marta, "30000001", ""));
        var tabla = servicios.supervision().estadisticas(marta, "mes").resumenes();
        assertEquals(1, tabla.size(), "En la tabla solo aparece ella");
        assertEquals(marta.getCedula(), tabla.get(0).cedula());
        assertTrue(servicios.invitaciones().listar(marta).isEmpty());
    }

    @Test
    @DisplayName("Con un código de la empresa Norte, la persona queda en Norte (no en la otra)")
    void unirseALaEmpresaDelCodigo() {
        Jefe marta = registrarJefaNorte();
        var inv = servicios.invitaciones().crear(marta, "Empleado", "", "", 7, "");
        Cuenta pablo = servicios.registro().registrar(datos("pablo@correo.com", "28555111", null, null));
        Usuario ficha = servicios.registro().unirse(pablo, inv.getCodigo().toString());
        assertEquals("Constructora Norte", ficha.getEmpresa().getNombre());
        assertEquals(2, servicios.personal().listarVisibles(marta).size());
        assertEquals(5, servicios.personal().listarVisibles(d.jefe).size());
    }

    @Test
    @DisplayName("La cédula es única en todo el sistema y el nombre de empresa no se repite")
    void unicidad() {
        registrarJefaNorte();
        assertThrows(OperacionInvalidaException.class, () -> servicios.registro().registrar(
                datos("otra@correo.com", "30000001", null, null)), "La cédula de José ya existe en otra empresa");
        assertThrows(OperacionInvalidaException.class, () -> servicios.registro().registrar(
                datos("x@correo.com", "17000111", "jefe", "constructora norte")), "Nombre repetido");
        assertThrows(RuntimeException.class, () -> servicios.registro().registrar(
                datos("y@correo.com", "17000222", "jefe", "AB")), "Nombre muy corto");
        assertEquals(null, d.empresa.getPlataforma().buscarCuenta("x@correo.com"),
                "Si la empresa no se puede crear, tampoco queda la cuenta");
    }

    @Test
    @DisplayName("Una cuenta ya registrada puede crear su empresa después")
    void crearEmpresaDespues() {
        Cuenta cuenta = servicios.registro().registrar(datos("luisa@correo.com", "19222333", null, null));
        assertFalse(cuenta.perteneceAEmpresa());
        Jefe luisa = servicios.registro().crearEmpresa(cuenta, "Obras Luisa");
        assertNotNull(luisa.getEmpresa());
        assertThrows(OperacionInvalidaException.class, () -> servicios.registro().crearEmpresa(cuenta, "Otra más"));
    }

    @Test
    @DisplayName("Guardar y cargar conserva varias empresas con sus personas y cuentas")
    void persistenciaVariasEmpresas() {
        Jefe marta = registrarJefaNorte();
        servicios.obras().crear(marta, Map.of("nombre", "Galpón Norte", "ubicacion", "Maracay",
                "inicio", HOY.toString(), "fin", HOY.plusMonths(3).toString()));
        FabricaIncidencias fabrica = FabricaIncidencias.conTiposPorDefecto();
        Path archivo = carpeta.resolve("plataforma.txt");
        new RepositorioTexto(archivo, () -> d.empresa, fabrica).guardar(d.empresa.getPlataforma());
        var cargada = new RepositorioTexto(archivo, () -> null, fabrica).cargar();
        assertEquals(2, cargada.listarEmpresas().size());
        Empresa norte = cargada.listarEmpresas().get(1);
        assertEquals("Constructora Norte", norte.getNombre());
        assertEquals(1, norte.listarObras().size());
        assertSame(norte, cargada.autenticar("15111222", "clave123").getEmpresa());
        assertSame(norte, cargada.buscarCuenta("marta@correo.com").getUsuario().getEmpresa());
    }

    @Test
    @DisplayName("Las cifras de la portada suman todas las empresas sin exponer sus datos")
    void cifrasPublicasAgregadas() {
        Jefe marta = registrarJefaNorte();
        servicios.obras().crear(marta, Map.of("nombre", "Galpón Norte", "ubicacion", "Maracay",
                "inicio", HOY.toString(), "fin", HOY.plusMonths(3).toString()));
        var cifras = servicios.personal().cifrasPublicas();
        assertEquals(2, cifras.empresas());
        assertEquals(6, cifras.personas());
        assertEquals(1, cifras.obras());
    }
}
