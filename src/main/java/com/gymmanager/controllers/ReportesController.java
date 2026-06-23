package com.gymmanager.controllers;

import com.gymmanager.models.Usuario;
import com.gymmanager.services.ReporteService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador del módulo de Reportes y Ganancias (Fase 9).
 * Solo accesible para el rol ADMIN.
 *
 * Las gráficas se construyen programáticamente (no en FXML) para tener
 * control total de tipos genéricos y estilos en tiempo de ejecución.
 *
 * Organización:
 *   Tab 1 → Ganancias (barras verticales, filtro día/semana/mes)
 *   Tab 2 → Productos más vendidos (tabla + barras horizontales)
 *   Tab 3 → Historial membresías
 *   Tab 4 → Historial ventas POS (doble clic → detalle)
 */
public class ReportesController {

    // ─── Tab 1: Ganancias ──────────────────────────────────────────────────────
    @FXML private RadioButton rbDia;
    @FXML private RadioButton rbSemana;
    @FXML private RadioButton rbMes;
    @FXML private Label       lblMes;
    @FXML private ChoiceBox<String>  cbMes;
    @FXML private ChoiceBox<Integer> cbAnio;
    @FXML private Label lblTotalPeriodo;
    @FXML private Label lblSinDatosGanancias;
    @FXML private VBox  contenedorChartGanancias;

    // ─── Tab 2: Productos más vendidos ─────────────────────────────────────────
    @FXML private TableView<String[]>           tablaProductos;
    @FXML private TableColumn<String[], String> colProdNombre;
    @FXML private TableColumn<String[], String> colProdCantidad;
    @FXML private HBox contenedorChartProductos;

    // ─── Tab 3: Historial membresías ───────────────────────────────────────────
    @FXML private TableView<String[]>           tablaMembresias;
    @FXML private TableColumn<String[], String> colMembFecha;
    @FXML private TableColumn<String[], String> colMembSocio;
    @FXML private TableColumn<String[], String> colMembMonto;
    @FXML private TableColumn<String[], String> colMembTipo;

    // ─── Tab 4: Historial ventas POS ───────────────────────────────────────────
    @FXML private TableView<String[]>           tablaVentas;
    @FXML private TableColumn<String[], String> colVentaFecha;
    @FXML private TableColumn<String[], String> colVentaHora;
    @FXML private TableColumn<String[], String> colVentaUsuario;
    @FXML private TableColumn<String[], String> colVentaTotal;

    private ReporteService reporteService;

    // Gráficas instanciadas en código (no en FXML) para control de tipos genéricos
    private BarChart<String, Number> chartGanancias;
    private BarChart<Number, String> chartProductos;

    // =========================================================================
    // INICIALIZACIÓN
    // =========================================================================

    public void inicializar(Usuario usuario) {
        reporteService = ReporteService.getInstance();

        configurarToggleGrupo();
        configurarSelectoresFiltro();
        crearGraficaGanancias();
        crearGraficaProductos();
        configurarTablaProductos();
        configurarTablaMembresias();
        configurarTablaVentas();

        rbMes.setSelected(true);
        actualizarVisibilidadFiltros();

        cargarGanancias();
        cargarProductosMasVendidos();
        cargarHistorialMembresias();
        cargarHistorialVentas();
    }

    // =========================================================================
    // FILTROS DEL TAB 1
    // =========================================================================

    private void configurarToggleGrupo() {
        ToggleGroup grupoPeriodo = new ToggleGroup();
        rbDia.setToggleGroup(grupoPeriodo);
        rbSemana.setToggleGroup(grupoPeriodo);
        rbMes.setToggleGroup(grupoPeriodo);
        grupoPeriodo.selectedToggleProperty().addListener(
                (obs, anterior, nuevo) -> actualizarVisibilidadFiltros()
        );
    }

    private void configurarSelectoresFiltro() {
        cbMes.setItems(FXCollections.observableArrayList(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        ));
        cbMes.getSelectionModel().select(LocalDate.now().getMonthValue() - 1);

        int anioActual = LocalDate.now().getYear();
        ObservableList<Integer> anios = FXCollections.observableArrayList();
        for (int i = anioActual; i >= anioActual - 4; i--) {
            anios.add(i);
        }
        cbAnio.setItems(anios);
        cbAnio.getSelectionModel().selectFirst();
    }

    private void actualizarVisibilidadFiltros() {
        boolean esPorDia = rbDia.isSelected();
        lblMes.setVisible(esPorDia);
        lblMes.setManaged(esPorDia);
        cbMes.setVisible(esPorDia);
        cbMes.setManaged(esPorDia);
    }

