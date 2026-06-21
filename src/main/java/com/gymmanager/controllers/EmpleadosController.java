package com.gymmanager.controllers;

import com.gymmanager.models.Usuario;
import com.gymmanager.services.EmpleadoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

/**
 * Controlador del módulo de gestión de empleados.
 * Solo accesible para ADMIN; lista, crea, edita y activa/desactiva recepcionistas.
 */
public class EmpleadosController {

    // ─── Tabla ────────────────────────────────────────────────────────────────
    @FXML private TableView<Usuario>         tablaEmpleados;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colEstado;

    // ─── Controles ────────────────────────────────────────────────────────────
    @FXML private Button btnEditar;
    @FXML private Button btnToggleEstado;
    @FXML private Label  lblConteo;

    private final EmpleadoService empleadoService = EmpleadoService.getInstance();
    private final ObservableList<Usuario> empleados = FXCollections.observableArrayList();

    /** Usuario en sesión; se recibe del DashboardController para la bitácora. */
    private Usuario usuarioSesion;

    // ─── Inicialización ───────────────────────────────────────────────────────

    /**
     * Punto de entrada desde DashboardController.
     * Debe llamarse después de cargar el FXML.
     */
    public void inicializar(Usuario usuario) {
        this.usuarioSesion = usuario;
        configurarTabla();
        configurarBotonesPorSeleccion();
        cargarEmpleados();
    }

    /** Enlaza columnas de la tabla con propiedades del modelo. */
    private void configurarTabla() {
        colUsuario.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsuario()));

        colRol.setCellValueFactory(c ->
                new SimpleStringProperty("Recepcionista"));

        colEstado.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));

        // Color según estado activo/inactivo
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("Activo".equals(item)
                            ? "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
                            : "-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                }
            }
        });

        tablaEmpleados.setItems(empleados);
        tablaEmpleados.setPlaceholder(new Label("No hay empleados registrados."));
    }

    /** Habilita/deshabilita botones de acción según la selección activa. */
    private void configurarBotonesPorSeleccion() {
        btnEditar.setDisable(true);
        btnToggleEstado.setDisable(true);

        tablaEmpleados.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, seleccionado) -> {
                    boolean haySeleccion = seleccionado != null;
                    btnEditar.setDisable(!haySeleccion);
                    btnToggleEstado.setDisable(!haySeleccion);

                    if (haySeleccion) {
                        // Texto dinámico según estado actual
                        btnToggleEstado.setText(seleccionado.isActivo()
                                ? "⊘ Desactivar" : "✓ Activar");
                    }
                });
    }

    // ─── Carga de datos ───────────────────────────────────────────────────────

    private void cargarEmpleados() {
        empleados.setAll(empleadoService.listarEmpleados());

        int total    = empleados.size();
        long activos = empleados.stream().filter(Usuario::isActivo).count();
        lblConteo.setText(total + " empleado(s)  ·  " + activos + " activo(s)");
    }

    // ─── Acciones del usuario ─────────────────────────────────────────────────

    @FXML
    private void nuevoEmpleado() {
        abrirModal(null);
    }

    @FXML
    private void editarEmpleado() {
        Usuario seleccionado = tablaEmpleados.getSelectionModel().getSelectedItem();
        if (seleccionado != null) abrirModal(seleccionado);
    }

    /** Alterna el estado activo/inactivo del empleado seleccionado. */
    @FXML
    private void toggleEstado() {
        Usuario seleccionado = tablaEmpleados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        boolean nuevoEstado = !seleccionado.isActivo();
        String accion = nuevoEstado ? "activar" : "desactivar";

        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
        confirmar.setTitle("Confirmar acción");
        confirmar.setHeaderText(null);
        confirmar.setContentText(
                "¿Deseas " + accion + " al empleado «" + seleccionado.getUsuario() + "»?");
        aplicarEstiloDialog(confirmar);

        Optional<ButtonType> respuesta = confirmar.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            try {
                empleadoService.cambiarEstado(
                        seleccionado.getId(), nuevoEstado, usuarioSesion.getUsuario());
                cargarEmpleados();
            } catch (Exception e) {
                mostrarError("No se pudo cambiar el estado: " + e.getMessage());
            }
        }
    }

    // ─── Modal ────────────────────────────────────────────────────────────────

    /**
     * Abre el modal de alta o edición.
     * @param empleado null → modo creación · Usuario → modo edición.
     */
    private void abrirModal(Usuario empleado) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/agregarEditarEmpleado.fxml"));
            Parent root = loader.load();

            AgregarEditarEmpleadoController ctrl = loader.getController();
            ctrl.inicializar(empleado, usuarioSesion.getUsuario());

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(empleado == null ? "Nuevo Empleado" : "Editar Empleado");
            modal.setResizable(false);

            Scene escena = new Scene(root);
            escena.getStylesheets().add(
                    getClass().getResource("/com/gymmanager/css/estilos.css").toExternalForm());
            modal.setScene(escena);
            modal.showAndWait();

            cargarEmpleados(); // refrescar tabla al cerrar el modal

        } catch (IOException e) {
            mostrarError("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        aplicarEstiloDialog(alert);
        alert.showAndWait();
    }

    private void aplicarEstiloDialog(Alert alert) {
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/gymmanager/css/estilos.css").toExternalForm());
    }
}