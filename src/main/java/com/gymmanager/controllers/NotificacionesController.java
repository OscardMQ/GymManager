package com.gymmanager.controllers;

import com.gymmanager.models.Notificacion;
import com.gymmanager.services.BackupService;
import com.gymmanager.services.NotificacionService;
import com.gymmanager.services.WhatsAppException;
import com.gymmanager.services.WhatsAppService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * Controlador de la vista de notificaciones.
 * Maneja el formulario de configuración de CallMeBot
 * y muestra el historial de notificaciones enviadas.
 */
public class NotificacionesController {

    // ── Configuración ─────────────────────────────────────────────────────────
    @FXML private TextField txtNumeroDueno;
    @FXML private TextField txtApiKey;
    @FXML private Label     lblEstadoConfig;

    // ── Respaldos ─────────────────────────────────────────────────────────────
    @FXML private Label     lblCarpetaLocal;
    @FXML private TextField txtCarpetaSecundaria;
    @FXML private Label     lblEstadoRespaldo;

    // ── Historial ─────────────────────────────────────────────────────────────
    @FXML private TableView<Notificacion>           tablaHistorial;
    @FXML private TableColumn<Notificacion, String> colFecha;
    @FXML private TableColumn<Notificacion, String> colSocio;
    @FXML private TableColumn<Notificacion, String> colTipo;
    @FXML private TableColumn<Notificacion, String> colEstado;
    @FXML private TableColumn<Notificacion, String> colMensaje;
    @FXML private Label lblConteoHistorial;

    private final WhatsAppService     whatsApp     = WhatsAppService.getInstance();
    private final NotificacionService notifService = NotificacionService.getInstance();

    @FXML
    public void initialize() {
        configurarTabla();
        cargarConfiguracionEnCampos();
        cargarConfiguracionRespaldos();
        cargarHistorial();
    }

    // ── Respaldos ─────────────────────────────────────────────────────────────

    private void cargarConfiguracionRespaldos() {
        BackupService backup = BackupService.getInstance();
        lblCarpetaLocal.setText("Respaldo diario automático en: " + backup.getCarpetaLocal());
        backup.obtenerCarpetaSecundaria().ifPresent(txtCarpetaSecundaria::setText);
    }

    @FXML
    private void elegirCarpetaRespaldo() {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle("Elegir carpeta secundaria de respaldo");
        File carpeta = selector.showDialog(txtCarpetaSecundaria.getScene().getWindow());
        if (carpeta == null) return;

        boolean ok = BackupService.getInstance().guardarCarpetaSecundaria(carpeta.getAbsolutePath());
        txtCarpetaSecundaria.setText(ok ? carpeta.getAbsolutePath() : "");
        setEstadoRespaldo(ok
                ? "✅  Carpeta guardada. Los respaldos también se copiarán ahí."
                : "❌  No se pudo guardar la carpeta.", ok);
    }

    @FXML
    private void quitarCarpetaRespaldo() {
        boolean ok = BackupService.getInstance().guardarCarpetaSecundaria("");
        if (ok) txtCarpetaSecundaria.clear();
        setEstadoRespaldo(ok
                ? "Carpeta secundaria eliminada; solo respaldo local."
                : "❌  No se pudo actualizar la configuración.", ok);
    }

    /** Respaldo manual en hilo de fondo para no congelar la UI. */
    @FXML
    private void respaldarAhora() {
        setEstadoRespaldo("⏳  Creando respaldo...", true);
        Thread t = new Thread(() -> {
            try {
                var ruta = BackupService.getInstance().respaldarAhora();
                Platform.runLater(() ->
                        setEstadoRespaldo("✅  Respaldo creado: " + ruta, true));
            } catch (Exception e) {
                Platform.runLater(() ->
                        setEstadoRespaldo("❌  " + e.getMessage(), false));
            }
        }, "hilo-respaldo-manual");
        t.setDaemon(true);
        t.start();
    }

