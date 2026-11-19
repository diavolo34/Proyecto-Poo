package construtrack.controlador.swing;

import construtrack.modelo.Obra;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.modelo.inventario.Articulo;
import construtrack.modelo.inventario.Asignacion;
import construtrack.servicio.ServicioInventario;
import construtrack.servicio.ServicioPersonal;
import construtrack.vista.swing.Dialogos;
import construtrack.vista.swing.Opcion;
import construtrack.vista.swing.PanelInventario;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

public final class ControladoraInventario {
    private final PanelInventario vista;
    private final ServicioInventario inventario;
    private final ServicioPersonal personal;
    private final Usuario usuario;
    private final Runnable alCambiar;
    private List<Articulo> articulos = List.of();

    public ControladoraInventario(PanelInventario vista, ServicioInventario inventario, ServicioPersonal personal, Usuario usuario,
            Runnable alCambiar) {
        this.vista = vista;
        this.inventario = inventario;
        this.personal = personal;
        this.usuario = usuario;
        this.alCambiar = alCambiar;
        vista.getBtnAgregar().addActionListener(e -> formulario(null));
        vista.getBtnEditar().addActionListener(e -> {
            Articulo a = seleccionado();
            if (a != null) {
                formulario(a);
            }
        });
        vista.getBtnEntregar().addActionListener(e -> entregar());
        vista.getBtnHistorial().addActionListener(e -> historial());
        vista.getBtnDevolucion().addActionListener(e -> devolver());
    }

    private Articulo seleccionado() {
        Object codigo = vista.getArticulos().valorSeleccionado(0);
        if (codigo == null) {
            vista.getArticulos().mostrarMensaje("selecciona un artículo en la tabla.", true);
            return null;
        }
        return articulos.stream().filter(a -> a.getCodigo().equals(codigo)).findFirst().orElse(null);
    }

