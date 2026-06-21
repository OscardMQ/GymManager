package com.gymmanager.controllers;

import com.gymmanager.models.Producto;
import com.gymmanager.models.Usuario;
import com.gymmanager.services.ProductoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
 * Módulo de inventario de productos.
 * ADMIN: puede crear, editar y eliminar.
 * RECEPCIONISTA: puede crear y editar; Eliminar oculto.
 */
public class ProductosController {

    // ─── Tabla ────────────────────────────────────────────────────────────────
    @FXML private TableView<Producto>           tablaProductos;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colPrecio;
    @FXML private TableColumn<Producto, String> colStock;
    @FXML private TableColumn<Producto, String> colStockMin;
    @FXML private TableColumn<Producto, String> colEstado;

    // ─── Controles ────────────────────────────────────────────────────────────
    @FXML private TextField txtBuscar;
    @FXML private Button    btnEditar;
    @FXML private Button    btnEliminar;
    @FXML private Label     lblConteo;
    @FXML private Label     lblAlertaStock;

    private final ProductoService productoService = ProductoService.getInstance();
    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private FilteredList<Producto> productosFiltrados;
    private Usuario usuarioSesion;

    // ─── Inicialización ───────────────────────────────────────────────────────

    public void inicializar(Usuario usuario) {
        this.usuarioSesion = usuario;
        configurarTabla();
        configurarBusqueda();
        configurarBotonesPorSeleccion();
        configurarAccesoPorRol();
        cargarProductos();
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getNombre()));
        colCategoria.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategoria()));
        colPrecio.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getPrecioFormateado()));
        colStock.setCellValueFactory(c     -> new SimpleStringProperty(String.valueOf(c.getValue().getStock())));
        colStockMin.setCellValueFactory(c  -> new SimpleStringProperty(String.valueOf(c.getValue().getStockMinimo())));

        colEstado.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEstadoStockTexto()));

        // Celda de estado con color según nivel de stock
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "Sin stock"  -> "-fx-text-fill: #dc2626; -fx-font-weight: bold;";
                    case "Stock bajo" -> "-fx-text-fill: #f59e0b; -fx-font-weight: bold;";
                    default           -> "-fx-text-fill: #16a34a; -fx-font-weight: bold;";
                });
            }
        });

        // Color de fila según estado — aplica clases CSS definidas en estilos.css
        tablaProductos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Producto item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("fila-stock-bajo", "fila-sin-stock");
                if (empty || item == null) return;
                switch (item.getEstadoStock()) {
                    case SIN_STOCK  -> getStyleClass().add("fila-sin-stock");
                    case STOCK_BAJO -> getStyleClass().add("fila-stock-bajo");
                    default -> {}
                }
            }
        });

        tablaProductos.setPlaceholder(new Label("No hay productos registrados."));
    }

    /** Filtra por nombre o categoría mientras el usuario escribe. */
    private void configurarBusqueda() {
        productosFiltrados = new FilteredList<>(productos, p -> true);
        txtBuscar.textProperty().addListener((obs, old, texto) ->
                productosFiltrados.setPredicate(p -> {
                    if (texto == null || texto.isBlank()) return true;
                    String filtro = texto.toLowerCase();
                    return p.getNombre().toLowerCase().contains(filtro)
                            || p.getCategoria().toLowerCase().contains(filtro);
                })
        );
        tablaProductos.setItems(productosFiltrados);
    }

    private void configurarBotonesPorSeleccion() {
        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

        tablaProductos.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> {
                    boolean haySeleccion = sel != null;
                    boolean esAdmin = esAdmin();
                    btnEditar.setDisable(!haySeleccion);
                    btnEliminar.setDisable(!haySeleccion || !esAdmin);
                });
    }

    /** El botón Eliminar solo es visible para ADMIN. */
    private void configurarAccesoPorRol() {
        btnEliminar.setVisible(esAdmin());
        btnEliminar.setManaged(esAdmin());
    }

    // ─── Carga de datos ───────────────────────────────────────────────────────

    private void cargarProductos() {
        productos.setAll(productoService.listarTodos());

        int total    = productos.size();
        long bajStock = productos.stream()
                .filter(p -> p.getEstadoStock() != Producto.EstadoStock.OK)
                .count();

        lblConteo.setText(total + " producto(s) en inventario");

        if (bajStock > 0) {
            lblAlertaStock.setText("⚠  " + bajStock + " producto(s) con stock bajo o agotado");
            lblAlertaStock.setVisible(true);
            lblAlertaStock.setManaged(true);
        } else {
            lblAlertaStock.setVisible(false);
            lblAlertaStock.setManaged(false);
        }
    }

    // ─── Acciones ─────────────────────────────────────────────────────────────

    @FXML private void nuevoProducto()  { abrirModal(null); }

    @FXML
    private void editarProducto() {
        Producto sel = tablaProductos.getSelectionModel().getSelectedItem();
        if (sel != null) abrirModal(sel);
    }

    @FXML
    private void eliminarProducto() {
        Producto sel = tablaProductos.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar producto");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar «" + sel.getNombre() + "» del inventario?\n"
                + "Esta acción no se puede deshacer.");
        aplicarEstiloDialog(confirm);

        Optional<ButtonType> resp = confirm.showAndWait();
        if (resp.isPresent() && resp.get() == ButtonType.OK) {
            productoService.eliminar(sel.getId(), usuarioSesion.getUsuario());
            cargarProductos();
        }
    }

    // ─── Modal ────────────────────────────────────────────────────────────────

    private void abrirModal(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/agregarEditarProducto.fxml"));
            Parent root = loader.load();

            AgregarEditarProductoController ctrl = loader.getController();
            ctrl.inicializar(producto, usuarioSesion.getUsuario());

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(producto == null ? "Nuevo Producto" : "Editar Producto");
            modal.setResizable(false);
            Scene escena = new Scene(root);
            escena.getStylesheets().add(
                    getClass().getResource("/com/gymmanager/css/estilos.css").toExternalForm());
            modal.setScene(escena);
            modal.showAndWait();

            cargarProductos();

        } catch (IOException e) {
            mostrarError("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean esAdmin() {
        return usuarioSesion != null && usuarioSesion.getRol() == Usuario.Rol.ADMIN;
    }

    private void mostrarError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        aplicarEstiloDialog(a);
        a.showAndWait();
    }

    private void aplicarEstiloDialog(Alert alert) {
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/gymmanager/css/estilos.css").toExternalForm());
    }
}