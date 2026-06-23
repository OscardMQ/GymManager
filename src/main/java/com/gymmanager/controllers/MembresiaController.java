package com.gymmanager.controllers;

import com.gymmanager.dao.MembresiaDAOImpl;
import com.gymmanager.models.Membresia;
import com.gymmanager.models.Usuario;
import com.gymmanager.services.BitacoraService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Controlador del módulo Membresías.
 * Solo accesible para ADMIN. Permite listar, crear, editar y eliminar membresías.
 */
public class MembresiaController {

    @FXML private TableView<Membresia>            tablaMembresias;
    @FXML private TableColumn<Membresia, String>  colNombre;
    @FXML private TableColumn<Membresia, Double>  colPrecio;
    @FXML private TableColumn<Membresia, Integer> colDuracion;
    @FXML private TableColumn<Membresia, Double>  colDescuento;
    @FXML private TableColumn<Membresia, String>  colDescripcion;

    @FXML private Button btnNueva;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    private Usuario          usuarioActual;
    private final MembresiaDAOImpl dao = new MembresiaDAOImpl();

    // ── Punto de entrada ────────────────────────────────────────────────────

    /** Llamado por DashboardController tras cargar el FXML. */
    public void inicializar(Usuario usuario) {
        this.usuarioActual = usuario;
        configurarColumnas();
        configurarSeleccion();
        cargarMembresias();
    }

    // ── Configuración inicial ────────────────────────────────────────────────

    /** Enlaza columnas con propiedades del modelo y aplica formato de celdas. */
    private void configurarColumnas() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colDuracion.setCellValueFactory(new PropertyValueFactory<>("duracionDias"));
        colDescuento.setCellValueFactory(new PropertyValueFactory<>("descuentoEstudiante"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        // Formato moneda en columna Precio
        colPrecio.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });

        // Descuento: muestra "Sin descuento" si el valor es 0
        colDescuento.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item == 0.0 ? "Sin descuento" : String.format("$%.2f", item));
                }
            }
        });
    }

    /** Botones Editar y Eliminar solo activos cuando hay fila seleccionada. */
    private void configurarSeleccion() {
        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
        tablaMembresias.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, nueva) -> {
                    boolean haySeleccion = nueva != null;
                    btnEditar.setDisable(!haySeleccion);
                    btnEliminar.setDisable(!haySeleccion);
                });
    }

    // ── Carga de datos ───────────────────────────────────────────────────────

    private void cargarMembresias() {
        try {
            List<Membresia> lista = dao.listar();
            tablaMembresias.setItems(FXCollections.observableArrayList(lista));
        } catch (SQLException e) {
            mostrarError("Error al cargar membresías: " + e.getMessage());
        }
    }

    // ── Manejadores de botones ───────────────────────────────────────────────

    @FXML
    private void onNueva() {
        abrirFormulario(null);
    }

    @FXML
    private void onEditar() {
        Membresia seleccionada = tablaMembresias.getSelectionModel().getSelectedItem();
        if (seleccionada != null) abrirFormulario(seleccionada);
    }

    @FXML
    private void onEliminar() {
        Membresia seleccionada = tablaMembresias.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;

        // Diálogo de confirmación; initOwner hereda el tema CSS de la ventana padre
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.initOwner(tablaMembresias.getScene().getWindow());
        confirmacion.setTitle("Eliminar Membresía");
        confirmacion.setHeaderText("¿Eliminar \"" + seleccionada.getNombre() + "\"?");
        confirmacion.setContentText("Esta acción no se puede deshacer.");
        confirmacion.getDialogPane().getStyleClass().add("dialog-dark");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                dao.eliminar(seleccionada.getId());
                BitacoraService.getInstance().registrar(
                        usuarioActual.getUsuario(),
                        "BAJA_MEMBRESIA",
                        "Membresía eliminada: " + seleccionada.getNombre()
                );
                cargarMembresias();
            } catch (SQLException e) {
                mostrarError("Error al eliminar: " + e.getMessage());
            }
        }
    }

    // ── Formulario modal ─────────────────────────────────────────────────────

    /**
     * Abre el formulario de alta/edición como ventana modal.
     * @param membresia null → modo NUEVA; objeto → modo EDITAR.
     */
    private void abrirFormulario(Membresia membresia) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/agregarEditarMembresia.fxml")
            );
            Scene escena = new Scene(loader.load());

            // Inicializar controller ANTES de mostrar la ventana
            AgregarEditarMembresiaController ctrl = loader.getController();
            ctrl.inicializar(membresia, usuarioActual);

            Stage stage = new Stage();
            stage.setScene(escena);
            stage.setTitle(membresia == null ? "Nueva Membresía" : "Editar Membresía");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tablaMembresias.getScene().getWindow()); // hereda CSS del padre
            stage.setResizable(false);
            stage.showAndWait();

            cargarMembresias(); // refresca tras guardar o cancelar
        } catch (IOException e) {
            mostrarError("Error al abrir formulario: " + e.getMessage());
        }
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        alert.initOwner(tablaMembresias.getScene().getWindow());
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}