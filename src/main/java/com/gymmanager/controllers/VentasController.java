package com.gymmanager.controllers;

import com.gymmanager.models.DetalleVenta;
import com.gymmanager.models.Producto;
import com.gymmanager.models.Usuario;
import com.gymmanager.services.ProductoService;
import com.gymmanager.services.VentaService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Controlador del módulo Punto de Venta.
 *
 * Panel izquierdo: catálogo de productos con búsqueda en tiempo real.
 * Panel derecho:   carrito de compra con total y acciones de cobro.
 *
 * Visible para ADMIN y RECEPCIONISTA.
 * Doble clic sobre un producto agrega al carrito (atajo de teclado/mouse).
 */
public class VentasController {

    // ── Catálogo (panel izquierdo) ──
    @FXML private TextField                       txtBuscarProducto;
    @FXML private TableView<Producto>             tblProductos;
    @FXML private TableColumn<Producto, String>   colProdNombre;
    @FXML private TableColumn<Producto, String>   colProdCategoria;
    @FXML private TableColumn<Producto, Double>   colProdPrecio;
    @FXML private TableColumn<Producto, Integer>  colProdStock;

    // ── Carrito (panel derecho) ──
    @FXML private TableView<DetalleVenta>            tblCarrito;
    @FXML private TableColumn<DetalleVenta, String>  colCarNombre;
    @FXML private TableColumn<DetalleVenta, Integer> colCarCantidad;
    @FXML private TableColumn<DetalleVenta, Double>  colCarPrecio;
    @FXML private TableColumn<DetalleVenta, Double>  colCarSubtotal;
    @FXML private Label lblTotal;
    @FXML private Label lblMensaje;

    // ── Estado interno ──
    private ObservableList<Producto>     todosLosProductos;
    private FilteredList<Producto>       productosFiltrados;
    private ObservableList<DetalleVenta> carrito;

    private ProductoService productoService;
    private VentaService    ventaService;
    private Usuario         usuarioActual;

    // ──────────────────────────────────────────────
    //  Inicialización (llamada desde DashboardController)
    // ──────────────────────────────────────────────

    /**
     * Punto de entrada desde DashboardController.
     * Configura tablas, búsqueda y carga el catálogo de productos.
     */
    public void inicializar(Usuario usuario) {
        this.usuarioActual   = usuario;
        this.productoService = ProductoService.getInstance();
        this.ventaService    = VentaService.getInstance();
        this.carrito         = FXCollections.observableArrayList();

        configurarTablaProductos();
        configurarTablaCarrito();
        configurarBusqueda();

        tblCarrito.setItems(carrito);
        actualizarTotal();

        // Ocultar mensaje hasta que haya algo que mostrar
        lblMensaje.setVisible(false);
        lblMensaje.setManaged(false);

        cargarProductos();
    }

    // ──────────────────────────────────────────────
    //  Configuración de tablas
    // ──────────────────────────────────────────────