    // =========================================================================
    // TAB 1: GANANCIAS
    // =========================================================================

    private void crearGraficaGanancias() {
        CategoryAxis ejeX = new CategoryAxis();
        NumberAxis   ejeY = new NumberAxis();
        ejeX.setLabel("Período");
        ejeY.setLabel("Total ($)");
        aplicarEstiloEje(ejeX);
        aplicarEstiloEje(ejeY);

        chartGanancias = new BarChart<>(ejeX, ejeY);
        chartGanancias.setAnimated(false);
        chartGanancias.setLegendVisible(false);
        chartGanancias.setBarGap(4);
        chartGanancias.setCategoryGap(20);
        chartGanancias.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(chartGanancias, Priority.ALWAYS);
        contenedorChartGanancias.getChildren().add(chartGanancias);
    }

    @FXML
    private void cargarGanancias() {
        int anio = cbAnio.getValue() != null ? cbAnio.getValue() : LocalDate.now().getYear();
        List<double[]> datos;

        if (rbDia.isSelected()) {
            int mes = cbMes.getSelectionModel().getSelectedIndex() + 1;
            datos = reporteService.gananciasPorDia(mes, anio);
        } else if (rbSemana.isSelected()) {
            datos = reporteService.gananciasPorSemana(anio);
        } else {
            datos = reporteService.gananciasPorMes(anio);
        }

        boolean sinDatos = datos.isEmpty();
        lblSinDatosGanancias.setVisible(sinDatos);
        lblSinDatosGanancias.setManaged(sinDatos);
        chartGanancias.setVisible(!sinDatos);
        chartGanancias.setManaged(!sinDatos);
        chartGanancias.getData().clear();

        if (sinDatos) {
            lblTotalPeriodo.setText("");
            return;
        }

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        double totalPeriodo = 0;

        for (double[] punto : datos) {
            String etiqueta = construirEtiqueta((int) punto[0]);
            serie.getData().add(new XYChart.Data<>(etiqueta, punto[1]));
            totalPeriodo += punto[1];
        }

        chartGanancias.getData().add(serie);

        // Colorear barras naranja con ancho máximo fijo para evitar barra gigante
        for (XYChart.Data<String, Number> item : serie.getData()) {
            item.nodeProperty().addListener((obs, anterior, nodo) -> {
                if (nodo != null) {
                    nodo.setStyle("-fx-bar-fill: #ff6b35; -fx-max-width: 60px;");
                }
            });
        }

        lblTotalPeriodo.setText(String.format("Total del período: $%.2f", totalPeriodo));
    }

    private String construirEtiqueta(int valor) {
        if (rbMes.isSelected()) {
            String[] nombresMes = {
                    "Ene","Feb","Mar","Abr","May","Jun",
                    "Jul","Ago","Sep","Oct","Nov","Dic"
            };
            return (valor >= 1 && valor <= 12) ? nombresMes[valor - 1] : String.valueOf(valor);
        }
        if (rbSemana.isSelected()) return "Sem " + valor;
        return "D" + valor;
    }

    // =========================================================================
    // TAB 2: PRODUCTOS MÁS VENDIDOS
    // =========================================================================

    private void crearGraficaProductos() {
        NumberAxis   ejeX = new NumberAxis();
        CategoryAxis ejeY = new CategoryAxis();
        ejeX.setLabel("Unidades vendidas");
        aplicarEstiloEje(ejeX);
        aplicarEstiloEje(ejeY);

        chartProductos = new BarChart<>(ejeX, ejeY);
        chartProductos.setAnimated(false);
        chartProductos.setLegendVisible(false);
        chartProductos.setBarGap(2);
        chartProductos.setCategoryGap(6);
        chartProductos.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(chartProductos, Priority.ALWAYS);
        contenedorChartProductos.getChildren().add(chartProductos);
    }

