package com.gymmanager.controllers;

import com.gymmanager.models.LoginRequest;
import com.gymmanager.models.Usuario;
import com.gymmanager.services.AuthService;
import com.gymmanager.services.BitacoraService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Controlador del formulario de login.
 * Coordina validación, autenticación y navegación al dashboard.
 *
 * Seguridad:
 *  - La verificación BCrypt corre en un hilo de fondo (no congela la UI).
 *  - 5 intentos fallidos seguidos bloquean el login por 30 segundos.
 *  - Entrar con la contraseña por defecto obliga a cambiarla antes de continuar.
 */
public class LoginController {

    /** Contraseña de la semilla inicial; entrar con ella exige cambiarla. */
    private static final String CONTRASENA_DEFAULT = "Admin123*";

    private static final int  MAX_INTENTOS       = 5;
    private static final long BLOQUEO_MILIS      = 30_000;

    // static: sobreviven al recargar la vista (logout → login)
    private static int  intentosFallidos = 0;
    private static long bloqueadoHasta   = 0;

    @FXML private TextField     campoUsuario;
    @FXML private PasswordField campoContrasena;
    @FXML private Label         etiquetaError;
    @FXML private Button        botonIngresar;

    private final AuthService     authService     = AuthService.getInstance();
    private final BitacoraService bitacoraService = BitacoraService.getInstance();

    /** Configura atajos de teclado al cargar la vista. */
    @FXML
    public void initialize() {
        // Enter en usuario → mueve foco a contraseña
        campoUsuario.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) campoContrasena.requestFocus();
        });

        // Enter en contraseña → intenta el login
        campoContrasena.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });
    }

    /** Punto de entrada del botón Ingresar. */
    @FXML
    private void handleLogin() {
        String usuario    = campoUsuario.getText().trim();
        String contrasena = campoContrasena.getText();

        limpiarError();

        long restante = bloqueadoHasta - System.currentTimeMillis();
        if (restante > 0) {
            mostrarError("Demasiados intentos fallidos. Espera "
                    + (restante / 1000 + 1) + " segundos.");
            return;
        }

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Por favor, ingresa usuario y contraseña.");
            return;
        }

        botonIngresar.setDisable(true);

        // BCrypt tarda ~300 ms: verificar en hilo de fondo para no congelar la UI
        Thread t = new Thread(() -> {
            Optional<Usuario> resultado =
                    authService.autenticar(new LoginRequest(usuario, contrasena));
            Platform.runLater(() -> procesarResultado(usuario, contrasena, resultado));
        }, "hilo-login");
        t.setDaemon(true);
        t.start();
    }

    /** Corre en el hilo de JavaFX una vez verificadas las credenciales. */
    private void procesarResultado(String usuario, String contrasena,
                                   Optional<Usuario> resultado) {
        botonIngresar.setDisable(false);

        if (resultado.isEmpty()) {
            intentosFallidos++;
            bitacoraService.registrar(usuario, "LOGIN_FALLIDO",
                    "Credenciales inválidas para: " + usuario);
            if (intentosFallidos >= MAX_INTENTOS) {
                bloqueadoHasta   = System.currentTimeMillis() + BLOQUEO_MILIS;
                intentosFallidos = 0;
                mostrarError("Demasiados intentos fallidos. Espera 30 segundos.");
            } else {
                mostrarError("Usuario o contraseña incorrectos.");
            }
            campoContrasena.clear();
            campoContrasena.requestFocus();
            return;
        }

        intentosFallidos = 0;
        Usuario autenticado = resultado.get();

        // Contraseña por defecto (pública en el repositorio): forzar cambio
        if (CONTRASENA_DEFAULT.equals(contrasena)) {
            if (!forzarCambioContrasena(autenticado)) {
                authService.cerrarSesion();
                mostrarError("Debes establecer una contraseña nueva para entrar.");
                campoContrasena.clear();
                return;
            }
        }

        bitacoraService.registrar(usuario, "LOGIN",
                "Sesión iniciada — Rol: " + autenticado.getRol());
        abrirDashboard(autenticado);
    }

    /**
     * Diálogo modal que obliga a definir una contraseña nueva.
     * @return true si la contraseña fue cambiada; false si el usuario canceló.
     */
    private boolean forzarCambioContrasena(Usuario usuario) {
        PasswordField nueva    = new PasswordField();
        PasswordField confirma = new PasswordField();
        nueva.setPromptText("Nueva contraseña (mínimo 8 caracteres)");
        confirma.setPromptText("Confirmar contraseña");

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Cambio de contraseña obligatorio");
        dialogo.setHeaderText("Estás usando la contraseña por defecto.\n"
                + "Por seguridad debes establecer una nueva antes de continuar.");
        VBox caja = new VBox(10, nueva, confirma);
        caja.setPadding(new Insets(10));
        dialogo.getDialogPane().setContent(caja);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        while (true) {
            Optional<ButtonType> res = dialogo.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) return false;

            String pass  = nueva.getText();
            String error = null;
            if (pass.length() < 8)                       error = "Mínimo 8 caracteres.";
            else if (!pass.equals(confirma.getText()))   error = "Las contraseñas no coinciden.";
            else if (pass.equals(CONTRASENA_DEFAULT))    error = "No puedes reutilizar la contraseña por defecto.";

            if (error == null) {
                try {
                    authService.cambiarContrasena(usuario.getId(), pass);
                    bitacoraService.registrar(usuario.getUsuario(), "CAMBIO_CONTRASENA",
                            "Contraseña por defecto reemplazada");
                    return true;
                } catch (SQLException e) {
                    error = "Error de base de datos: " + e.getMessage();
                }
            }
            dialogo.setHeaderText(error + "\nIntenta de nuevo.");
        }
    }

    /**
     * Carga el dashboard y pasa el usuario al controlador.
     * El tamaño y título del Stage cambian aquí.
     */
    private void abrirDashboard(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gymmanager/views/dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 700);
            scene.getStylesheets().add(
                    getClass().getResource("/com/gymmanager/css/estilos.css").toExternalForm());

            DashboardController dashCtrl = loader.getController();
            dashCtrl.inicializar(usuario);

            Stage stage = (Stage) botonIngresar.getScene().getWindow();
            stage.setTitle("GymManager — " + usuario.getUsuario());
            stage.setScene(scene);
            stage.setMaximized(true);

        } catch (IOException e) {
            mostrarError("Error al cargar el dashboard.");
            e.printStackTrace();
        }
    }

    private void mostrarError(String mensaje) {
        etiquetaError.setText(mensaje);
        etiquetaError.setVisible(true);
        etiquetaError.setManaged(true);
    }

    private void limpiarError() {
        etiquetaError.setVisible(false);
        etiquetaError.setManaged(false);
    }
}
