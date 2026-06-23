package com.gymmanager.controllers;

import com.gymmanager.dao.MembresiaDAOImpl;
import com.gymmanager.models.Membresia;
import com.gymmanager.models.Usuario;
import com.gymmanager.services.BitacoraService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Controlador del formulario para dar de alta o editar una membresía.
 * Se abre como ventana modal desde MembresiaController.
 */
public class AgregarEditarMembresiaController {

    @FXML private Label    lblTitulo;
    @FXML private TextField tfNombre;
    @FXML private TextField tfPrecio;
    @FXML private TextField tfDuracion;
    @FXML private CheckBox  chkDescuento;
    @FXML private TextField tfDescuento;
    @FXML private TextArea  taDescripcion;
    @FXML private Label     lblError;
    @FXML private Button    btnGuardar;
    @FXML private Button    btnCancelar;

    private Membresia      membresiaEditar; // null = modo NUEVA
    private Usuario        usuarioActual;
    private final MembresiaDAOImpl dao = new MembresiaDAOImpl();

    // ── Punto de entrada ────────────────────────────────────────────────────

    /**
     * Llamado por MembresiaController antes de mostrar la ventana.
     * @param membresia null → modo NUEVA; objeto con datos → modo EDITAR.
     */
    public void inicializar(Membresia membresia, Usuario usuario) {
        this.membresiaEditar = membresia;
        this.usuarioActual   = usuario;

        // El campo de monto solo se activa cuando el checkbox está marcado
        tfDescuento.disableProperty().bind(chkDescuento.selectedProperty().not());

        if (membresia == null) {
            lblTitulo.setText("Nueva Membresía");
        } else {
            // Modo EDITAR: rellena los campos con los datos actuales
            lblTitulo.setText("Editar Membresía");
            tfNombre.setText(membresia.getNombre());
            tfPrecio.setText(String.valueOf(membresia.getPrecio()));
            tfDuracion.setText(String.valueOf(membresia.getDuracionDias()));
            taDescripcion.setText(
                    membresia.getDescripcion() != null ? membresia.getDescripcion() : ""
            );
            if (membresia.getDescuentoEstudiante() > 0) {
                // El binding se actualiza automáticamente al marcar el checkbox
                chkDescuento.setSelected(true);
                tfDescuento.setText(String.valueOf(membresia.getDescuentoEstudiante()));
            }
        }
    }

    // ── Manejadores ─────────────────────────────────────────────────────────

    @FXML
    private void onGuardar() {
        if (!validar()) return;

        try {
            Membresia m = construirModelo();
            if (membresiaEditar == null) {
                dao.insertar(m);
                BitacoraService.getInstance().registrar(
                        usuarioActual.getUsuario(),
                        "ALTA_MEMBRESIA",
                        "Membresía creada: " + m.getNombre()
                );
            } else {
                m.setId(membresiaEditar.getId());
                dao.actualizar(m);
                BitacoraService.getInstance().registrar(
                        usuarioActual.getUsuario(),
                        "EDICION_MEMBRESIA",
                        "Membresía editada: " + m.getNombre()
                );
            }
            cerrar();
        } catch (SQLException e) {
            lblError.setText("Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        cerrar();
    }

    // ── Validación y construcción del modelo ─────────────────────────────────

    /** Valida campos; muestra error en lblError y devuelve false si hay problema. */
    private boolean validar() {
        lblError.setText("");

        if (tfNombre.getText().isBlank()) {
            lblError.setText("El nombre no puede estar vacío.");
            return false;
        }

        try {
            double precio = Double.parseDouble(tfPrecio.getText().trim());
            if (precio <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblError.setText("El precio debe ser un número mayor a 0.");
            return false;
        }

        try {
            int duracion = Integer.parseInt(tfDuracion.getText().trim());
            if (duracion <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblError.setText("La duración debe ser un entero mayor a 0.");
            return false;
        }

        if (chkDescuento.isSelected()) {
            try {
                double desc = Double.parseDouble(tfDescuento.getText().trim());
                if (desc <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                lblError.setText("El monto del descuento debe ser mayor a 0.");
                return false;
            }
        }

        return true;
    }

    /** Construye el objeto Membresia a partir de los campos del formulario. */
    private Membresia construirModelo() {
        Membresia m = new Membresia();
        m.setNombre(tfNombre.getText().trim());
        m.setPrecio(Double.parseDouble(tfPrecio.getText().trim()));
        m.setDuracionDias(Integer.parseInt(tfDuracion.getText().trim()));
        m.setDescuentoEstudiante(
                chkDescuento.isSelected()
                        ? Double.parseDouble(tfDescuento.getText().trim())
                        : 0.0
        );
        m.setDescripcion(taDescripcion.getText().trim());
        return m;
    }

    private void cerrar() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}