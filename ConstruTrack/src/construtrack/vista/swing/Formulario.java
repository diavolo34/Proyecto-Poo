package construtrack.vista.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

final class Formulario {
    private final JPanel panel = new JPanel(new GridBagLayout());
    private int fila;

    Formulario() {
        panel.setOpaque(false);
    }

    Formulario campo(String etiqueta, JComponent campo, String ayuda) {
        return campo(etiqueta, campo, ayuda, 0, 2);
    }

    Formulario campo(String etiqueta, JComponent campo, String ayuda, int columna, int ancho) {
        JPanel bloque = new JPanel(new BorderLayout(0, 3));
        bloque.setOpaque(false);
        JLabel lbl = Tema.texto(etiqueta);
        lbl.setFont(Tema.NEGRITA);
        lbl.setLabelFor(campo);
        if (campo instanceof JTextField texto) {
            texto.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x8590A2)), Tema.margen(6, 8, 6, 8)));
            texto.getAccessibleContext().setAccessibleName(etiqueta);
        }
        bloque.add(lbl, BorderLayout.NORTH);
        bloque.add(campo, BorderLayout.CENTER);
        if (ayuda != null) {
            bloque.add(Tema.textoSuave(ayuda), BorderLayout.SOUTH);
        }
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = columna;
        c.gridy = fila;
        c.gridwidth = ancho;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, columna == 1 ? 8 : 0, 0, columna == 0 && ancho == 1 ? 8 : 0);
        panel.add(bloque, c);
        if (columna + ancho >= 2) {
            fila++;
        }
        return this;
    }

    JPanel panel() {
        return panel;
    }
}
