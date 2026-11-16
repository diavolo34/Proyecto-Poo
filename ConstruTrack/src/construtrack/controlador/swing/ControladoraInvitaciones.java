package construtrack.controlador.swing;

import construtrack.modelo.Invitacion;
import construtrack.modelo.Obra;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.ServicioInvitaciones;
import construtrack.servicio.ServicioPersonal;
import construtrack.vista.swing.Opcion;
import construtrack.vista.swing.PanelInvitaciones;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JOptionPane;

/** Controladora de invitaciones (solo Jefe): crear, copiar el código y cancelar. */
public final class ControladoraInvitaciones {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PanelInvitaciones vista;
    private final ServicioInvitaciones invitaciones;
    private final ServicioPersonal personal;
    private final Usuario usuario;

    public ControladoraInvitaciones(PanelInvitaciones vista, ServicioInvitaciones invitaciones,
            ServicioPersonal personal, Usuario usuario) {
        this.vista = vista;
        this.invitaciones = invitaciones;
        this.personal = personal;
        this.usuario = usuario;
        vista.getBtnCrear().addActionListener(e -> crear());
        vista.getBtnCopiar().addActionListener(e -> copiar());
        vista.getBtnCancelar().addActionListener(e -> cancelar());
    }

    private void crear() {
        try {
            String rol = String.valueOf(vista.getCboRol().getSelectedItem());
            Opcion supervisor = (Opcion) vista.getCboSupervisor().getSelectedItem();
            Opcion obra = (Opcion) vista.getCboObra().getSelectedItem();
            Invitacion v = invitaciones.crear(usuario, rol,
                    "Empleado".equals(rol) && supervisor != null ? supervisor.valor() : "",
                    obra == null ? "" : obra.valor(), (Integer) vista.getSpnDias().getValue(),
                    vista.getTxtCorreo().getText());
            vista.mostrarCodigo(v.getCodigo().toString());
            vista.mostrarMensaje("invitación creada. Vence el " + FECHA.format(v.getVence()) + ".", false);
            vista.getTxtCorreo().setText("");
            refrescar();
        } catch (ConstruTrackException ex) {
            vista.mostrarMensaje(ex.getMessage(), true);
        }
    }

    private void copiar() {
        String codigo = vista.getLblCodigo().getText();
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(codigo), null);
            vista.mostrarMensaje("código " + codigo + " copiado. Pégalo en WhatsApp o en un correo.", false);
        } catch (IllegalStateException ex) {
            vista.mostrarMensaje("no se pudo copiar automáticamente. El código es " + codigo + ".", true);
        }
    }

    private void cancelar() {
        int fila = vista.getTabla().getSelectedRow();
        if (fila < 0) {
            vista.mostrarMensaje("selecciona una invitación en la tabla.", true);
            return;
        }
        String codigo = String.valueOf(vista.getModelo().getValueAt(vista.getTabla().convertRowIndexToModel(fila), 0));
        if (JOptionPane.showConfirmDialog(vista, "¿Cancelar la invitación " + codigo + "? Ya no se podrá usar.",
                "Cancelar invitación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            invitaciones.cancelar(usuario, codigo);
            vista.mostrarMensaje("la invitación " + codigo + " fue cancelada.", false);
            refrescar();
        } catch (ConstruTrackException ex) {
            vista.mostrarMensaje(ex.getMessage(), true);
        }
    }

    public void refrescar() {
        Object supervisorElegido = vista.getCboSupervisor().getSelectedItem();
        vista.getCboSupervisor().removeAllItems();
        vista.getCboSupervisor().addItem(new Opcion("", "— Sin supervisor —"));
        for (Supervisor s : personal.listarSupervisores(usuario)) {
            vista.getCboSupervisor().addItem(new Opcion(s.getCedula(), s.getNombre()));
        }
        if (supervisorElegido != null) {
            vista.getCboSupervisor().setSelectedItem(supervisorElegido);
        }
        if (vista.getCboObra().getItemCount() == 0) {
            vista.getCboObra().addItem(new Opcion("", "— Sin obra —"));
            for (Obra o : personal.listarObras(usuario)) {
                vista.getCboObra().addItem(new Opcion(o.getCodigo(), o.getNombre()));
            }
        }
        LocalDateTime ahora = invitaciones.ahora();
        vista.getModelo().setRowCount(0);
        for (Invitacion v : invitaciones.listar(usuario)) {
            vista.getModelo().addRow(new Object[] {v.getCodigo(), v.getRol(),
                v.getSupervisor() == null ? "—" : v.getSupervisor().getNombre(),
                v.getObra() == null ? "—" : v.getObra().getNombre(), v.estadoEn(ahora).getNombre(),
                FECHA.format(v.getVence()), v.getUsadaPor() == null ? "—" : v.getUsadaPor().getNombreCompleto()});
        }
    }
}
