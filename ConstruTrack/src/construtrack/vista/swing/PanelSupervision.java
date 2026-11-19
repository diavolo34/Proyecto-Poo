package construtrack.vista.swing;

import construtrack.modelo.alertas.Alerta;
import construtrack.modelo.alertas.Nivel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

public final class PanelSupervision extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JComboBox<Opcion> cboPeriodo = new JComboBox<>(new Opcion[] {
        new Opcion("ultimos7", "Últimos 7 días"), new Opcion("semana", "Esta semana"),
        new Opcion("mes", "Este mes")});
    private final JLabel lblAsistencia = Tema.texto("—");
    private final JLabel lblPuntualidad = Tema.texto("—");
    private final JLabel lblHoras = Tema.texto("—");
    private final JLabel lblAlertas = Tema.texto("—");
    private final GraficoBarras grafico = new GraficoBarras();
    private final DefaultListModel<Alerta> modeloAlertas = new DefaultListModel<>();
    private final JList<Alerta> lstAlertas = new JList<>(modeloAlertas);
    private final JComboBox<Opcion> cboReporte = new JComboBox<>(new Opcion[] {
        new Opcion("asistencia", "Asistencia"), new Opcion("puntualidad", "Puntualidad"),
        new Opcion("permisos", "Permisos y reposos"), new Opcion("reportes-obra", "Reportes de proyecto")});
    private final JComboBox<Opcion> cboFormato = new JComboBox<>(new Opcion[] {
        new Opcion("csv", "CSV (Excel)"), new Opcion("txt", "Texto")});
    private final JButton btnExportar = Tema.botonPrimario("Exportar reporte");
    private final JLabel lblMensaje = Tema.texto(" ");

    public PanelSupervision() {
        super(new BorderLayout(0, 12));
        setBorder(Tema.margen(12, 4, 4, 4));

        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(Tema.titulo("Supervisión"), BorderLayout.WEST);
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filtro.setOpaque(false);
        JLabel lblPeriodo = Tema.texto("Período:");
        lblPeriodo.setLabelFor(cboPeriodo);
        filtro.add(lblPeriodo);
        filtro.add(cboPeriodo);
        barra.add(filtro, BorderLayout.EAST);

        JPanel cifras = new JPanel(new GridLayout(1, 4, 12, 0));
        cifras.setOpaque(false);
        cifras.add(tarjetaCifra(lblAsistencia, "Asistencia"));
        cifras.add(tarjetaCifra(lblPuntualidad, "Puntualidad"));
        cifras.add(tarjetaCifra(lblHoras, "Horas trabajadas"));
        cifras.add(tarjetaCifra(lblAlertas, "Alertas activas"));

        JPanel norte = new JPanel(new BorderLayout(0, 12));
        norte.setOpaque(false);
        norte.add(barra, BorderLayout.NORTH);
        norte.add(cifras, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 12, 0));
        centro.setOpaque(false);
        JPanel tarjetaGrafico = Tema.tarjeta(new BorderLayout(0, 10));
        tarjetaGrafico.add(Tema.subtitulo("Asistencia por persona"), BorderLayout.NORTH);
        tarjetaGrafico.add(Tema.desplazable(grafico), BorderLayout.CENTER);
        centro.add(tarjetaGrafico);

        JPanel tarjetaAlertas = Tema.tarjeta(new BorderLayout(0, 10));
        tarjetaAlertas.add(Tema.subtitulo("Alertas automáticas"), BorderLayout.NORTH);
        lstAlertas.setCellRenderer(new RenderAlerta());
        lstAlertas.getAccessibleContext().setAccessibleName("Alertas automáticas");
        tarjetaAlertas.add(Tema.desplazable(lstAlertas), BorderLayout.CENTER);
        centro.add(tarjetaAlertas);
        add(centro, BorderLayout.CENTER);

        JPanel exportar = Tema.tarjeta(new FlowLayout(FlowLayout.LEFT, 10, 0));
        exportar.add(Tema.subtitulo("Reportes"));
        JLabel lblReporte = Tema.texto("Tipo:");
        lblReporte.setLabelFor(cboReporte);
        exportar.add(lblReporte);
        exportar.add(cboReporte);
        JLabel lblFormato = Tema.texto("Formato:");
        lblFormato.setLabelFor(cboFormato);
        exportar.add(lblFormato);
        exportar.add(cboFormato);
        exportar.add(btnExportar);
        exportar.add(lblMensaje);
        add(exportar, BorderLayout.SOUTH);
    }

    private static JPanel tarjetaCifra(JLabel valor, String descripcion) {
        JPanel tarjeta = Tema.tarjeta(new BorderLayout());
        valor.setFont(Tema.CIFRA);
        tarjeta.add(Tema.textoSuave(descripcion), BorderLayout.NORTH);
        tarjeta.add(valor, BorderLayout.CENTER);
        return tarjeta;
    }

    public void mostrarMensaje(String texto, boolean esError) {
        lblMensaje.setText((esError ? "Error: " : "Listo: ") + texto);
        lblMensaje.setForeground(esError ? Tema.ROJO : Tema.VERDE);
    }

    public JComboBox<Opcion> getCboPeriodo() {
        return cboPeriodo;
    }

    public JLabel getLblAsistencia() {
        return lblAsistencia;
    }

    public JLabel getLblPuntualidad() {
        return lblPuntualidad;
    }

    public JLabel getLblHoras() {
        return lblHoras;
    }

    public JLabel getLblAlertas() {
        return lblAlertas;
    }

    public GraficoBarras getGrafico() {
        return grafico;
    }

    public DefaultListModel<Alerta> getModeloAlertas() {
        return modeloAlertas;
    }

    public JComboBox<Opcion> getCboReporte() {
        return cboReporte;
    }

    public JComboBox<Opcion> getCboFormato() {
        return cboFormato;
    }

    public JButton getBtnExportar() {
        return btnExportar;
    }

    private static final class RenderAlerta extends JPanel implements ListCellRenderer<Alerta> {
        private static final long serialVersionUID = 1L;
        private final JLabel nivel = Tema.chip(" ", Tema.GRIS_SUAVE, Tema.TEXTO);
        private final JLabel nombre = Tema.texto(" ");
        private final JLabel mensaje = Tema.textoSuave(" ");

        RenderAlerta() {
            super(new BorderLayout(10, 2));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE), Tema.margen(8, 8, 8, 8)));
            JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            izquierda.setOpaque(false);
            izquierda.add(nivel);
            add(izquierda, BorderLayout.WEST);
            JPanel textos = new JPanel(new GridLayout(2, 1));
            textos.setOpaque(false);
            nombre.setFont(Tema.NEGRITA);
            textos.add(nombre);
            textos.add(mensaje);
            add(textos, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Alerta> lista, Alerta alerta, int indice,
                boolean seleccionada, boolean foco) {
            boolean alta = alerta.getNivel() == Nivel.ALTA;
            nivel.setText(alerta.getNivel().getNombre());
            nivel.setBackground(alta ? Tema.ROJO_SUAVE : Tema.AMARILLO_SUAVE);
            nivel.setForeground(alta ? Tema.ROJO : Tema.AMARILLO);
            nombre.setText(alerta.getNombre());
            mensaje.setText(alerta.getMensaje());
            setBackground(seleccionada ? Tema.AZUL_SUAVE : Tema.SUPERFICIE);
            return this;
        }
    }
}
