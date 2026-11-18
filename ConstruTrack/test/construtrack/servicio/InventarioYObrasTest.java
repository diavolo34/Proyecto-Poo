package construtrack.servicio;

import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.CorreoFalso;
import construtrack.Datos;
import construtrack.RelojAjustable;
import construtrack.modelo.CodigoInvitacion;
import construtrack.modelo.EstadoObra;
import construtrack.modelo.Invitacion;
import construtrack.modelo.Obra;
import construtrack.modelo.Plataforma;
import construtrack.modelo.RetrasoObra;
import construtrack.modelo.alertas.Alerta;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.inventario.Articulo;
import construtrack.modelo.inventario.Asignacion;
import construtrack.modelo.inventario.EstadoArticulo;
import construtrack.modelo.novedades.FabricaNovedades;
import construtrack.persistencia.AlmacenMemoria;
import construtrack.persistencia.RepositorioMemoria;
import construtrack.persistencia.RepositorioTexto;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InventarioYObrasTest {
    private final Datos d = new Datos();
    private final RelojAjustable reloj = new RelojAjustable(Datos.RELOJ);
    private final Servicios servicios = new Servicios(d.empresa.getPlataforma(), new RepositorioMemoria(d.empresa), reloj, 1,
            new CorreoFalso());

    private Articulo andamios() {
        return servicios.inventario().crear(d.gerente, Map.of("categoria", "EQUIPO", "nombre", "Andamio tubular", "cantidad", "10"));
    }

    @Test
    @DisplayName("El gerente carga el inventario, entrega por cantidades y no puede entregar más de lo disponible")
    void entregas() {
        Articulo andamio = andamios();
        assertEquals("EQ-001", andamio.getCodigo());
        assertThrows(PermisoDenegadoException.class, () -> servicios.inventario().crear(d.ana, Map.of("categoria", "EQUIPO",
                "nombre", "Andamio", "cantidad", "1")));
        servicios.inventario().asignar(d.gerente, "EQ-001", Map.of("cedula", "20000001", "cantidad", "6"));
        assertEquals(4, andamio.disponibles());
        assertThrows(OperacionInvalidaException.class, () -> servicios.inventario().asignar(d.gerente, "EQ-001",
                Map.of("cedula", "30000001", "cantidad", "5")));
        assertThrows(OperacionInvalidaException.class, () -> servicios.inventario().editar(d.gerente, "EQ-001",
                Map.of("nombre", "Andamio tubular", "cantidad", "3")), "No puede quedar menos de lo entregado");
        assertEquals(1, servicios.inventario().aCargoDe(d.ana, "20000001").size());
        assertThrows(PermisoDenegadoException.class, () -> servicios.inventario().aCargoDe(d.jose, "20000001"));
    }

    @Test
    @DisplayName("La devolución registra el estado: con daños pasa a mantenimiento; extraviado reduce el inventario")
    void devoluciones() {
        Articulo taladro = servicios.inventario().crear(d.gerente, Map.of("categoria", "HERRAMIENTA", "nombre", "Taladro",
                "serie", "BSH-1", "cantidad", "1"));
        Asignacion a = servicios.inventario().asignar(d.gerente, taladro.getCodigo(), Map.of("cedula", "30000001",
                "devolucionPrevista", HOY.plusDays(1).toString()));
        assertThrows(DatoInvalidoException.class, () -> servicios.inventario().devolver(d.gerente, a.getId(), "DANADA", ""));
        servicios.inventario().devolver(d.gerente, a.getId(), "DANADA", "Mandril partido");
        assertEquals(EstadoArticulo.EN_MANTENIMIENTO, taladro.getEstado());
        assertThrows(OperacionInvalidaException.class, () -> servicios.inventario().asignar(d.gerente, taladro.getCodigo(),
                Map.of("cedula", "30000001")));

        Articulo andamio = andamios();
        Asignacion b = servicios.inventario().asignar(d.gerente, andamio.getCodigo(), Map.of("cedula", "20000001", "cantidad", "2"));
        servicios.inventario().devolver(d.gerente, b.getId(), "EXTRAVIADA", "Robado en la obra");
        assertEquals(8, andamio.getCantidad());
        assertThrows(OperacionInvalidaException.class, () -> servicios.inventario().eliminar(d.gerente, andamio.getCodigo()),
                "Con historial no se elimina, se da de baja");
    }

    @Test
    @DisplayName("Las devoluciones vencidas y los retrasos de obra llegan como alerta al gerente")
    void alertasDelGerente() {
        Articulo andamio = andamios();
        servicios.inventario().asignar(d.gerente, andamio.getCodigo(), Map.of("cedula", "30000001",
                "devolucionPrevista", HOY.toString()));
        Obra obra = servicios.obras().crear(d.ana, Map.of("nombre", "Torre Este", "inicio", HOY.toString(),
                "fin", HOY.plusMonths(3).toString()));
        assertEquals(EstadoObra.PLANIFICADA, obra.getEstado());
        assertThrows(OperacionInvalidaException.class, () -> servicios.obras().concluir(d.ana, obra.getCodigo()));
        servicios.obras().iniciar(d.ana, obra.getCodigo());
        assertEquals(EstadoObra.EN_CURSO, obra.getEstado());

        assertThrows(PermisoDenegadoException.class, () -> servicios.obras().reportarRetraso(d.gerente, obra.getCodigo(),
                "Lluvias fuertes", HOY.plusMonths(4).toString()));
        assertThrows(DatoInvalidoException.class, () -> servicios.obras().reportarRetraso(d.ana, obra.getCodigo(),
                "Lluvias fuertes", HOY.plusMonths(2).toString()), "La nueva fecha debe ser posterior");
        RetrasoObra retraso = servicios.obras().reportarRetraso(d.ana, obra.getCodigo(), "Lluvias fuertes en la excavación",
                HOY.plusMonths(4).toString());
        assertEquals(HOY.plusMonths(4), obra.getFechaFinEstimada());

        reloj.avanzar(Duration.ofDays(2));
        List<Alerta> alertas = servicios.supervision().alertas(d.gerente, "semana");
        assertTrue(alertas.stream().anyMatch(a -> a.getNombre().equals("Retraso en Torre Este") && a.getMensaje().contains("Lluvias")));
        assertTrue(alertas.stream().anyMatch(a -> a.getMensaje().contains("Debía devolver «Andamio tubular»")));
        assertTrue(servicios.supervision().alertas(d.ana, "semana").stream().noneMatch(a -> a.getNombre().startsWith("Retraso")));

        servicios.obras().revisarRetraso(d.gerente, obra.getCodigo(), retraso.getId());
        assertTrue(servicios.supervision().alertas(d.gerente, "semana").stream().noneMatch(a -> a.getNombre().startsWith("Retraso")));
        servicios.obras().concluir(d.gerente, obra.getCodigo());
        assertEquals(100, obra.porcentajeTranscurrido(HOY));
        assertThrows(OperacionInvalidaException.class, () -> servicios.obras().asignar(d.gerente, "30000001", obra.getCodigo()),
                "Una obra concluida no admite personal");
    }

    @Test
    @DisplayName("Las invitaciones vencidas o canceladas se borran solas; las usadas se conservan 7 días")
    void invitacionesCaducadas() {
        Invitacion vigente = d.gerente.crearInvitacion("Empleado", "", "", 7, Datos.AHORA, new Random(1));
        Invitacion vence = d.gerente.crearInvitacion("Empleado", "", "", 1, Datos.AHORA, new Random(2));
        servicios.invitaciones().cancelar(d.gerente, vigente.getCodigo().toString());
        assertEquals(1, servicios.invitaciones().listar(d.gerente).size(), "La cancelada desaparece");
        reloj.avanzar(Duration.ofDays(2));
        assertTrue(servicios.invitaciones().listar(d.gerente).isEmpty(), "La vencida desaparece");
        assertEquals(null, d.empresa.buscarInvitacion(CodigoInvitacion.de(vence.getCodigo().toString())));
    }

    @Test
    @DisplayName("Inventario, entregas, ciclo de obra y retrasos se guardan y se cargan")
    void persistencia() {
        Articulo andamio = andamios();
        Asignacion a = servicios.inventario().asignar(d.gerente, andamio.getCodigo(), Map.of("cedula", "20000001", "cantidad", "3",
                "nota", "Fachada | norte"));
        servicios.inventario().devolver(d.gerente, a.getId(), "BUENA", "");
        servicios.inventario().asignar(d.gerente, andamio.getCodigo(), Map.of("cedula", "30000001", "cantidad", "2"));
        Obra obra = servicios.obras().crear(d.ana, Map.of("nombre", "Torre Este", "inicio", HOY.toString(),
                "fin", HOY.plusMonths(3).toString()));
        servicios.obras().iniciar(d.ana, obra.getCodigo());
        servicios.obras().reportarRetraso(d.ana, obra.getCodigo(), "Falta de cemento", HOY.plusMonths(5).toString());

        AlmacenMemoria almacen = new AlmacenMemoria();
        FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();
        new RepositorioTexto(almacen, () -> null, fabrica).guardar(d.empresa.getPlataforma());
        Plataforma cargada = new RepositorioTexto(almacen, () -> null, fabrica).cargar();
        var empresa = cargada.listarEmpresas().get(0);
        Articulo copia = empresa.getInventario().buscar(andamio.getCodigo());
        assertEquals(8, copia.disponibles());
        assertEquals(2, copia.getAsignaciones().size());
        assertEquals("Fachada | norte", copia.getAsignaciones().get(0).getNota());
        Obra obraCargada = empresa.buscarObra(obra.getCodigo());
        assertEquals(EstadoObra.EN_CURSO, obraCargada.getEstado());
        assertEquals(1, obraCargada.retrasosSinRevisar().size());
        assertEquals("Ana Rodríguez", obraCargada.getRetrasos().get(0).getReportadoPor().nombre());
        assertFalse(empresa.getInventario().asignaciones().isEmpty());
    }
}
