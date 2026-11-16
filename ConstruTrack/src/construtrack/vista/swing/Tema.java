package construtrack.vista.swing;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public final class Tema {
    public static final Color AZUL = new Color(0x0C66E4);
    public static final Color AZUL_OSCURO = new Color(0x0055CC);
    public static final Color AZUL_SUAVE = new Color(0xE9F2FF);
    public static final Color FONDO = new Color(0xF7F8F9);
    public static final Color SUPERFICIE = Color.WHITE;
    public static final Color BORDE = new Color(0xDCDFE4);
    public static final Color TEXTO = new Color(0x172B4D);
    public static final Color TEXTO_SUAVE = new Color(0x44546F);
    public static final Color VERDE = new Color(0x216E4E);
    public static final Color VERDE_SUAVE = new Color(0xDCFFF1);
    public static final Color AMARILLO = new Color(0x7F5F01);
    public static final Color AMARILLO_SUAVE = new Color(0xFFF7D6);
    public static final Color ROJO = new Color(0xAE2E24);
    public static final Color ROJO_SUAVE = new Color(0xFFECEB);
    public static final Color MORADO = new Color(0x5E4DB2);
    public static final Color MORADO_SUAVE = new Color(0xF3F0FF);
    public static final Color GRIS_SUAVE = new Color(0xF1F2F4);

    public static final Font FUENTE = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font NEGRITA = FUENTE.deriveFont(Font.BOLD);
    public static final Font PEQUENA = FUENTE.deriveFont(12f);
    public static final Font SUBTITULO = FUENTE.deriveFont(Font.BOLD, 15f);
    public static final Font TITULO = FUENTE.deriveFont(Font.BOLD, 22f);
    public static final Font CIFRA = FUENTE.deriveFont(Font.BOLD, 28f);

    private Tema() {
    }

    public static void instalar() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
        }
        FontUIResource fuente = new FontUIResource(FUENTE);
        for (String clave : new String[] {"Label", "Button", "TextField", "PasswordField", "TextArea", "ComboBox",
            "CheckBox", "Table", "TableHeader", "TabbedPane", "List", "Spinner", "OptionPane.message",
            "OptionPane.button", "ToolTip", "MenuItem", "FileChooser"}) {
            UIManager.put(clave + ".font", fuente);
        }
        UIManager.put("TextField.margin", new java.awt.Insets(5, 8, 5, 8));
        UIManager.put("PasswordField.margin", new java.awt.Insets(5, 8, 5, 8));
        UIManager.put("TextArea.margin", new java.awt.Insets(5, 8, 5, 8));
        UIManager.put("Button.disabledText", TEXTO_SUAVE);
        UIManager.put("Panel.background", FONDO);
        UIManager.put("OptionPane.background", SUPERFICIE);
        UIManager.put("TabbedPane.selected", SUPERFICIE);
        UIManager.put("TabbedPane.contentAreaColor", FONDO);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("ToolTip.background", TEXTO);
        UIManager.put("ToolTip.foreground", Color.WHITE);
    }

    public static JButton botonPrimario(String texto) {
        return boton(texto, AZUL, AZUL_OSCURO, Color.WHITE);
    }

    public static JButton botonSecundario(String texto) {
        return boton(texto, GRIS_SUAVE, BORDE, TEXTO);
    }

    public static JButton botonPeligro(String texto) {
        return boton(texto, ROJO_SUAVE, new Color(0xFFD5D2), ROJO);
    }

    public static JButton botonExito(String texto) {
        return boton(texto, VERDE_SUAVE, new Color(0xBAF3DB), VERDE);
    }

    private static JButton boton(String texto, Color fondo, Color fondoHover, Color colorTexto) {
        JButton boton = new JButton(texto);
        boton.setFont(NEGRITA);
        boton.setForeground(colorTexto);
        boton.setBackground(fondo);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setFocusPainted(true);
        boton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        boton.addPropertyChangeListener("enabled", e -> {
            boolean activo = boton.isEnabled();
            boton.setBackground(activo ? fondo : GRIS_SUAVE);
            boton.setCursor(Cursor.getPredefinedCursor(activo ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        });

        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(fondoHover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(fondo);
            }
        });
        return boton;
    }

    public static JLabel titulo(String texto) {
        return etiqueta(texto, TITULO, TEXTO);
    }

    public static JLabel subtitulo(String texto) {
        return etiqueta(texto, SUBTITULO, TEXTO);
    }

    public static JLabel texto(String texto) {
        return etiqueta(texto, FUENTE, TEXTO);
    }

    public static JLabel textoSuave(String texto) {
        return etiqueta(texto, PEQUENA, TEXTO_SUAVE);
    }

    private static JLabel etiqueta(String texto, Font fuente, Color color) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente);
        etiqueta.setForeground(color);
        return etiqueta;
    }

    public static JLabel chip(String texto, Color fondo, Color colorTexto) {
        JLabel chip = new JLabel(texto);
        chip.setFont(PEQUENA.deriveFont(Font.BOLD));
        chip.setForeground(colorTexto);
        chip.setBackground(fondo);
        chip.setOpaque(true);
        chip.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return chip;
    }

    public static JPanel tarjeta(LayoutManager layout) {
        JPanel tarjeta = new JPanel(layout);
        tarjeta.setBackground(SUPERFICIE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE), BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        return tarjeta;
    }

    public static Border margen(int arriba, int izquierda, int abajo, int derecha) {
        return BorderFactory.createEmptyBorder(arriba, izquierda, abajo, derecha);
    }

    public static JTable tabla(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(30);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(BORDE);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setSelectionBackground(AZUL_SUAVE);
        tabla.setSelectionForeground(TEXTO);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setFont(NEGRITA);
        tabla.getTableHeader().setBackground(GRIS_SUAVE);
        tabla.getTableHeader().setForeground(TEXTO_SUAVE);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 32));
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                    boolean foco, int fila, int columna) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                c.setBorder(margen(0, 8, 0, 8));
                return c;
            }
        });
        return tabla;
    }

    public static void anchos(JTable tabla, int... anchos) {
        for (int i = 0; i < anchos.length && i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    public static DefaultTableModel modeloSoloLectura(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    public static JScrollPane desplazable(java.awt.Component componente) {
        JScrollPane scroll = new JScrollPane(componente);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));
        scroll.getViewport().setBackground(SUPERFICIE);
        return scroll;
    }
}
