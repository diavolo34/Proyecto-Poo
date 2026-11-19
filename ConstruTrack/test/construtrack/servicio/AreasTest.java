package construtrack.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.CorreoFalso;
import construtrack.Datos;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.novedades.FabricaNovedades;
import construtrack.modelo.talento.TipoEgreso;
import construtrack.persistencia.AlmacenArchivosMemoria;
import construtrack.persistencia.AlmacenMemoria;
import construtrack.persistencia.RepositorioMemoria;
import construtrack.persistencia.RepositorioTexto;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AreasTest {
    private final Datos d = new Datos();
    private final Servicios servicios = new Servicios(d.empresa.getPlataforma(), new RepositorioMemoria(d.empresa), Datos.RELOJ, 1,
            new CorreoFalso(), Runnable::run, new AlmacenArchivosMemoria());

    private void plomeriaDeAna() {
        servicios.areas().crear(d.gerente, "Plomería", "20000001");
    }

    @Test
    @DisplayName("Solo el gerente crea un área, y siempre con un supervisor activo como responsable")
    void crear() {
        assertThrows(PermisoDenegadoException.class, () -> servicios.areas().crear(d.ana, "Plomería", "20000001"));
        assertThrows(DatoInvalidoException.class, () -> servicios.areas().crear(d.gerente, "Plomería", "30000001"),
                "Un empleado no puede ser el responsable");
        plomeriaDeAna();
        assertThrows(OperacionInvalidaException.class, () -> servicios.areas().crear(d.gerente, "plomería", "20000002"));
        assertEquals("Plomería", d.ana.getArea(), "El supervisor queda en su área");
        assertTrue(d.empresa.listarAreas().get(0).esDirigidaPor(d.ana));
    }

    @Test
    @DisplayName("El área manda: al incluir a un empleado, su supervisor pasa a ser el del área")
    void elAreaDefineElSupervisor() {
        plomeriaDeAna();
        servicios.areas().crear(d.gerente, "Transporte", "20000002");
        servicios.areas().incluir(d.gerente, "30000004", "Plomería");
        assertEquals(d.ana, d.laura.getSupervisor());
        assertTrue(d.ana.supervisa(d.laura));
        servicios.areas().incluir(d.gerente, "30000004", "Transporte");
        assertEquals(d.luis, d.laura.getSupervisor(), "Al moverla cambia de supervisor");
        assertFalse(d.ana.supervisa(d.laura));
        servicios.areas().cambiarSupervisor(d.gerente, "Transporte", "20000001");
        assertEquals(d.ana, d.laura.getSupervisor(), "Si cambia el supervisor del área, cambia para todos sus integrantes");
        assertThrows(OperacionInvalidaException.class, () -> servicios.areas().incluir(d.gerente, "10000001", "Plomería"),
                "El gerente no pertenece a ningún área");
    }

    @Test
    @DisplayName("El supervisor suma a su área solo a personas sin área; mover o sacar lo hace el gerente")
    void permisosDelSupervisor() {
        plomeriaDeAna();
        servicios.areas().crear(d.gerente, "Transporte", "20000002");
        servicios.areas().incluir(d.ana, "30000001", "Plomería");
        assertEquals(d.ana, d.jose.getSupervisor());
        assertThrows(PermisoDenegadoException.class, () -> servicios.areas().incluir(d.ana, "30000004", "Transporte"),
                "No puede sumar personas al área de otro supervisor");
        servicios.areas().incluir(d.luis, "30000004", "Transporte");
        assertThrows(PermisoDenegadoException.class, () -> servicios.areas().incluir(d.ana, "30000004", "Plomería"),
                "Laura ya tiene área: solo el gerente la puede mover");
        assertThrows(PermisoDenegadoException.class, () -> servicios.areas().sacar(d.ana, "30000001"));
        servicios.areas().sacar(d.gerente, "30000001");
        assertNull(d.jose.getArea());
        assertNull(d.jose.getSupervisor());
        assertEquals(1, servicios.areas().ver(d.ana).areas().size(), "El supervisor solo ve sus áreas");
        assertEquals(2, servicios.areas().ver(d.gerente).areas().size());
    }

    @Test
    @DisplayName("Al eliminar un área, sus integrantes quedan sin área y sin supervisor")
    void eliminar() {
        plomeriaDeAna();
        servicios.areas().incluir(d.gerente, "30000001", "Plomería");
        assertEquals(2, servicios.areas().eliminar(d.gerente, "Plomería").size(), "Ana y José quedan sin área");
        assertNull(d.jose.getArea());
        assertNull(d.jose.getSupervisor());
        assertTrue(d.empresa.listarAreas().isEmpty());
        assertTrue(servicios.areas().ver(d.gerente).sinArea().contains(d.jose));
    }

    @Test
    @DisplayName("El responsable pertenece a un área que dirige: no se le mueve fuera ni se le saca mientras la dirija")
    void responsableEnSuArea() {
        plomeriaDeAna();
        servicios.areas().crear(d.gerente, "Electricidad", "20000001");
        servicios.areas().crear(d.gerente, "Transporte", "20000002");
        assertEquals("Plomería", d.ana.getArea());
        assertThrows(OperacionInvalidaException.class, () -> servicios.areas().sacar(d.gerente, "20000001"));
        assertThrows(OperacionInvalidaException.class, () -> servicios.areas().incluir(d.gerente, "20000001", "Transporte"));
        servicios.areas().incluir(d.gerente, "20000001", "Electricidad");
        assertEquals("Electricidad", d.ana.getArea(), "Puede pasar a otra de las áreas que dirige");
        servicios.areas().cambiarSupervisor(d.gerente, "Electricidad", "20000002");
        assertEquals("Plomería", d.ana.getArea(), "Al dejar de dirigirla, vuelve a un área que sí dirige");
        assertEquals("Transporte", d.luis.getArea());
        assertEquals(1, servicios.areas().eliminar(d.gerente, "Plomería").size(), "Ana ya no dirige ninguna y queda sin área");
        assertNull(d.ana.getArea());
    }

    @Test
    @DisplayName("Si el supervisor egresa, el área queda sin supervisor y el gerente recibe la alerta")
    void supervisorQueEgresa() {
        plomeriaDeAna();
        servicios.areas().incluir(d.gerente, "30000001", "Plomería");
        Map<String, String> datos = new HashMap<>();
        datos.put("tipo", TipoEgreso.DESPIDO.name());
        datos.put("fecha", Datos.HOY.toString());
        datos.put("motivo", "Reestructuración del área");
        datos.put("voucher", servicios.archivos().subir(d.gerente, "voucher.pdf", "%PDF-1.4 x".getBytes(), "20000001", true).id());
        datos.put("devolvioEquipos", "true");
        servicios.talento().registrarEgreso(d.gerente, "20000001", datos);
        assertFalse(d.empresa.buscarArea("Plomería").tieneSupervisor());
        assertNull(d.jose.getSupervisor());
        assertTrue(servicios.supervision().alertas(d.gerente, "semana").stream()
                .anyMatch(a -> a.getNombre().equals("Área Plomería sin supervisor")));
        servicios.areas().cambiarSupervisor(d.gerente, "Plomería", "20000002");
        assertEquals(d.luis, d.jose.getSupervisor());
    }

    @Test
    @DisplayName("Las áreas y su supervisor se guardan; las áreas antiguas sin supervisor se descartan al cargar")
    void persistencia() {
        plomeriaDeAna();
        servicios.areas().incluir(d.gerente, "30000001", "Plomería");
        AlmacenMemoria almacen = new AlmacenMemoria();
        FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();
        new RepositorioTexto(almacen, () -> null, fabrica).guardar(d.empresa.getPlataforma());
        Plataforma cargada = new RepositorioTexto(almacen, () -> null, fabrica).cargar();
        var empresa = cargada.listarEmpresas().get(0);
        assertEquals("Ana Rodríguez", empresa.buscarArea("plomería").getSupervisor().getNombre());
        Usuario jose = empresa.buscarUsuario("30000001");
        assertEquals("Plomería", jose.getArea());

        String antiguo = almacen.leer().orElseThrow().replace("AR|Plomería|20000001", "K|A|Plomería");
        almacen.escribir(antiguo);
        var vieja = new RepositorioTexto(almacen, () -> null, fabrica).cargar().listarEmpresas().get(0);
        assertTrue(vieja.listarAreas().isEmpty());
        assertNull(vieja.buscarUsuario("30000001").getArea(), "La persona queda sin área");
    }
}
