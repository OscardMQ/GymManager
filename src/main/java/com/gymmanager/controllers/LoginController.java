package com.gymmanager.controllers;

import com.gymmanager.models.LoginRequest;
import com.gymmanager.models.Usuario;
import com.gymmanager.services.AuthService;
import com.gymmanager.services.BitacoraService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

/**
 * Controlador del formulario de login.
 * Coordina validación, autenticación y navegación al dashboard.
 */
public class LoginController {

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

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Por favor, ingresa usuario y contraseña.");
            return;
        }

        botonIngresar.setDisable(true);

        try {
            Optional<Usuario> resultado = authService.autenticar(new LoginRequest(usuario, contrasena));

            if (resultado.isEmpty()) {
                bitacoraService.registrar(usuario, "LOGIN_FALLIDO",
                        "Credenciales inválidas para: " + usuario);
                mostrarError("Usuario o contraseña incorrectos.");
                campoContrasena.clear();
                campoContrasena.requestFocus();
                return;
            }

            Usuario autenticado = resultado.get();
            bitacoraService.registrar(usuario, "LOGIN",
                    "Sesión iniciada — Rol: " + autenticado.getRol());

            abrirDashboard(autenticado);

        } finally {
            // Siempre rehabilitar el botón (en caso de excepción imprevista)
            botonIngresar.setDisable(false);
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