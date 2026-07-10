package com.gymmanager.controllers;

import com.gymmanager.models.Socio;
import com.gymmanager.models.Usuario;
import com.gymmanager.services.BitacoraService;
import com.gymmanager.services.SocioService;
import com.gymmanager.utils.FechaUtils;
import com.gymmanager.utils.Iconos;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class SociosController {

    @FXML private TableView<Socio>           tablaSocios;
    @FXML private TableColumn<Socio, String> colNombre;
    @FXML private TableColumn<Socio, String> colMembresia;
    @FXML private TableColumn<Socio, String> colFechaInicio;
    @FXML private TableColumn<Socio, String> colFechaFin;
    @FXML private TableColumn<Socio, String> colEstado;
    @FXML private TableColumn<Socio, String> colDias;
    @FXML private TextField                  campoBusqueda;
    @FXML private Button                     btnNuevo;
    @FXML private Button                     btnEditar;
    @FXML private Button                     btnBaja;
    @FXML private Label                      etiquetaInfo;
    @FXML private ComboBox<String>           filtroEstado;

    private final SocioService socioService = SocioService.getInstance();
    private ObservableList<Socio> todosSocios = FXCollections.observableArrayList();
    private FilteredList<Socio> sociosFiltrados;
    private Usuario usuarioActual;

    public void inicializar(Usuario usuario) {
        this.usuarioActual = usuario;
        configurarColumnas();
        configurarFiltros();
        configurarPermisos();
        configurarDobleClick();
        cargarSocios();
    }

    private void configurarColumnas() {
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colMembresia.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMembresiaNombre()));
        colFechaInicio.setCellValueFactory(c ->
                new SimpleStringProperty(FechaUtils.aFormatoDisplay(c.getValue().getFechaInicio())));
        colFechaFin.setCellValueFactory(c ->
                new SimpleStringProperty(FechaUtils.aFormatoDisplay(c.getValue().getFechaFin())));

        colEstado.setCellValueFactory(c -> {
            Socio s = c.getValue();
            if (!s.isActivo()) return new SimpleStringProperty("Inactivo");
            if (FechaUtils.estaVencida(s.getFechaFin())) return new SimpleStringProperty("Vencido");
            if (FechaUtils.diasRestantes(s.getFechaFin()) <= 7) return new SimpleStringProperty("Por vencer");
            return new SimpleStringProperty("Activo");
        });

        colDias.setCellValueFactory(c -> {
            Socio s = c.getValue();
            if (!s.isActivo() || FechaUtils.estaVencida(s.getFechaFin()))
                return new SimpleStringProperty("—");
            return new SimpleStringProperty(FechaUtils.diasRestantes(s.getFechaFin()) + " días");
        });

        tablaSocios.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Socio item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("fila-activo","fila-vencido","fila-baja","fila-por-vencer");
                if (item == null || empty) return;
                if (!item.isActivo())                                    getStyleClass().add("fila-baja");
                else if (FechaUtils.estaVencida(item.getFechaFin()))     getStyleClass().add("fila-vencido");
                else if (FechaUtils.diasRestantes(item.getFechaFin()) <= 7) getStyleClass().add("fila-por-vencer");
                else                                                     getStyleClass().add("fila-activo");
            }
        });
    }

    private void configurarFiltros() {
        filtroEstado.getItems().addAll("Todos","Activos","Vencidos","Por vencer","Inactivos");
        filtroEstado.setValue("Todos");
        sociosFiltrados = new FilteredList<>(todosSocios, s -> true);
        tablaSocios.setItems(sociosFiltrados);
        campoBusqueda.textProperty().addListener((obs, v, n) -> aplicarFiltro());
        filtroEstado.valueProperty().addListener((obs, v, n) -> aplicarFiltro());
    }

    private void configurarPermisos() {
        if (!usuarioActual.esAdmin()) btnBaja.setVisible(false);
    }

    private void configurarDobleClick() {
        tablaSocios.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
                abrirModal(tablaSocios.getSelectionModel().getSelectedItem());
        });
    }

    void cargarSocios() {
        try {
            todosSocios.setAll(socioService.listar());
            aplicarFiltro();
            actualizarContador();
        } catch (SQLException e) {
            mostrarError("Error al cargar socios: " + e.getMessage());
        }
    }

    private void aplicarFiltro() {
        String texto  = campoBusqueda.getText().toLowerCase().trim();
        String estado = filtroEstado.getValue();
        sociosFiltrados.setPredicate(s -> {
            boolean coincideNombre = texto.isEmpty() || s.getNombre().toLowerCase().contains(texto);
            boolean coincideEstado = switch (estado) {
                case "Activos"    -> s.isActivo() && !FechaUtils.estaVencida(s.getFechaFin());
                case "Vencidos"   -> s.isActivo() &&  FechaUtils.estaVencida(s.getFechaFin());
                case "Por vencer" -> s.isActivo() && !FechaUtils.estaVencida(s.getFechaFin())
                        && FechaUtils.diasRestantes(s.getFechaFin()) <= 7;
                case "Inactivos"  -> !s.isActivo();
                default           -> true;
            };
            return coincideNombre && coincideEstado;
        });
        actualizarContador();
    }

    private void actualizarContador() {
        int total    = todosSocios.size();
        int visibles = sociosFiltrados.size();
        etiquetaInfo.setText(total == visibles ? total + " socios" : visibles + " de " + total + " socios");
    }

    @FXML private void handleNuevo()  { abrirModal(null); }

    @FXML
    private void handleEditar() {
        Socio sel = tablaSocios.getSelectionModel().getSelectedItem();
        if (sel == null) { mostrarError("Selecciona un socio para editar."); return; }
        abrirModal(sel);
    }

    @FXML
    private void handleBaja() {
        Socio sel = tablaSocios.getSelectionModel().getSelectedItem();
        if (sel == null)        { mostrarError("Selecciona un socio para dar de baja."); return; }
        if (!sel.isActivo())    { mostrarError("El socio ya está inactivo."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Dar de baja");
        confirm.setHeaderText("¿Dar de baja a " + sel.getNombre() + "?");
        confirm.setContentText("El registro se conservará en la base de datos.");
        aplicarEstiloDialog(confirm);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try {
                    socioService.darDeBaja(sel.getId());
                    BitacoraService.getInstance().registrar(
                            usuarioActual.getUsuario(), "BAJA_SOCIO", "Baja de " + sel.getNombre());
                    cargarSocios();
                } catch (SQLException e) {
                    mostrarError("No se pudo dar de baja: " + e.getMessage());
                }
            }
        });
    }

    private void abrirModal(Socio socio) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/agregarEditarSocio.fxml"));
            Parent root = loader.load();
            AgregarEditarSocioController ctrl = loader.getController();
            ctrl.inicializar(socio, usuarioActual);

            Stage modal = new Stage();
            Iconos.aplicar(modal);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(socio == null ? "Nuevo Socio" : "Editar Socio");
            modal.setResizable(false);
            Scene escena = new Scene(root, 480, 560);
            escena.getStylesheets().add(
                    getClass().getResource("/com/gymmanager/css/estilos.css").toExternalForm());
            modal.setScene(escena);
            modal.showAndWait();
            cargarSocios();
        } catch (IOException e) {
            mostrarError("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void mostrarError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle("Error");
        aplicarEstiloDialog(alert);
        alert.showAndWait();
    }

    private void aplicarEstiloDialog(Alert alert) {
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/gymmanager/css/estilos.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dialog-dark");
    }
}