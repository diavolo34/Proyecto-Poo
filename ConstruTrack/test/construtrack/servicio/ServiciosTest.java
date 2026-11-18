package construtrack.servicio;

import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.CorreoFalso;
import construtrack.Datos;
import construtrack.modelo.excepciones.CredencialesInvalidasException;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.persistencia.RepositorioMemoria;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServiciosTest {
    private final Datos d = new Datos();
    private final RepositorioMemoria repositorio = new RepositorioMemoria(d.empresa);
    private final CorreoFalso correo = new CorreoFalso();
    private final Servicios servicios = new Servicios(d.empresa.getPlataforma(), repositorio, Datos.RELOJ, 1, correo);

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
        assertEquals(5, servicios.personal().listarVisibles(d.gerente).size());
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
        var inv = servicios.invitaciones().crear(d.gerente, "Empleado", "20000001", "", 7, "");
        assertThrows(OperacionInvalidaException.class, () -> servicios.registro().unirse(cuenta, inv.getCodigo().toString()),
                "Sin confirmar el correo no puede unirse");
        servicios.registro().confirmarCorreo(cuenta, correo.ultimoCodigo());
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
        assertThrows(DatoInvalidoException.class, () -> servicios.personal().registrar(d.gerente, Map.of("rol", "Empleado",
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
    @DisplayName("Obras: supervisor y gerente las gestionan; no se elimina una obra con personas")
    void gestionDeObras() {
        Map<String, String> datos = Map.of("nombre", "Edificio Las Acacias", "ubicacion", "Caracas",
                "inicio", HOY.toString(), "fin", HOY.plusMonths(6).toString());
        assertThrows(PermisoDenegadoException.class, () -> servicios.obras().crear(d.jose, datos));
        var obra = servicios.obras().crear(d.ana, datos);
        assertEquals("OB-01", obra.getCodigo());
        assertEquals("OB-02", servicios.obras().crear(d.gerente, Map.of("nombre", "Puente Norte", "ubicacion", "",
                "inicio", HOY.toString(), "fin", HOY.plusMonths(2).toString())).getCodigo());

        servicios.obras().asignar(d.ana, "30000001", "OB-01");
        assertEquals(obra, d.jose.getObra());
        assertThrows(PermisoDenegadoException.class, () -> servicios.obras().asignar(d.ana, "30000004", "OB-01"),
                "Laura no es de su equipo");
        assertThrows(OperacionInvalidaException.class, () -> servicios.obras().eliminar(d.gerente, "OB-01"));
        servicios.obras().asignar(d.gerente, "30000001", "");
        servicios.obras().eliminar(d.gerente, "OB-01");
        assertEquals(1, servicios.obras().listar(d.gerente).size());
        assertThrows(DatoInvalidoException.class, () -> servicios.obras().crear(d.gerente, Map.of("nombre", "Mal",
                "inicio", HOY.toString(), "fin", HOY.minusDays(1).toString())));
    }

    @Test
    @DisplayName("Las revisadas salen del tablero a las 3 horas, pero siguen en el reporte de incidencias")
    void tableroArchivaYReporteConserva() {
        Incidencia aprobada = servicios.incidencias().reportar(d.jose, "PERMISO_PERSONAL", "30000001", HOY, null, "Trámite", Map.of());
        Incidencia rechazada = servicios.incidencias().reportar(d.laura, "PERMISO_PERSONAL", "30000004", HOY, null, "Viaje", Map.of());
        Incidencia pendiente = servicios.incidencias().reportar(d.ana, "AUSENCIA", "30000001", HOY.minusDays(40), null, "Faltó", Map.of());
        servicios.incidencias().revisar(d.gerente, aprobada.getId(), EstadoIncidencia.APROBADA, "Ok");
        servicios.incidencias().revisar(d.gerente, rechazada.getId(), EstadoIncidencia.RECHAZADA, "Sin soporte");
        assertEquals(3, servicios.incidencias().listarTablero(d.gerente).size(), "Recién revisadas siguen visibles");

        Servicios tresHorasDespues = new Servicios(d.empresa, repositorio,
                Clock.offset(Datos.RELOJ, Duration.ofHours(Incidencia.HORAS_EN_TABLERO)), 100);
        List<Incidencia> tablero = tresHorasDespues.incidencias().listarTablero(d.gerente);
        assertEquals(List.of(pendiente), tablero);
        tablero.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        assertEquals(3, tresHorasDespues.incidencias().listarVisibles(d.gerente).size());

        String todo = tresHorasDespues.supervision().exportar(d.gerente, "incidencias", "csv", "todo").contenido();
        assertTrue(todo.contains("Sin soporte") && todo.contains("Faltó") && todo.contains("Carlos Méndez"), todo);
        String semana = tresHorasDespues.supervision().exportar(d.gerente, "incidencias", "csv", "semana").contenido();
        assertFalse(semana.contains("Faltó"), "Un período corto no trae lo de hace 40 días");
    }

    @Test
    @DisplayName("La tabla de puntualidad del gerente incluye al gerente y a los supervisores; la del supervisor, a él y su equipo")
    void tablaIncluyeLideres() {
        var delGerente = servicios.supervision().estadisticas(d.gerente, "semana").resumenes().stream().map(r -> r.cedula()).toList();
        assertEquals(5, delGerente.size());
        assertTrue(delGerente.containsAll(List.of("10000001", "20000001", "20000002")));
        var deAna = servicios.supervision().estadisticas(d.ana, "semana").resumenes().stream().map(r -> r.cedula()).toList();
        assertEquals(List.of("20000001", "30000001"), deAna);
    }

    @Test
    @DisplayName("Cada líder solo ve las horas extra que puede aprobar")
    void horasExtraSegunPermiso() {
        for (var persona : List.of(d.ana, d.jose)) {
            RegistroAsistencia r = new RegistroAsistencia(HOY.minusDays(1), LocalTime.of(7, 0), false);
            r.registrarSalida(LocalTime.of(19, 0));
            persona.agregarAsistencia(r);
        }
        assertEquals(List.of("José García"), servicios.supervision().horasExtraPendientes(d.ana).stream()
                .map(h -> h.persona().getNombre()).toList(), "Ana no aprueba sus propias horas");
        assertEquals(2, servicios.supervision().horasExtraPendientes(d.gerente).size());
    }
}
