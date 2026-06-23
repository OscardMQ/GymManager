package com.gymmanager.controllers;

import com.gymmanager.models.Usuario;
import com.gymmanager.services.BitacoraService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador del módulo Bitácora.
 * Solo ADMIN. Vista de solo lectura con filtros por rango de fecha y tipo de acción.
 */
public class BitacoraController {

    @FXML private DatePicker                    dpDesde;
    @FXML private DatePicker                    dpHasta;
    @FXML private ChoiceBox<String>             cbAccion;
    @FXML private Button                        btnFiltrar;
    @FXML private Button                        btnLimpiar;

    @FXML private TableView<Object[]>           tablaBitacora;
    @FXML private TableColumn<Object[], String> colFecha;
    @FXML private TableColumn<Object[], String> colHora;
    @FXML private TableColumn<Object[], String> colUsuario;
    @FXML private TableColumn<Object[], String> colAccion;
    @FXML private TableColumn<Object[], String> colDescripcion;

    private Usuario usuarioActual;

    /** Formato ISO para convertir LocalDate del DatePicker a String de consulta SQL. */
    private static final DateTimeFormatter FMT_ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Punto de entrada ────────────────────────────────────────────────────

    /** Llamado por DashboardController tras cargar el FXML. */
    public void inicializar(Usuario usuario) {
        this.usuarioActual = usuario;
        configurarColumnas();
        cargarAcciones();
        cargarTodos();
    }

    // ── Configuración ────────────────────────────────────────────────────────

    /**
     * Enlaza cada columna con el índice correspondiente del Object[] de la fila.
     * [0]=fecha [1]=hora [2]=usuario [3]=accion [4]=descripcion
     */
    private void configurarColumnas() {
        colFecha.setCellValueFactory(      d -> new SimpleStringProperty(strVal(d.getValue(), 0)));
        colHora.setCellValueFactory(       d -> new SimpleStringProperty(strVal(d.getValue(), 1)));
        colUsuario.setCellValueFactory(    d -> new SimpleStringProperty(strVal(d.getValue(), 2)));
        colAccion.setCellValueFactory(     d -> new SimpleStringProperty(strVal(d.getValue(), 3)));
        colDescripcion.setCellValueFactory(d -> new SimpleStringProperty(strVal(d.getValue(), 4)));
    }

    /** Extrae un String de forma segura desde un Object[] evitando NullPointerException. */
    private String strVal(Object[] fila, int indice) {
        return (fila != null && indice < fila.length && fila[indice] != null)
                ? fila[indice].toString()
                : "";
    }

    /** Carga las acciones distintas de la BD en el ChoiceBox de filtros. */
    private void cargarAcciones() {
        List<String> acciones = BitacoraService.getInstance().listarAcciones();
        cbAccion.getItems().clear();
        cbAccion.getItems().add("Todas");
        cbAccion.getItems().addAll(acciones);
        cbAccion.setValue("Todas");
    }

    // ── Carga y filtrado ─────────────────────────────────────────────────────

    /** Carga todos los registros de la bitácora sin filtros activos. */
    private void cargarTodos() {
        List<Object[]> datos = BitacoraService.getInstance().listar();
        tablaBitacora.setItems(FXCollections.observableArrayList(datos));
    }

    @FXML
    private void onFiltrar() {
        String desde  = dpDesde.getValue() != null
                ? dpDesde.getValue().format(FMT_ISO) : null;
        String hasta  = dpHasta.getValue() != null
                ? dpHasta.getValue().format(FMT_ISO) : null;
        String accion = cbAccion.getValue();

        List<Object[]> datos = BitacoraService.getInstance()
                .listarConFiltros(desde, hasta, accion);
        tablaBitacora.setItems(FXCollections.observableArrayList(datos));
    }

    @FXML
    private void onLimpiar() {
        dpDesde.setValue(null);
        dpHasta.setValue(null);
        cbAccion.setValue("Todas");
        cargarTodos();
    }
}