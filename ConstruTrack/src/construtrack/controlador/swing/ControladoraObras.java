package construtrack.controlador.swing;

import construtrack.modelo.Obra;
import construtrack.modelo.RetrasoObra;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.ServicioObras;
import construtrack.vista.swing.Dialogos;
import construtrack.vista.swing.PanelObras;
import construtrack.vista.swing.PanelTabla;
import construtrack.vista.swing.Tema;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public final class ControladoraObras {
    private final PanelObras vista;
    private final ServicioObras obras;
    private final Usuario usuario;
    private final Runnable alCambiar;

    public ControladoraObras(PanelObras vista, ServicioObras obras, Usuario usuario, Runnable alCambiar) {
        this.vista = vista;
        this.obras = obras;
        this.usuario = usuario;
        this.alCambiar = alCambiar;
        vista.getBtnIniciar().addActionListener(e -> cambiarEstado(true));
        vista.getBtnConcluir().addActionListener(e -> cambiarEstado(false));
        vista.getBtnRetraso().addActionListener(e -> reportarRetraso());
        vista.getBtnRevisado().addActionListener(e -> marcarRevisado());
    }

    private String obraSeleccionada() {
        Object codigo = vista.getObras().valorSeleccionado(0);
        if (codigo == null) {
            vista.getObras().mostrarMensaje("selecciona una obra en la tabla.", true);
        }
        return codigo == null ? null : codigo.toString();
    }

    private void cambiarEstado(boolean iniciar) {
        String codigo = obraSeleccionada();
        if (codigo == null) {
            return;
        }
        String nombre = String.valueOf(vista.getObras().valorSeleccionado(1));
        if (!Dialogos.confirmar(vista, iniciar ? "Inicio de obra" : "Conclusión de la obra", "¿Registrar hoy "
                + (iniciar ? "el inicio" : "la conclusión") + " de «" + nombre + "»?")) {
            return;
        }
        try {
            Obra obra = iniciar ? obras.iniciar(usuario, codigo) : obras.concluir(usuario, codigo);
            vista.getObras().mostrarMensaje("«" + obra.getNombre() + "» está " + obra.getEstado().getNombre().toLowerCase() + ".", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.getObras().mostrarMensaje(e.getMessage(), true);
        }
    }

    private void reportarRetraso() {
        String codigo = obraSeleccionada();
        if (codigo == null) {
            return;
        }
        JTextArea motivo = new JTextArea(3, 30);
        motivo.setLineWrap(true);
        motivo.setWrapStyleWord(true);
        JTextField fecha = new JTextField(10);
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(new JLabel("Fin estimado actual: " + vista.getObras().valorSeleccionado(4)));
        panel.add(new JLabel("Motivo del retraso:"));
        panel.add(Tema.desplazable(motivo));
        panel.add(new JLabel("Nueva fecha estimada de conclusión (AAAA-MM-DD):"));
        panel.add(fecha);
        if (!Dialogos.formulario(vista, "Reportar retraso", panel)) {
            return;
        }
        try {
            RetrasoObra r = obras.reportarRetraso(usuario, codigo, motivo.getText(), fecha.getText().trim());
            vista.getObras().mostrarMensaje("retraso reportado (+" + r.diasDeRetraso() + " días). El gerente ya tiene la alerta.", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.getObras().mostrarMensaje(e.getMessage(), true);
        }
    }

    private void marcarRevisado() {
        PanelTabla tabla = vista.getRetrasos();
        Object codigo = tabla.valorSeleccionado(0);
        if (codigo == null) {
            tabla.mostrarMensaje("selecciona un retraso en la tabla.", true);
            return;
        }
        try {
            String obra = codigo.toString().split(" · ")[0];
            obras.revisarRetraso(usuario, obra, (Integer) tabla.valorSeleccionado(1));
            tabla.mostrarMensaje("retraso marcado como revisado.", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            tabla.mostrarMensaje(e.getMessage(), true);
        }
    }

    public void refrescar() {
        vista.getObras().limpiar();
        vista.getRetrasos().limpiar();
        for (ServicioObras.ObraConPersonas op : obras.listar(usuario)) {
            Obra o = op.obra();
            vista.getObras().fila(o.getCodigo(), o.getNombre(), o.getEstado().getNombre(), o.getFechaInicio(), o.getFechaFinEstimada(),
                    op.personas().size(), o.retrasosSinRevisar().size());
            for (RetrasoObra r : o.getRetrasos()) {
                vista.getRetrasos().fila(o.getCodigo() + " · " + o.getNombre(), r.getId(), r.getFecha().toLocalDate(),
                        r.getReportadoPor().nombre(), r.getFinAnterior(), r.getFinNuevo(), r.diasDeRetraso(), r.getMotivo(),
                        r.getRevisadoPor() == null ? "Sin revisar" : r.getRevisadoPor().nombre());
            }
        }
    }
}
