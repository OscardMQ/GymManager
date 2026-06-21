package com.gymmanager.controllers;

import com.gymmanager.models.Producto;
import com.gymmanager.services.ProductoService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Modal de alta/edición de productos.
 * La categoría acepta valores de la lista o texto personalizado (ComboBox editable).
 */
public class AgregarEditarProductoController {

    @FXML private Label            lblTitulo;
    @FXML private TextField        txtNombre;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private TextField        txtPrecio;
    @FXML private TextField        txtStock;
    @FXML private TextField        txtStockMinimo;
    @FXML private Label            lblError;
    @FXML private Button           btnGuardar;

    private static final String[] CATEGORIAS = {
            "Bebidas", "Suplementos", "Accesorios",
            "Equipamiento", "Higiene", "Ropa y Calzado", "Otro"
    };

    private final ProductoService productoService = ProductoService.getInstance();
    private Producto productoEditando;
    private String   realizadoPor;

    // ─── Inicialización ───────────────────────────────────────────────────────

    public void inicializar(Producto producto, String realizadoPor) {
        this.productoEditando = producto;
        this.realizadoPor     = realizadoPor;

        cmbCategoria.setItems(FXCollections.observableArrayList(CATEGORIAS));
        cmbCategoria.setEditable(true);
        ocultarError();

        if (producto != null) {
            lblTitulo.setText("Editar Producto");
            txtNombre.setText(producto.getNombre());
            cmbCategoria.setValue(producto.getCategoria());
            txtPrecio.setText(String.valueOf(producto.getPrecio()));
            txtStock.setText(String.valueOf(producto.getStock()));
            txtStockMinimo.setText(String.valueOf(producto.getStockMinimo()));
        } else {
            lblTitulo.setText("Nuevo Producto");
            cmbCategoria.setValue("General");
            txtStockMinimo.setText("5");
        }
    }

    // ─── Acciones ─────────────────────────────────────────────────────────────

    @FXML
    private void guardar() {
        ocultarError();
        try {
            String nombre    = txtNombre.getText().trim();
            String categoria = cmbCategoria.getValue() != null
                    ? cmbCategoria.getValue().trim() : "General";
            double precio    = parsearDouble(txtPrecio.getText(), "precio");
            int    stock     = parsearEntero(txtStock.getText(), "stock");
            int    stockMin  = parsearEntero(txtStockMinimo.getText(), "stock mínimo");

            if (productoEditando == null) {
                productoService.alta(nombre, categoria, precio, stock, stockMin, realizadoPor);
            } else {
                productoEditando.setNombre(nombre);
                productoEditando.setCategoria(categoria);
                productoEditando.setPrecio(precio);
                productoEditando.setStock(stock);
                productoEditando.setStockMinimo(stockMin);
                productoService.actualizar(productoEditando, realizadoPor);
            }
            cerrar();

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML private void cancelar() { cerrar(); }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private double parsearDouble(String texto, String campo) throws Exception {
        try {
            return Double.parseDouble(texto.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new Exception("El campo «" + campo + "» debe ser un número válido.");
        }
    }

    private int parsearEntero(String texto, String campo) throws Exception {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new Exception("El campo «" + campo + "» debe ser un número entero.");
        }
    }

    private void mostrarError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void ocultarError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void cerrar() {
        ((Stage) btnGuardar.getScene().getWindow()).close();
    }
}