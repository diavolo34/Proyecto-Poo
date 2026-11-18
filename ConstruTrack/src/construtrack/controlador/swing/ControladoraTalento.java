package construtrack.controlador.swing;

import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.modelo.inventario.Asignacion;
import construtrack.modelo.talento.Egreso;
import construtrack.servicio.ServicioArchivos;
import construtrack.servicio.ServicioTalento;
import construtrack.vista.swing.Dialogos;
import construtrack.vista.swing.PanelTalento;
import construtrack.vista.swing.Tema;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public final class ControladoraTalento {
    private final PanelTalento vista;
    private final ServicioTalento talento;
    private final ServicioArchivos archivos;
    private final Usuario usuario;
    private final Runnable alCambiar;
    private List<Egreso> egresos = List.of();

    public ControladoraTalento(PanelTalento vista, ServicioTalento talento, ServicioArchivos archivos, Usuario usuario,
            Runnable alCambiar) {
        this.vista = vista;
        this.talento = talento;
        this.archivos = archivos;
        this.usuario = usuario;
        this.alCambiar = alCambiar;
        vista.getBtnEgreso().addActionListener(e -> registrarEgreso());
        vista.getBtnVoucher().addActionListener(e -> verDocumento(true));
        vista.getBtnCarta().addActionListener(e -> verDocumento(false));
        vista.getBtnExpediente().addActionListener(e -> guardarExpediente());
        vista.getBtnEliminar().addActionListener(e -> eliminarFicha());
    }

    private void registrarEgreso() {
        Object cedula = vista.getActivos().valorSeleccionado(0);
        if (cedula == null) {
            vista.getActivos().mostrarMensaje("selecciona a una persona en la tabla.", true);
            return;
        }
        List<Asignacion> pendientes = talento.pendientes(usuario, cedula.toString()).entregas();
        JComboBox<String> tipo = new JComboBox<>(new String[] {"Despido", "Renuncia"});
        JTextField fecha = new JTextField(LocalDate.now().toString(), 10);
        JTextArea motivo = new JTextArea(3, 30);
        motivo.setLineWrap(true);
        Path[] documentos = new Path[2];
        JLabel lblVoucher = Tema.textoSuave("Sin elegir");
        JLabel lblCarta = Tema.textoSuave("Sin elegir (solo renuncia)");
        JButton voucher = Tema.botonSecundario("Voucher de pago…");
        JButton carta = Tema.botonSecundario("Carta de renuncia…");
        voucher.addActionListener(e -> elegir(documentos, 0, lblVoucher));
        carta.addActionListener(e -> elegir(documentos, 1, lblCarta));
        JRadioButton si = new JRadioButton("Sí los devolvió", pendientes.isEmpty());
        JRadioButton no = new JRadioButton("No los devolvió");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(si);
        grupo.add(no);
        JTextArea nota = new JTextArea(2, 30);
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(new JLabel("Egreso de " + vista.getActivos().valorSeleccionado(1)));
        panel.add(new JLabel("Tipo:"));
        panel.add(tipo);
        panel.add(new JLabel("Fecha (AAAA-MM-DD):"));
        panel.add(fecha);
        panel.add(new JLabel("Motivo:"));
        panel.add(Tema.desplazable(motivo));
        panel.add(voucher);
        panel.add(lblVoucher);
        panel.add(carta);
        panel.add(lblCarta);
        panel.add(new JLabel("<html><b>Equipos a su cargo:</b> " + (pendientes.isEmpty() ? "ninguno registrado"
                : pendientes.stream().map(a -> a.getArticulo().getNombre() + " × " + a.getCantidad()).collect(Collectors.joining(", ")))
                + "</html>"));
        panel.add(si);
        panel.add(no);
        panel.add(new JLabel("Nota de lo que falta (si no los devolvió):"));
        panel.add(Tema.desplazable(nota));
        if (!Dialogos.formulario(vista, "Registrar egreso", panel)) {
            return;
        }
        try {
            boolean renuncia = tipo.getSelectedIndex() == 1;
            Map<String, String> datos = new HashMap<>();
            datos.put("tipo", renuncia ? "RENUNCIA" : "DESPIDO");
            datos.put("fecha", fecha.getText().trim());
            datos.put("motivo", motivo.getText());
            datos.put("devolvioEquipos", si.isSelected() ? "true" : no.isSelected() ? "false" : "");
            datos.put("notaEquipos", nota.getText());
            datos.put("voucher", subir(documentos[0], cedula.toString()));
            if (renuncia) {
                datos.put("carta", subir(documentos[1], cedula.toString()));
            }
            Egreso egreso = talento.registrarEgreso(usuario, cedula.toString(), datos);
            vista.getActivos().mostrarMensaje("egreso de " + egreso.getNombre() + " registrado. Su acceso quedó desactivado.", false);
            alCambiar.run();
        } catch (IOException e) {
            vista.getActivos().mostrarMensaje("no se pudo leer el documento: " + e.getMessage(), true);
        } catch (ConstruTrackException e) {
            vista.getActivos().mostrarMensaje(e.getMessage(), true);
        }
    }

    private void elegir(Path[] documentos, int indice, JLabel etiqueta) {
        Path elegido = Dialogos.elegirDocumento(vista, "Documento");
        if (elegido != null) {
            documentos[indice] = elegido;
            etiqueta.setText(elegido.getFileName().toString());
        }
    }

    private String subir(Path archivo, String cedula) throws IOException {
        if (archivo == null) {
            return null;
        }
        return archivos.subir(usuario, archivo.getFileName().toString(), Files.readAllBytes(archivo), cedula, true).id();
    }

    private Egreso seleccionado() {
        Object id = vista.getEgresos().valorSeleccionado(0);
        if (id == null) {
            vista.getEgresos().mostrarMensaje("selecciona un egreso en la tabla.", true);
            return null;
        }
        return egresos.stream().filter(e -> e.getId() == (Integer) id).findFirst().orElse(null);
    }

    private void verDocumento(boolean voucher) {
        Egreso e = seleccionado();
        if (e == null) {
            return;
        }
        String id = voucher ? e.getVoucher() : e.getCarta();
        if (id == null) {
            vista.getEgresos().mostrarMensaje("este egreso no tiene carta de renuncia.", true);
            return;
        }
        try {
            ServicioArchivos.Descarga d = archivos.descargar(usuario, id);
            Dialogos.abrir(vista, d.nombre(), d.datos());
        } catch (ConstruTrackException ex) {
            vista.getEgresos().mostrarMensaje(ex.getMessage(), true);
        }
    }

    private void guardarExpediente() {
        Egreso e = seleccionado();
        if (e != null) {
            Dialogos.guardarTexto(vista, "expediente-" + e.getCedula() + ".txt", talento.expediente(usuario, e.getId()));
        }
    }

    private void eliminarFicha() {
        Egreso e = seleccionado();
        if (e == null || !Dialogos.confirmar(vista, "Eliminar ficha", "Se eliminará la ficha de " + e.getNombre()
                + " de la empresa y su cédula quedará libre. Sus datos quedan registrados: el egreso, los documentos y un "
                + "expediente con su asistencia, permisos y reportes se conservan en Talento humano.")) {
            return;
        }
        try {
            talento.eliminarFicha(usuario, e.getId());
            vista.getEgresos().mostrarMensaje("ficha eliminada. El expediente se conserva.", false);
            alCambiar.run();
        } catch (ConstruTrackException ex) {
            vista.getEgresos().mostrarMensaje(ex.getMessage(), true);
        }
    }

    public void refrescar() {
        vista.getActivos().limpiar();
        for (Usuario u : talento.activos(usuario)) {
            vista.getActivos().fila(u.getCedula(), u.getNombre(), u.getRol(), u.getPuesto() == null ? "—" : u.getPuesto(),
                    u.getArea() == null ? "—" : u.getArea(), u.getFechaIngreso() == null ? "—" : u.getFechaIngreso());
        }
        egresos = talento.listar(usuario);
        vista.getEgresos().limpiar();
        for (Egreso e : egresos) {
            vista.getEgresos().fila(e.getId(), e.getNombre(), e.getCedula(), e.getTipo().getNombre(), e.getFecha(), e.getMotivo(),
                    (e.devolvioEquipos() ? "Sí los devolvió" : "No los devolvió") + (e.getNotaEquipos().isEmpty() ? "" : " · " + e.getNotaEquipos()),
                    e.fichaEliminada() ? "Eliminada" : "Inactiva");
        }
    }
}
