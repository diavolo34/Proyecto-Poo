package construtrack.controlador.swing;

import construtrack.modelo.Empleado;
import construtrack.modelo.Horario;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.ServicioPersonal;
import construtrack.vista.swing.DialogoUsuario;
import construtrack.vista.swing.PanelPersonal;
import java.awt.Frame;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

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
        dialogo.cargarCatalogo(usuario.getEmpresa().getCatalogo().getPuestos(),
                usuario.getEmpresa().listarAreas().stream().map(construtrack.modelo.Area::getNombre).toList());
        if (cedula != null) {
            Usuario u = personal.ver(usuario, cedula);
            Horario h = u.getHorario();
            dialogo.cargar(u.getRol(), u.getCedula(), u.getNombre(), h.getDias(), h.getHoraEntrada().toString(),
                    h.getHoraSalida().toString(), u.estaPresente());
            dialogo.cargarPuesto(u.getPuesto(), u.getArea());
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

    public void refrescar() {
        DefaultTableModel modelo = vista.getModelo();
        modelo.setRowCount(0);
        for (Usuario u : personal.listarVisibles(usuario)) {
            Horario h = u.getHorario();
            String supervisor = u instanceof Empleado e && e.getSupervisor() != null ? e.getSupervisor().getNombre() : "—";
            modelo.addRow(new Object[] {u.getCedula(), u.getNombre(), u.getRol(), u.getPuesto() == null ? "—" : u.getPuesto(),
                u.getArea() == null ? "—" : u.getArea(), u.estaPresente() ? "En el proyecto" : "Fuera", h.getDias() + " · " + h.getHoraEntrada() + "–" + h.getHoraSalida(),
                supervisor, u.getObra() == null ? "—" : u.getObra().getNombre()});
        }
    }
}
