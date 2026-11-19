package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public final class PanelObras extends JPanel {
    private static final long serialVersionUID = 1L;

    private final PanelTabla obras = new PanelTabla("Proyectos", "Planificado → En curso con el «Inicio del proyecto» → Concluido con la "
            + "«Conclusión del proyecto». Si la fecha estimada se mueve, el supervisor reporta el retraso y el gerente recibe la alerta.",
            "Código", "Proyecto", "Estado", "Inicio", "Fin estimado", "Personas", "Retrasos sin revisar");
    private final PanelTabla retrasos = new PanelTabla("Retrasos reportados", null, "Proyecto", "Id", "Reportado", "Por", "Fin anterior",
            "Nuevo fin", "Días", "Motivo", "Revisado por");
    private final JButton btnIniciar = Tema.botonExito("Inicio del proyecto");
    private final JButton btnConcluir = Tema.botonExito("Conclusión del proyecto");
    private final JButton btnRetraso = Tema.botonPeligro("Reportar retraso");
    private final JButton btnRevisado = Tema.botonSecundario("Marcar como revisado");

    public PanelObras(boolean esSupervisor, boolean esGerente) {
        super(new GridLayout(2, 1, 0, 12));
        setBorder(Tema.margen(12, 4, 4, 4));
        obras.anchos(70, 220, 90, 90, 90, 70, 130);
        retrasos.anchos(150, 30, 90, 120, 90, 90, 50, 260, 120);
        obras.agregarBoton(btnIniciar);
        obras.agregarBoton(btnConcluir);
        if (esSupervisor) {
            obras.agregarBoton(btnRetraso);
        }
        if (esGerente) {
            retrasos.agregarBoton(btnRevisado);
        }
        add(obras);
        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        abajo.add(retrasos, BorderLayout.CENTER);
        add(abajo);
    }

    public PanelTabla getObras() {
        return obras;
    }

    public PanelTabla getRetrasos() {
        return retrasos;
    }

    public JButton getBtnIniciar() {
        return btnIniciar;
    }

    public JButton getBtnConcluir() {
        return btnConcluir;
    }

    public JButton getBtnRetraso() {
        return btnRetraso;
    }

    public JButton getBtnRevisado() {
        return btnRevisado;
    }
}
