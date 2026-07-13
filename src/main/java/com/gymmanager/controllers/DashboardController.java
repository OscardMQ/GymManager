package com.gymmanager.controllers;

import com.gymmanager.models.Usuario;
import com.gymmanager.services.AuthService;
import com.gymmanager.services.BitacoraService;
import com.gymmanager.services.NotificacionService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import com.gymmanager.controllers.VentasController;
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
    @FXML private Button btnMenuEmpleados;         // oculto para RECEPCIONISTA (Fase 6)
    @FXML private Button btnMenuReportes;

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
        setVisibilidadBoton(btnMenuEmpleados, esAdmin);      // Fase 6
        setVisibilidadBoton(btnMenuReportes, esAdmin);
        setVisibilidadBoton(btnMenuProductos, esAdmin);

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
        if (!usuarioActual.esAdmin()) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/bitacora.fxml"));
            Parent vista = loader.load();
            BitacoraController ctrl = loader.getController();
            ctrl.inicializar(usuarioActual);
            colocarEnContentArea(vista);
        } catch (IOException e) {
            System.err.println("[DashboardController] Error cargando bitácora: " + e.getMessage());
        }
    }
    @FXML private void menuMembresias() {
        if (!usuarioActual.esAdmin()) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/membresias.fxml"));
            Parent vista = loader.load();
            MembresiaController ctrl = loader.getController();
            ctrl.inicializar(usuarioActual);
            colocarEnContentArea(vista);
        } catch (IOException e) {
            System.err.println("[DashboardController] Error cargando membresías: " + e.getMessage());
        }
    }
    @FXML private void menuNotificaciones() {
        if (!usuarioActual.esAdmin()) return;
        cargarVista("/com/gymmanager/views/notificaciones.fxml");
    }

    @FXML
    private void menuEmpleados() {
        if (!usuarioActual.esAdmin()) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/empleados.fxml"));
            Parent vista = loader.load();
            EmpleadosController ctrl = loader.getController();
            ctrl.inicializar(usuarioActual);
            colocarEnContentArea(vista);
        } catch (IOException e) {
            System.err.println("[DashboardController] Error cargando empleados: " + e.getMessage());
        }
    }

    // a) Campo junto a btnMenuEmpleados:
    @FXML private Button btnMenuProductos;

// b) En configurarMenuPorRol() — Productos es visible para AMBOS roles.
//    No agregar a la lista de botones ocultos para RECEPCIONISTA.

    // c) Método de navegación:
    @FXML
    private void menuProductos() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/productos.fxml"));
            Parent vista = loader.load();
            ProductosController ctrl = loader.getController();
            ctrl.inicializar(usuarioActual);            // ajusta al nombre de tu campo de sesión
            colocarEnContentArea(vista);
        } catch (IOException e) {
            System.err.println("Error cargando módulo productos: " + e.getMessage());
        }
    }

    @FXML private Button btnMenuVentas;

    /** Carga el módulo Punto de Venta en el área central del dashboard. */
    @FXML
    private void menuVentas() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/ventasPuntoVenta.fxml")
            );
            Parent vista = loader.load();
            VentasController ctrl = loader.getController();
            ctrl.inicializar(usuarioActual);
            colocarEnContentArea(vista);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Carga de vistas ───────────────────────────────────────────────────────

    /** Carga el home con KPIs. Los datos se refrescan cada vez que se navega al home. */
    private void cargarDashboardHome() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/dashboard_home.fxml"));
            Node vista = loader.load();
            DashboardHomeController ctrl = loader.getController();
            ctrl.inicializar(usuarioActual);
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
    private void menuReportes() {
        if (!usuarioActual.esAdmin()) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/gymmanager/views/reportes.fxml")
            );
            Parent vista = loader.load();
            ReportesController ctrl = loader.getController();
            ctrl.inicializar(usuarioActual);
            colocarEnContentArea(vista);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void cerrarSesion() {
        // Registrar el cierre y limpiar la sesión en memoria
        if (usuarioActual != null) {
            BitacoraService.getInstance().registrar(
                    usuarioActual.getUsuario(), "LOGOUT", "Sesión cerrada");
        }
        AuthService.getInstance().cerrarSesion();
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