    private void configurarTablaProductos() {
        colProdNombre.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNombre()));

        colProdCategoria.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategoria()));

        // Precio con formato $X.XX
        colProdPrecio.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getPrecio()).asObject());
        colProdPrecio.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                setText(empty || precio == null ? null : String.format("$%.2f", precio));
            }
        });

        colProdStock.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getStock()).asObject());

        // Fila roja si sin stock, amarilla si stock bajo
        tblProductos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Producto p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setStyle("");
                } else if (p.getStock() == 0) {
                    setStyle("-fx-background-color: rgba(220,38,38,0.20);");
                } else if (p.getEstadoStock() != Producto.EstadoStock.OK) {
                    setStyle("-fx-background-color: rgba(234,179,8,0.12);");
                } else {
                    setStyle("");
                }
            }
        });

        // Doble clic como atajo para agregar al carrito
        tblProductos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !tblProductos.getSelectionModel().isEmpty()) {
                agregarAlCarrito();
            }
        });
    }

    private void configurarTablaCarrito() {
        colCarNombre.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNombreProducto()));

        colCarCantidad.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getCantidad()).asObject());

        colCarPrecio.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getPrecioUnitario()).asObject());
        colCarPrecio.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                setText(empty || precio == null ? null : String.format("$%.2f", precio));
            }
        });

        colCarSubtotal.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getSubtotal()).asObject());
        colCarSubtotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double sub, boolean empty) {
                super.updateItem(sub, empty);
                setText(empty || sub == null ? null : String.format("$%.2f", sub));
            }
        });
    }

    /** Conecta el campo de búsqueda con el predicado del FilteredList. */
    private void configurarBusqueda() {
        txtBuscarProducto.textProperty().addListener((obs, old, filtro) ->
                productosFiltrados.setPredicate(p -> {
                    if (filtro == null || filtro.isBlank()) return true;
                    String lc = filtro.toLowerCase();
                    return p.getNombre().toLowerCase().contains(lc)
                            || p.getCategoria().toLowerCase().contains(lc);
                })
        );
    }

    // ──────────────────────────────────────────────
    //  Carga de datos desde BD
    // ──────────────────────────────────────────────

    /** Recarga el catálogo desde la BD (necesario después de una venta para ver stock actualizado). */
    private void cargarProductos() {
        try {
            todosLosProductos  = FXCollections.observableArrayList(productoService.listarTodos());
            productosFiltrados = new FilteredList<>(todosLosProductos, p -> true);
            tblProductos.setItems(productosFiltrados);
        } catch (Exception e) {
            mostrarError("Error al cargar productos: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    //  Acciones del usuario
    // ──────────────────────────────────────────────

    /** Agrega el producto seleccionado al carrito o incrementa su cantidad si ya existe. */
    @FXML
    private void agregarAlCarrito() {
        Producto seleccionado = tblProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarError("Selecciona un producto del catálogo.");
            return;
        }
        if (seleccionado.getStock() == 0) {
            mostrarError("\"" + seleccionado.getNombre() + "\" no tiene stock disponible.");
            return;
        }

        Optional<DetalleVenta> yaEnCarrito = carrito.stream()
                .filter(d -> d.getProductoId() == seleccionado.getId())
                .findFirst();

        if (yaEnCarrito.isPresent()) {
            DetalleVenta detalle = yaEnCarrito.get();
            if (detalle.getCantidad() >= seleccionado.getStock()) {
                mostrarError("Ya agregaste todo el stock disponible de \"" + seleccionado.getNombre() + "\".");
                return;
            }
            detalle.setCantidad(detalle.getCantidad() + 1);
            // ObservableList no detecta cambios internos del objeto; forzar refresco
            tblCarrito.refresh();
        } else {
            carrito.add(new DetalleVenta(
                    seleccionado.getId(),
                    seleccionado.getNombre(),
                    seleccionado.getPrecio(),
                    1
            ));
        }

        actualizarTotal();
        ocultarMensaje();
    }

    /** Elimina el ítem seleccionado del carrito completamente. */
    @FXML
    private void quitarDelCarrito() {
        DetalleVenta seleccionado = tblCarrito.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarError("Selecciona un artículo del carrito para quitarlo.");
            return;
        }
        carrito.remove(seleccionado);
        actualizarTotal();
        ocultarMensaje();
    }

    /** Muestra confirmación y registra la venta si el usuario confirma. */
    @FXML
    private void cobrar() {
        if (carrito.isEmpty()) {
            mostrarError("El carrito está vacío. Agrega productos antes de cobrar.");
            return;
        }

        double total = calcularTotal();

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar cobro");
        confirmacion.setHeaderText(String.format("Total a cobrar: $%.2f", total));
        confirmacion.setContentText(
                String.format("%d artículo(s) en el carrito.%n¿Registrar la venta?", carrito.size())
        );
        aplicarEstiloOscuro(confirmacion);

        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) return;

        try {
            ventaService.registrarVenta(carrito, usuarioActual.getUsuario());
            limpiarCarrito();         // primero limpiar...
            cargarProductos();        // ...luego refrescar stock en catálogo
            mostrarExito(String.format("✓ Venta registrada — Total cobrado: $%.2f", total));
        } catch (IllegalStateException e) {
            mostrarError(e.getMessage());
        } catch (SQLException e) {
            mostrarError("Error al registrar la venta: " + e.getMessage());
        }
    }

    /** Vacía el carrito sin registrar ninguna venta. */
    @FXML
    private void limpiarCarrito() {
        carrito.clear();
        actualizarTotal();
        ocultarMensaje();
    }

    // ──────────────────────────────────────────────
    //  Utilidades
    // ──────────────────────────────────────────────

    private double calcularTotal() {
        return carrito.stream().mapToDouble(DetalleVenta::getSubtotal).sum();
    }

    private void actualizarTotal() {
        lblTotal.setText(String.format("$%.2f", calcularTotal()));
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText("⚠  " + mensaje);
        lblMensaje.setStyle("-fx-text-fill: #dc2626;");
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private void mostrarExito(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: #16a34a;");
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private void ocultarMensaje() {
        lblMensaje.setVisible(false);
        lblMensaje.setManaged(false);
    }

    /** Aplica fondo oscuro al diálogo de confirmación para que no rompa el tema. */
    private void aplicarEstiloOscuro(Alert alert) {
        alert.getDialogPane().setStyle(
                "-fx-background-color: #1a1d2e;"
        );
        alert.getDialogPane().lookup(".content.label").setStyle(
                "-fx-text-fill: #c5c7d4;"
        );
    }
}