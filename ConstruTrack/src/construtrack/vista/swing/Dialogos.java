package construtrack.vista.swing;

import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class Dialogos {
    private Dialogos() {
    }

    public static boolean formulario(Component padre, String titulo, JPanel contenido) {
        return JOptionPane.showConfirmDialog(padre, contenido, titulo, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    public static String pedirTexto(Component padre, String titulo, String mensaje) {
        return JOptionPane.showInputDialog(padre, mensaje, titulo, JOptionPane.QUESTION_MESSAGE);
    }

    public static boolean confirmar(Component padre, String titulo, String mensaje) {
        return JOptionPane.showConfirmDialog(padre, "<html><body style='width:340px'>" + mensaje + "</body></html>", titulo,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static Path elegirDocumento(Component padre, String titulo) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle(titulo);
        selector.setFileFilter(new FileNameExtensionFilter("Fotos o PDF (JPG, PNG, WEBP, PDF)", "jpg", "jpeg", "png", "webp", "pdf"));
        return selector.showOpenDialog(padre) == JFileChooser.APPROVE_OPTION ? selector.getSelectedFile().toPath() : null;
    }

    public static void abrir(Component padre, String nombre, byte[] datos) {
        try {
            Path carpeta = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "construtrack"));
            Path archivo = carpeta.resolve(nombre.replaceAll("[\\\\/:*?\"<>|]", "_"));
            Files.write(archivo, datos);
            archivo.toFile().deleteOnExit();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(archivo.toFile());
            } else {
                JOptionPane.showMessageDialog(padre, "El documento se guardó en:\n" + archivo, "Documento",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(padre, "No se pudo abrir el documento: " + e.getMessage(), "Documento",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void guardarTexto(Component padre, String nombre, String contenido) {
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new java.io.File(nombre));
        if (selector.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Files.writeString(selector.getSelectedFile().toPath(), contenido, StandardCharsets.UTF_8);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(padre, "No se pudo guardar: " + e.getMessage(), "Guardar", JOptionPane.ERROR_MESSAGE);
        }
    }
}
