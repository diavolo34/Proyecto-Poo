package construtrack.controlador.swing;

import construtrack.modelo.Area;
import construtrack.modelo.Empleado;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.ServicioAreas;
import construtrack.vista.swing.Dialogos;
import construtrack.vista.swing.Opcion;
import construtrack.vista.swing.PanelAreas;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class ControladoraAreas {
    private final PanelAreas vista;
    private final ServicioAreas areas;
    private final Usuario usuario;
    private final Runnable alCambiar;
    private ServicioAreas.Vista datos;

    public ControladoraAreas(PanelAreas vista, ServicioAreas areas, Usuario usuario, Runnable alCambiar) {
        this.vista = vista;
        this.areas = areas;
        this.usuario = usuario;
        this.alCambiar = alCambiar;
        vista.getBtnNueva().addActionListener(e -> nueva());
        vista.getBtnSupervisor().addActionListener(e -> cambiarSupervisor());
        vista.getBtnEliminar().addActionListener(e -> eliminar());
        vista.getBtnIncluir().addActionListener(e -> incluir());
        vista.getBtnSacar().addActionListener(e -> sacar());
    }

    private JComboBox<Opcion> comboSupervisores() {
        JComboBox<Opcion> combo = new JComboBox<>();
        for (Supervisor s : datos.supervisores()) {
            combo.addItem(new Opcion(s.getCedula(), s.getNombre()));
        }
        return combo;
    }

    private JComboBox<Opcion> comboAreas() {
        JComboBox<Opcion> combo = new JComboBox<>();
        for (ServicioAreas.AreaConIntegrantes a : datos.areas()) {
            Supervisor s = a.area().getSupervisor();
            combo.addItem(new Opcion(a.area().getNombre(), a.area().getNombre() + (s == null ? " · sin supervisor" : " · " + s.getNombre())));
        }
        return combo;
    }

    private void nueva() {
        if (datos.supervisores().isEmpty()) {
            vista.getAreas().mostrarMensaje("primero registra o invita a un supervisor: cada área necesita uno.", true);
            return;
        }
        JTextField nombre = new JTextField(20);
        JComboBox<Opcion> supervisor = comboSupervisores();
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(new JLabel("Nombre del área (por ejemplo, Plomería):"));
        panel.add(nombre);
        panel.add(new JLabel("Supervisor responsable:"));
        panel.add(supervisor);
        if (!Dialogos.formulario(vista, "Nueva área", panel)) {
            return;
        }
        try {
            Area area = areas.crear(usuario, nombre.getText(), ((Opcion) supervisor.getSelectedItem()).valor());
            vista.getAreas().mostrarMensaje("área «" + area.getNombre() + "» creada.", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.getAreas().mostrarMensaje(e.getMessage(), true);
        }
    }

    private String areaSeleccionada() {
        Object nombre = vista.getAreas().valorSeleccionado(0);
        if (nombre == null) {
            vista.getAreas().mostrarMensaje("selecciona un área en la tabla.", true);
        }
        return nombre == null ? null : nombre.toString();
    }

    private void cambiarSupervisor() {
        String area = areaSeleccionada();
        if (area == null) {
            return;
        }
        JComboBox<Opcion> supervisor = comboSupervisores();
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(new JLabel("Nuevo supervisor de «" + area + "»:"));
        panel.add(supervisor);
        panel.add(new JLabel("Todos los empleados del área pasan a su cargo."));
        if (supervisor.getItemCount() == 0 || !Dialogos.formulario(vista, "Cambiar supervisor", panel)) {
            return;
        }
        try {
            areas.cambiarSupervisor(usuario, area, ((Opcion) supervisor.getSelectedItem()).valor());
            vista.getAreas().mostrarMensaje("supervisor de «" + area + "» actualizado.", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.getAreas().mostrarMensaje(e.getMessage(), true);
        }
    }

    private void eliminar() {
        String area = areaSeleccionada();
        if (area == null || !Dialogos.confirmar(vista, "Eliminar área", "¿Eliminar el área «" + area
                + "»? Sus integrantes quedarán sin área y sin supervisor hasta que los incluyas en otra.")) {
            return;
        }
        try {
            int sueltos = areas.eliminar(usuario, area).size();
            vista.getAreas().mostrarMensaje("área eliminada; " + sueltos + " persona(s) quedaron sin área.", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.getAreas().mostrarMensaje(e.getMessage(), true);
        }
    }

    private void incluir() {
        Object cedula = vista.getPersonas().valorSeleccionado(0);
        if (cedula == null) {
            vista.getPersonas().mostrarMensaje("selecciona a una persona en la tabla.", true);
            return;
        }
        JComboBox<Opcion> area = comboAreas();
        if (area.getItemCount() == 0) {
            vista.getPersonas().mostrarMensaje("todavía no hay áreas.", true);
            return;
        }
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(new JLabel("Incluir a " + vista.getPersonas().valorSeleccionado(1) + " en:"));
        panel.add(area);
        if (!Dialogos.formulario(vista, "Incluir en un área", panel)) {
            return;
        }
        try {
            Usuario persona = areas.incluir(usuario, cedula.toString(), ((Opcion) area.getSelectedItem()).valor());
            vista.getPersonas().mostrarMensaje(persona.getNombre() + " ahora está en «" + persona.getArea() + "».", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.getPersonas().mostrarMensaje(e.getMessage(), true);
        }
    }

    private void sacar() {
        Object cedula = vista.getPersonas().valorSeleccionado(0);
        if (cedula == null) {
            vista.getPersonas().mostrarMensaje("selecciona a una persona en la tabla.", true);
            return;
        }
        try {
            Usuario persona = areas.sacar(usuario, cedula.toString());
            vista.getPersonas().mostrarMensaje(persona.getNombre() + " quedó sin área.", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.getPersonas().mostrarMensaje(e.getMessage(), true);
        }
    }

    private void fila(Usuario u) {
        Supervisor s = u instanceof Empleado e ? e.getSupervisor() : null;
        vista.getPersonas().fila(u.getCedula(), u.getNombre(), u.getRol(), u.getPuesto() == null ? "—" : u.getPuesto(),
                u.getArea() == null ? "Sin área" : u.getArea(), s == null ? "—" : s.getNombre());
    }

    public void refrescar() {
        datos = areas.ver(usuario);
        vista.getAreas().limpiar();
        vista.getPersonas().limpiar();
        for (ServicioAreas.AreaConIntegrantes a : datos.areas()) {
            Supervisor s = a.area().getSupervisor();
            vista.getAreas().fila(a.area().getNombre(), s == null ? "Sin supervisor" : s.getNombre(), a.integrantes().size());
            List<Usuario> integrantes = a.integrantes();
            integrantes.forEach(this::fila);
        }
        datos.sinArea().forEach(this::fila);
    }
}
