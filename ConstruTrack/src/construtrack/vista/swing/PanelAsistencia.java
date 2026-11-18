package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public final class PanelAsistencia extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JLabel lblSaludo = Tema.titulo(" ");
    private final JLabel lblEstado = Tema.chip(" ", Tema.GRIS_SUAVE, Tema.TEXTO);
    private final JLabel lblHorario = Tema.textoSuave(" ");
    private final JLabel lblMensaje = Tema.texto(" ");
    private final JButton btnEntrada = Tema.botonPrimario("Marcar entrada");
    private final JButton btnSalida = Tema.botonSecundario("Marcar salida");
    private final JLabel lblJornadas = Tema.texto("0");
    private final JLabel lblRetardos = Tema.texto("0");
    private final JLabel lblPuntualidad = Tema.texto("0");
    private final DefaultTableModel modelo =
            Tema.modeloSoloLectura("Fecha", "Entrada", "Salida", "Llegada", "Horas", "Control");

    public PanelAsistencia() {
        super(new BorderLayout(12, 12));
        setBorder(Tema.margen(12, 4, 4, 4));

        JPanel arriba = new JPanel(new GridLayout(1, 2, 12, 0));
        arriba.setOpaque(false);
        arriba.add(tarjetaEstado());
        arriba.add(tarjetaResumen());
        add(arriba, BorderLayout.NORTH);

        JPanel historial = Tema.tarjeta(new BorderLayout(0, 10));
        historial.add(Tema.subtitulo("Historial reciente"), BorderLayout.NORTH);
        JTable tabla = Tema.tabla(modelo);
        tabla.getAccessibleContext().setAccessibleName("Historial de asistencia");
        historial.add(Tema.desplazable(tabla), BorderLayout.CENTER);
        add(historial, BorderLayout.CENTER);
    }

    private JPanel tarjetaEstado() {
        JPanel tarjeta = Tema.tarjeta(new BorderLayout(0, 8));
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
        fila.setOpaque(false);
        fila.add(lblEstado);
        for (javax.swing.JComponent c : new javax.swing.JComponent[] {lblSaludo, fila, lblHorario}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            textos.add(c);
        }
        tarjeta.add(textos, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botones.setOpaque(false);
        btnEntrada.setMnemonic('E');
        btnSalida.setMnemonic('S');
        botones.add(btnEntrada);
        botones.add(javax.swing.Box.createHorizontalStrut(8));
        botones.add(btnSalida);
        JPanel abajo = new JPanel(new BorderLayout(0, 6));
        abajo.setOpaque(false);
        abajo.add(botones, BorderLayout.NORTH);
        abajo.add(lblMensaje, BorderLayout.SOUTH);
        tarjeta.add(abajo, BorderLayout.SOUTH);
        return tarjeta;
    }

    private JPanel tarjetaResumen() {
        JPanel tarjeta = Tema.tarjeta(new BorderLayout(0, 10));
        tarjeta.add(Tema.subtitulo("Esta semana"), BorderLayout.NORTH);
        JPanel cifras = new JPanel(new GridLayout(1, 3, 10, 0));
        cifras.setOpaque(false);
        cifras.add(cifra(lblJornadas, "Jornadas"));
        cifras.add(cifra(lblRetardos, "Llegadas tarde"));
        cifras.add(cifra(lblPuntualidad, "Puntualidad"));
        tarjeta.add(cifras, BorderLayout.CENTER);
        return tarjeta;
    }

    static JPanel cifra(JLabel valor, String descripcion) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Tema.GRIS_SUAVE);
        panel.setBorder(Tema.margen(10, 12, 10, 12));
        valor.setFont(Tema.CIFRA);
        panel.add(valor, BorderLayout.CENTER);
        panel.add(Tema.textoSuave(descripcion), BorderLayout.SOUTH);
        return panel;
    }

    public void mostrarEstado(boolean presente) {
        lblEstado.setText(presente ? "● En la obra" : "○ Fuera de la obra");
        lblEstado.setBackground(presente ? Tema.VERDE_SUAVE : Tema.GRIS_SUAVE);
        lblEstado.setForeground(presente ? Tema.VERDE : Tema.TEXTO_SUAVE);
        btnEntrada.setEnabled(!presente);
        btnSalida.setEnabled(presente);
    }

    public void mostrarMensaje(String texto, boolean esError) {
        lblMensaje.setText((esError ? "Error: " : "Listo: ") + texto);
        lblMensaje.setForeground(esError ? Tema.ROJO : Tema.VERDE);
    }

    public JLabel getLblSaludo() {
        return lblSaludo;
    }

    public JLabel getLblHorario() {
        return lblHorario;
    }

    public JButton getBtnEntrada() {
        return btnEntrada;
    }

    public JButton getBtnSalida() {
        return btnSalida;
    }

    public JLabel getLblJornadas() {
        return lblJornadas;
    }

    public JLabel getLblRetardos() {
        return lblRetardos;
    }

    public JLabel getLblPuntualidad() {
        return lblPuntualidad;
    }

    public DefaultTableModel getModelo() {
        return modelo;
    }
}
