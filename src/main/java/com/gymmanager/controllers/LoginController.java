package com.gymmanager.controllers;

import com.gymmanager.models.LoginRequest;
import com.gymmanager.models.Usuario;
import com.gymmanager.services.AuthService;
import com.gymmanager.services.BitacoraService;
import com.gymmanager.services.RecuperacionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
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
     * Recuperación de contraseña del administrador (sin puerta trasera).
     * Escribe un código en un archivo de la carpeta de datos local; solo
     * quien tiene acceso físico a la computadora puede leerlo y completar
     * el reseteo. El código nunca se muestra en pantalla.
     */
    @FXML
    private void handleRecuperar() {
        limpiarError();

        List<Usuario> admins = authService.listarAdministradores();
        if (admins.isEmpty()) {
            mostrarError("No hay cuentas de administrador para recuperar.");
            return;
        }

        Path archivo;
        try {
            archivo = RecuperacionService.getInstance().generarCodigo();
        } catch (IOException e) {
            mostrarError("No se pudo iniciar la recuperación: " + e.getMessage());
            return;
        }

        ComboBox<String> comboAdmin = new ComboBox<>();
        admins.forEach(u -> comboAdmin.getItems().add(u.getUsuario()));
        comboAdmin.getSelectionModel().selectFirst();

        TextField     campoCodigo = new TextField();
        PasswordField nueva       = new PasswordField();
        PasswordField confirma    = new PasswordField();
        campoCodigo.setPromptText("Código del archivo");
        nueva.setPromptText("Nueva contraseña (mínimo 8 caracteres)");
        confirma.setPromptText("Confirmar contraseña");

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Recuperar contraseña");
        dialogo.setHeaderText(
                "Se escribió un código en el archivo:\n" + archivo + "\n\n"
                + "Ábrelo en esa computadora, copia el código y escríbelo aquí "
                + "para establecer una contraseña nueva.");

        VBox caja = new VBox(10,
                new Label("Administrador:"), comboAdmin,
                new Label("Código de recuperación:"), campoCodigo,
                new Label("Nueva contraseña:"), nueva, confirma);
        caja.setPadding(new Insets(10));
        dialogo.getDialogPane().setContent(caja);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        while (true) {
            Optional<ButtonType> res = dialogo.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) {
                return; // el usuario canceló; el código en memoria expira solo
            }

            String pass  = nueva.getText();
            String error = null;
            if (!RecuperacionService.getInstance().validar(campoCodigo.getText()))
                error = "Código incorrecto o expirado (válido 10 minutos).";
            else if (pass.length() < 8)
                error = "La contraseña debe tener al menos 8 caracteres.";
            else if (!pass.equals(confirma.getText()))
                error = "Las contraseñas no coinciden.";

            if (error == null) {
                String nombreAdmin = comboAdmin.getValue();
                Usuario admin = admins.stream()
                        .filter(u -> u.getUsuario().equals(nombreAdmin))
                        .findFirst().orElseThrow();
                try {
                    authService.cambiarContrasena(admin.getId(), pass);
                    RecuperacionService.getInstance().limpiar();
                    bitacoraService.registrar(nombreAdmin, "RECUPERAR_CONTRASENA",
                            "Contraseña restablecida mediante código local");
                    campoUsuario.setText(nombreAdmin);
                    campoContrasena.clear();
                    campoContrasena.requestFocus();
                    mostrarExito("Contraseña restablecida. Ingresa con la nueva contraseña.");
                    return;
                } catch (SQLException e) {
                    error = "Error de base de datos: " + e.getMessage();
                }
            }
            dialogo.setHeaderText(error + "\nIntenta de nuevo.\n\nArchivo: " + archivo);
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
        etiquetaError.setStyle("");
        etiquetaError.setVisible(true);
        etiquetaError.setManaged(true);
    }

    /** Reutiliza la etiqueta de error pero en verde para confirmaciones. */
    private void mostrarExito(String mensaje) {
        etiquetaError.setText(mensaje);
        etiquetaError.setStyle("-fx-text-fill: #2e7d32;");
        etiquetaError.setVisible(true);
        etiquetaError.setManaged(true);
    }

    private void limpiarError() {
        etiquetaError.setStyle("");
        etiquetaError.setVisible(false);
        etiquetaError.setManaged(false);
    }
}