    private void configurarTablaProductos() {
        colProdNombre.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue()[0]));
        colProdCantidad.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue()[1]));
        aplicarEstiloTabla(tablaProductos);
    }

    private void cargarProductosMasVendidos() {
        List<String[]> datos = reporteService.productosMasVendidos(10);
        tablaProductos.setItems(FXCollections.observableArrayList(datos));
        chartProductos.getData().clear();

        if (datos.isEmpty()) return;

        XYChart.Series<Number, String> serie = new XYChart.Series<>();
        for (int i = datos.size() - 1; i >= 0; i--) {
            String[] fila = datos.get(i);
            serie.getData().add(new XYChart.Data<>(
                    Integer.parseInt(fila[1]), fila[0]
            ));
        }
        chartProductos.getData().add(serie);

        // Colorear barras naranja con alto máximo fijo
        for (XYChart.Data<Number, String> item : serie.getData()) {
            item.nodeProperty().addListener((obs, anterior, nodo) -> {
                if (nodo != null) {
                    nodo.setStyle("-fx-bar-fill: #ff6b35; -fx-max-height: 40px;");
                }
            });
        }
    }

    // =========================================================================
    // TAB 3: HISTORIAL MEMBRESÍAS
    // =========================================================================

    private void configurarTablaMembresias() {
        colMembFecha.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue()[0]));
        colMembSocio.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue()[1]));
        colMembMonto.setCellValueFactory(
                d -> new SimpleStringProperty("$" + d.getValue()[2]));
        colMembTipo.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue()[3]));
        aplicarEstiloTabla(tablaMembresias);
    }

    private void cargarHistorialMembresias() {
        List<String[]> datos = reporteService.historialPagosMembresia();
        tablaMembresias.setItems(FXCollections.observableArrayList(datos));
    }

    // =========================================================================
    // TAB 4: HISTORIAL VENTAS POS
    // =========================================================================

    private void configurarTablaVentas() {
        colVentaFecha.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue()[1]));
        colVentaHora.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue()[2]));
        colVentaUsuario.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue()[3]));
        colVentaTotal.setCellValueFactory(
                d -> new SimpleStringProperty("$" + d.getValue()[4]));
        aplicarEstiloTabla(tablaVentas);

        tablaVentas.setRowFactory(tv -> {
            TableRow<String[]> fila = new TableRow<>();
            fila.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !fila.isEmpty()) {
                    mostrarDetalleVenta(fila.getItem());
                }
            });
            return fila;
        });
    }

    private void cargarHistorialVentas() {
        List<String[]> datos = reporteService.historialVentas();
        tablaVentas.setItems(FXCollections.observableArrayList(datos));
    }

    private void mostrarDetalleVenta(String[] venta) {
        int ventaId;
        try {
            ventaId = Integer.parseInt(venta[0]);
        } catch (NumberFormatException e) {
            return;
        }

        List<String[]> detalle = reporteService.detalleVenta(ventaId);

        if (detalle.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "No se encontró el detalle de esta venta.",
                    ButtonType.OK).showAndWait();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "Venta #%s   %s   %s   Usuario: %s%n",
                venta[0], venta[1], venta[2], venta[3]
        ));
        sb.append("─".repeat(64)).append("\n");
        sb.append(String.format("%-26s  %6s  %11s  %11s%n",
                "Producto", "Cant.", "P.Unit.", "Subtotal"));
        sb.append("─".repeat(64)).append("\n");

        for (String[] item : detalle) {
            sb.append(String.format("%-26s  %6s  %11s  %11s%n",
                    truncar(item[0], 26),
                    item[1],
                    "$" + item[2],
                    "$" + item[3]
            ));
        }

        sb.append("─".repeat(64)).append("\n");
        sb.append(String.format("%53s $%s%n", "TOTAL:", venta[4]));

        TextArea areaTexto = new TextArea(sb.toString());
        areaTexto.setEditable(false);
        areaTexto.setWrapText(false);
        areaTexto.setPrefSize(560, 290);
        areaTexto.setStyle(
                "-fx-font-family: 'Courier New', Courier, monospace;" +
                        "-fx-font-size: 12;" +
                        "-fx-control-inner-background: #0f1117;" +
                        "-fx-text-fill: #eeeeee;"
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle de Venta");
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(areaTexto);
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setStyle(
                "-fx-background-color: #1a1d2e;" +
                        "-fx-border-color: #2a2d3e;"
        );
        alert.showAndWait();
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    private void aplicarEstiloEje(Axis<?> eje) {
        eje.setStyle("-fx-tick-label-fill: #aaaaaa; -fx-text-fill: #aaaaaa;");
    }

    private <T> void aplicarEstiloTabla(TableView<T> tabla) {
        tabla.setStyle(
                "-fx-background-color: #1a1d2e;" +
                        "-fx-border-color: #2a2d3e;"
        );
        Label sinDatos = new Label("Sin datos para mostrar");
        sinDatos.setStyle("-fx-text-fill: #666666; -fx-font-size: 13;");
        tabla.setPlaceholder(sinDatos);
    }

    private String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() > max ? texto.substring(0, max - 1) + "…" : texto;
    }
}