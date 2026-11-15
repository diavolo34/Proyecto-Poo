package construtrack.controlador.swing;

import construtrack.modelo.Empleado;
import construtrack.modelo.Horario;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.ServicioPersonal;
import construtrack.vista.swing.DialogoUsuario;
import construtrack.vista.swing.Opcion;
import construtrack.vista.swing.PanelPersonal;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Controladora del personal: búsqueda en vivo y, para el Jefe, registrar, editar y eliminar. */
public final class ControladoraPersonal {

    private final PanelPersonal vista;
    private final ServicioPersonal personal;
    private final Usuario usuario;
    private final Runnable alCambiar;
    private final TableRowSorter<DefaultTableModel> ordenador;

    public ControladoraPersonal(PanelPersonal vista, ServicioPersonal personal, Usuario usuario, Runnable alCambiar) {
        this.vista = vista;
        this.personal = personal;
        this.usuario = usuario;
        this.alCambiar = alCambiar;
        this.ordenador = new TableRowSorter<>(vista.getModelo());
        vista.getTabla().setRowSorter(ordenador);
        vista.getTxtBuscar().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }
        });
        vista.getBtnRegistrar().addActionListener(e -> abrirDialogo(null));
        vista.getBtnEditar().addActionListener(e -> {
            String cedula = cedulaSeleccionada();
            if (cedula != null) {
                abrirDialogo(cedula);
            }
        });
        vista.getBtnEliminar().addActionListener(e -> eliminar());
    }

    private void filtrar() {
        String texto = vista.getTxtBuscar().getText().trim();
        ordenador.setRowFilter(texto.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
    }

    private String cedulaSeleccionada() {
        int fila = vista.getTabla().getSelectedRow();
        if (fila < 0) {
            vista.mostrarMensaje("selecciona una persona en la tabla primero.", true);
            return null;
        }
        return String.valueOf(vista.getModelo().getValueAt(vista.getTabla().convertRowIndexToModel(fila), 0));
    }

    private void abrirDialogo(String cedula) {
        Frame dueno = (Frame) SwingUtilities.getWindowAncestor(vista);
        DialogoUsuario dialogo = new DialogoUsuario(dueno, cedula != null);
        List<Opcion> supervisores = new ArrayList<>();
        for (Supervisor s : personal.listarSupervisores()) {
            supervisores.add(new Opcion(s.getCedula(), s.getNombre()));
        }
        dialogo.cargarSupervisores(supervisores);
        if (cedula != null) {
            Usuario u = personal.ver(usuario, cedula);
            Horario h = u.getHorario();
            String supervisor = u instanceof Empleado e && e.getSupervisor() != null ? e.getSupervisor().getCedula() : "";
            dialogo.cargar(u.getRol(), u.getCedula(), u.getNombre(), h.getDias(), h.getHoraEntrada().toString(),
                    h.getHoraSalida().toString(), supervisor, u.estaPresente());
        }
        dialogo.getBtnCancelar().addActionListener(e -> dialogo.dispose());
        dialogo.getBtnGuardar().addActionListener(e -> {
            try {
                Map<String, String> datos = dialogo.datos();
                Usuario guardado = cedula == null ? personal.registrar(usuario, datos)
                        : personal.actualizar(usuario, cedula, datos);
                dialogo.dispose();
                vista.mostrarMensaje((cedula == null ? "se registró a " : "se actualizó a ") + guardado.getNombre() + ".",
                        false);
                alCambiar.run();
            } catch (ConstruTrackException ex) {
                dialogo.mostrarError(ex.getMessage());
            }
        });
        dialogo.setVisible(true);
    }

    private void eliminar() {
        String cedula = cedulaSeleccionada();
        if (cedula == null) {
            return;
        }
        Usuario u = personal.ver(usuario, cedula);
        int respuesta = JOptionPane.showConfirmDialog(vista,
                "¿Eliminar a " + u.getNombre() + "? Esta acción no se puede deshacer.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            personal.eliminar(usuario, cedula);
            vista.mostrarMensaje("se eliminó a " + u.getNombre() + ".", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.mostrarMensaje(e.getMessage(), true);
        }
    }

    public void refrescar() {
        DefaultTableModel modelo = vista.getModelo();
        modelo.setRowCount(0);
        for (Usuario u : personal.listarVisibles(usuario)) {
            Horario h = u.getHorario();
            String supervisor = u instanceof Empleado e && e.getSupervisor() != null ? e.getSupervisor().getNombre() : "—";
            modelo.addRow(new Object[] {u.getCedula(), u.getNombre(), u.getRol(),
                u.estaPresente() ? "En la obra" : "Fuera", h.getDias() + " · " + h.getHoraEntrada() + "–" + h.getHoraSalida(),
                supervisor, u.getObra() == null ? "—" : u.getObra().getNombre()});
        }
    }
}