    private void formulario(Articulo a) {
        JComboBox<Opcion> categoria = new JComboBox<>(new Opcion[] {new Opcion("HERRAMIENTA", "Herramienta"),
            new Opcion("EQUIPO", "Equipo"), new Opcion("MATERIAL", "Material")});
        JTextField nombre = new JTextField(a == null ? "" : a.getNombre(), 24);
        JTextField marca = new JTextField(a == null ? "" : a.getMarca(), 20);
        JTextField serie = new JTextField(a == null ? "" : a.getSerie(), 14);
        JSpinner cantidad = new JSpinner(new SpinnerNumberModel(a == null ? 1 : a.getCantidad(), 1, Articulo.CANTIDAD_MAXIMA, 1));
        JComboBox<Opcion> estado = new JComboBox<>(new Opcion[] {new Opcion("OPERATIVO", "Operativo"),
            new Opcion("EN_MANTENIMIENTO", "En mantenimiento"), new Opcion("DE_BAJA", "De baja")});
        JTextField nota = new JTextField(a == null ? "" : a.getNota(), 24);
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        if (a == null) {
            panel.add(new JLabel("Categoría:"));
            panel.add(categoria);
        } else {
            estado.setSelectedIndex(a.getEstado().ordinal());
        }
        panel.add(new JLabel("Nombre:"));
        panel.add(nombre);
        panel.add(new JLabel("Marca y modelo:"));
        panel.add(marca);
        panel.add(new JLabel("N.º de serie (con serie, la cantidad es 1):"));
        panel.add(serie);
        panel.add(new JLabel("Cantidad:"));
        panel.add(cantidad);
        if (a != null) {
            panel.add(new JLabel("Estado:"));
            panel.add(estado);
        }
        panel.add(new JLabel("Nota:"));
        panel.add(nota);
        if (!Dialogos.formulario(vista, a == null ? "Agregar al inventario" : "Editar " + a.getCodigo(), panel)) {
            return;
        }
        Map<String, String> datos = new HashMap<>();
        datos.put("categoria", ((Opcion) categoria.getSelectedItem()).valor());
        datos.put("nombre", nombre.getText());
        datos.put("marca", marca.getText());
        datos.put("serie", serie.getText());
        datos.put("cantidad", String.valueOf(cantidad.getValue()));
        datos.put("nota", nota.getText());
        if (a != null) {
            datos.put("estado", ((Opcion) estado.getSelectedItem()).valor());
        }
        try {
            Articulo guardado = a == null ? inventario.crear(usuario, datos) : inventario.editar(usuario, a.getCodigo(), datos);
            vista.getArticulos().mostrarMensaje(guardado.getCodigo() + " · " + guardado.getNombre() + " guardado.", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.getArticulos().mostrarMensaje(e.getMessage(), true);
        }
    }

    private void entregar() {
        Articulo a = seleccionado();
        if (a == null) {
            return;
        }
        JComboBox<Opcion> persona = new JComboBox<>();
        for (Usuario u : personal.listarVisibles(usuario)) {
            persona.addItem(new Opcion(u.getCedula(), u.getNombre() + " · " + (u.getPuesto() == null ? u.getRol() : u.getPuesto())));
        }
        JComboBox<Opcion> obra = new JComboBox<>();
        obra.addItem(new Opcion("", "— El proyecto de la persona —"));
        for (Obra o : personal.listarObras(usuario)) {
            if (o.admitePersonal()) {
                obra.addItem(new Opcion(o.getCodigo(), o.getNombre()));
            }
        }
        JSpinner cantidad = new JSpinner(new SpinnerNumberModel(1, 1, Math.max(1, a.disponibles()), 1));
        JTextField prevista = new JTextField(10);
        JTextField nota = new JTextField(24);
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(new JLabel(a.getCodigo() + " · " + a.getNombre() + " · disponibles: " + a.disponibles()));
        panel.add(new JLabel("Entregar a:"));
        panel.add(persona);
        panel.add(new JLabel("Cantidad:"));
        panel.add(cantidad);
        panel.add(new JLabel("Proyecto:"));
        panel.add(obra);
        panel.add(new JLabel("Devolver el (AAAA-MM-DD, opcional):"));
        panel.add(prevista);
        panel.add(new JLabel("Nota:"));
        panel.add(nota);
        if (!Dialogos.formulario(vista, "Entregar " + a.getNombre(), panel) || persona.getSelectedItem() == null) {
            return;
        }
        Map<String, String> datos = new HashMap<>();
        datos.put("cedula", ((Opcion) persona.getSelectedItem()).valor());
        datos.put("cantidad", String.valueOf(cantidad.getValue()));
        datos.put("obra", ((Opcion) obra.getSelectedItem()).valor());
        datos.put("devolucionPrevista", prevista.getText().trim());
        datos.put("nota", nota.getText());
        try {
            Asignacion entrega = inventario.asignar(usuario, a.getCodigo(), datos);
            vista.getArticulos().mostrarMensaje(a.getNombre() + " entregado a " + entrega.getPersona().nombre() + ".", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.getArticulos().mostrarMensaje(e.getMessage(), true);
        }
    }

    private void historial() {
        Articulo a = seleccionado();
        if (a == null) {
            return;
        }
        StringBuilder texto = new StringBuilder("<html><body style='width:420px'><b>" + a.getCodigo() + " · " + a.getNombre() + "</b><br><br>");
        if (a.getAsignaciones().isEmpty()) {
            texto.append("Todavía no se ha entregado.");
        }
        for (Asignacion x : a.getAsignaciones()) {
            texto.append(x.getFecha().toLocalDate()).append(" → ").append(x.getPersona().nombre()).append(" × ").append(x.getCantidad())
                    .append(x.estaAbierta() ? " · en uso" : " · devuelto el " + x.getFechaDevolucion().toLocalDate() + " ("
                            + x.getCondicion().getNombre().toLowerCase() + ", recibió " + x.getRecibidoPor().nombre() + ")")
                    .append("<br>");
        }
        javax.swing.JOptionPane.showMessageDialog(vista, texto + "</body></html>", "Historial", javax.swing.JOptionPane.PLAIN_MESSAGE);
    }

    private void devolver() {
        Object id = vista.getEntregas().valorSeleccionado(0);
        if (id == null) {
            vista.getEntregas().mostrarMensaje("selecciona una entrega en la tabla.", true);
            return;
        }
        JComboBox<Opcion> condicion = new JComboBox<>(new Opcion[] {new Opcion("BUENA", "En buen estado"),
            new Opcion("DANADA", "Con daños (pasa a mantenimiento)"), new Opcion("EXTRAVIADA", "No se devolvió: extraviado")});
        JTextField nota = new JTextField(24);
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(new JLabel(vista.getEntregas().valorSeleccionado(1) + " · " + vista.getEntregas().valorSeleccionado(2)));
        panel.add(new JLabel("¿Cómo se devolvió?"));
        panel.add(condicion);
        panel.add(new JLabel("Nota (obligatoria si tiene daños o se extravió):"));
        panel.add(nota);
        if (!Dialogos.formulario(vista, "Registrar devolución", panel)) {
            return;
        }
        try {
            inventario.devolver(usuario, (Integer) id, ((Opcion) condicion.getSelectedItem()).valor(), nota.getText());
            vista.getEntregas().mostrarMensaje("devolución registrada.", false);
            alCambiar.run();
        } catch (ConstruTrackException e) {
            vista.getEntregas().mostrarMensaje(e.getMessage(), true);
        }
    }

    public void refrescar() {
        articulos = inventario.listar(usuario);
        vista.getArticulos().limpiar();
        for (Articulo a : articulos) {
            vista.getArticulos().fila(a.getCodigo(), a.getNombre(), a.getCategoria().getNombre(),
                    (a.getMarca() + (a.getSerie().isEmpty() ? "" : " · S/N " + a.getSerie())).trim(), a.disponibles(), a.getCantidad(),
                    a.getEstado().getNombre());
        }
        LocalDate hoy = inventario.hoy();
        vista.getEntregas().limpiar();
        for (Asignacion x : inventario.asignaciones(usuario)) {
            if (x.estaAbierta()) {
                vista.getEntregas().fila(x.getId(), x.getArticulo().getCodigo() + " · " + x.getArticulo().getNombre(),
                        x.getPersona().nombre(), x.getCantidad(), x.getObra() == null ? "—" : x.getObra(), x.getFecha().toLocalDate(),
                        x.getDevolucionPrevista() == null ? "—" : x.getDevolucionPrevista() + (x.vencidaEn(hoy) ? " (vencida)" : ""));
            }
        }
    }
}
