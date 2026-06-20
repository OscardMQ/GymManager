package com.gymmanager.controllers;

import com.gymmanager.models.Usuario;
import com.gymmanager.services.NotificacionService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controlador principal del dashboard.
 * Gestiona la navegación del menú lateral y carga cada módulo en el área central.
 */
public class DashboardController {

    // ── Menú lateral ──────────────────────────────────────────────────────────
    @FXML private Button btnMenuDashboard;
    @FXML private Button btnMenuSocios;
    @FXML private Button btnMenuMembresias;        // oculto para RECEPCIONISTA
    @FXML private Button btnMenuBitacora;          // oculto para RECEPCIONISTA
    @FXML private Button btnMenuNotificaciones;    // oculto para RECEPCIONISTA (Fase 5)

    // ── Cabecera ──────────────────────────────────────────────────────────────
    @FXML private Label  lblNombreUsuario;
    @FXML private Label  lblRol;
    @FXML private Button btnCerrarSesion;

    // ── Área central donde se cargan los módulos ───────────────────────────────
    @FXML private AnchorPane contentArea;

    private Usuario usuarioActual;

    /**
     * Llamado por LoginController tras autenticar; recibe el usuario de la sesión.
     * No usa @FXML initialize() porque necesita datos externos al FXML.
     */
    public void inicializar(Usuario usuario) {
        this.usuarioActual = usuario;
        lblNombreUsuario.setText(usuario.getUsuario());
        lblRol.setText("Rol: " + usuario.getRol());
        configurarMenuPorRol();
        cargarDashboardHome();
        verificarNotificacionesEnFondo(); // Fase 5
    }

    /** Aplica visibilidad de botones según el rol del usuario autenticado. */
    private void configurarMenuPorRol() {
        boolean esAdmin = usuarioActual.esAdmin();

        setVisibilidadBoton(btnMenuMembresias, esAdmin);
        setVisibilidadBoton(btnMenuBitacora, esAdmin);
        setVisibilidadBoton(btnMenuNotificaciones, esAdmin); // Fase 5
    }

    private void setVisibilidadBoton(Button btn, boolean visible) {
        btn.setVisible(visible);
        btn.setManaged(visible); // libera espacio en el VBox cuando está oculto
    }

    // ── Manejadores del menú ──────────────────────────────────────────────────

    @FXML private void menuDashboard()      { cargarDashboardHome(); }
    @FXML private void menuSocios() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/socios.fxml"));
            Node vista = loader.load();
            SociosController ctrl = loader.getController();
            ctrl.inicializar(usuarioActual);
            colocarEnContentArea(vista);
        } catch (IOException e) {
            System.err.println("[DashboardController] Error cargando socios: " + e.getMessage());
        }
    }

    @FXML private void menuBitacora() {
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(12);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.control.Label l = new javafx.scene.control.Label("📋 Bitácora — próximamente");
        l.setStyle("-fx-text-fill:#8b8fa8; -fx-font-size:16px;");
        vbox.getChildren().add(l);
        colocarEnContentArea(vbox);
    }
    @FXML private void menuMembresias() {
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(12);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.control.Label l = new javafx.scene.control.Label("🏷 Membresías — próximamente");
        l.setStyle("-fx-text-fill:#8b8fa8; -fx-font-size:16px;");
        vbox.getChildren().add(l);
        colocarEnContentArea(vbox);
    }
    @FXML private void menuNotificaciones() { cargarVista("/com/gymmanager/views/notificaciones.fxml"); }

    // ── Carga de vistas ───────────────────────────────────────────────────────

    /** Carga el home con KPIs. Los datos se refrescan cada vez que se navega al home. */
    private void cargarDashboardHome() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/dashboard_home.fxml"));
            Node vista = loader.load();
            DashboardHomeController ctrl = loader.getController();
            ctrl.cargarKPIs();
            colocarEnContentArea(vista);
        } catch (IOException e) {
            System.err.println("[DashboardController] Error cargando home: " + e.getMessage());
        }
    }

    /** Carga cualquier vista FXML en el área central. */
    private void cargarVista(String ruta) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Node vista = loader.load();
            colocarEnContentArea(vista);
        } catch (IOException e) {
            System.err.println("[DashboardController] Error cargando " + ruta + ": " + e.getMessage());
        }
    }

    /** Estira el nodo para que ocupe todo el AnchorPane central. */
    private void colocarEnContentArea(Node vista) {
        AnchorPane.setTopAnchor(vista, 0.0);
        AnchorPane.setBottomAnchor(vista, 0.0);
        AnchorPane.setLeftAnchor(vista, 0.0);
        AnchorPane.setRightAnchor(vista, 0.0);
        contentArea.getChildren().setAll(vista);
    }

    /**
     * Lanza la verificación de vencimientos en un hilo daemon.
     * Si la API tarda o falla, la UI no se congela ni muestra error.
     */
    private void verificarNotificacionesEnFondo() {
        Thread t = new Thread(
                () -> NotificacionService.getInstance().verificarYNotificarVencimientos(),
                "verificacion-notificaciones"
        );
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void cerrarSesion() {
        try {
            Stage stage = (Stage) btnCerrarSesion.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/login.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("[DashboardController] Error al cerrar sesión: " + e.getMessage());
        }
    }
}