    private void setEstadoRespaldo(String mensaje, boolean exito) {
        lblEstadoRespaldo.setText(mensaje);
        lblEstadoRespaldo.setStyle(exito ? "-fx-text-fill: #2e7d32;" : "-fx-text-fill: #c62828;");
    }

    // ── Configuración ─────────────────────────────────────────────────────────

    /** Precarga los campos con los valores ya guardados en BD. */
    private void cargarConfiguracionEnCampos() {
        whatsApp.obtenerConfiguracion(whatsApp.getClaveNumeroDueno())
                .ifPresent(txtNumeroDueno::setText);
        whatsApp.obtenerConfiguracion(whatsApp.getClaveToken())
                .ifPresent(txtApiKey::setText);
    }

    @FXML
    private void guardarConfiguracion() {
        String numero = txtNumeroDueno.getText().trim();
        String token  = txtApiKey.getText().trim();

        if (numero.isEmpty() || token.isEmpty()) {
            setEstadoConfig("⚠  Completa ambos campos antes de guardar.", false);
            return;
        }

        boolean ok = whatsApp.guardarConfiguracion(whatsApp.getClaveNumeroDueno(), numero)
                & whatsApp.guardarConfiguracion(whatsApp.getClaveToken(), token);

        setEstadoConfig(ok
                ? "✅  Configuración guardada."
                : "❌  Error al guardar en base de datos.", ok);
    }

    /**
     * Guarda lo que esté en pantalla y lanza un mensaje de prueba al dueño.
     * La llamada HTTP ocurre en un hilo de fondo para no congelar la UI.
     */
    @FXML
    private void probarEnvio() {
        guardarConfiguracion();
        setEstadoConfig("⏳  Enviando mensaje de prueba...", true);

        Thread t = new Thread(() -> {
            try {
                whatsApp.enviarAlDueno(
                        "🧪 *GymManager* — Mensaje de prueba. " +
                                "La integración con CallMeBot funciona correctamente.");
                Platform.runLater(() ->
                        setEstadoConfig("✅  Mensaje de prueba enviado con éxito.", true));
            } catch (WhatsAppException e) {
                Platform.runLater(() ->
                        setEstadoConfig("❌  " + e.getMessage(), false));
            }
        }, "hilo-prueba-whatsapp");
        t.setDaemon(true);
        t.start();
    }

    // ── Historial ─────────────────────────────────────────────────────────────

    /** Configura columnas y la fábrica de celdas para colorear el estado. */
    private void configurarTabla() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaEnvio"));
        colSocio.setCellValueFactory(new PropertyValueFactory<>("nombreSocio"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colMensaje.setCellValueFactory(new PropertyValueFactory<>("mensaje"));

        // Color semántico por estado de envío
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(estado);
                setStyle(switch (estado) {
                    case "ENVIADO"      -> "-fx-text-fill: #16a34a; -fx-font-weight: bold;";
                    case "ERROR"        -> "-fx-text-fill: #dc2626; -fx-font-weight: bold;";
                    case "SIN_WHATSAPP" -> "-fx-text-fill: #8b8fa8;";
                    default             -> "-fx-text-fill: white;";
                });
            }
        });

        tablaHistorial.setPlaceholder(
                new Label("Sin notificaciones en los últimos 30 días"));
    }

    @FXML
    private void refrescarHistorial() {
        cargarHistorial();
    }

    void cargarHistorial() {
        try {
            List<Notificacion> lista = notifService.obtenerRecientes(30);
            tablaHistorial.setItems(FXCollections.observableArrayList(lista));
            lblConteoHistorial.setText(lista.size() + " registros en los últimos 30 días");
        } catch (SQLException e) {
            lblConteoHistorial.setText("Error al cargar historial.");
            System.err.println("[NotificacionesController] " + e.getMessage());
        }
    }

    private void setEstadoConfig(String texto, boolean exitoso) {
        lblEstadoConfig.setText(texto);
        lblEstadoConfig.setStyle(exitoso
                ? "-fx-text-fill: #16a34a;"
                : "-fx-text-fill: #dc2626;");
    }
}