package com.gymmanager.controllers;

import com.gymmanager.models.Usuario;
import com.gymmanager.services.EmpleadoService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Controlador del modal de alta/edición de empleados.
 * En modo edición la contraseña es opcional:
 * si se deja en blanco se conserva la contraseña actual.
 */
public class AgregarEditarEmpleadoController {

    @FXML private Label         lblTitulo;
    @FXML private TextField     txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private PasswordField txtConfirmarContrasena;
    @FXML private Label         lblContrasenaHint;
    @FXML private Label         lblError;
    @FXML private Button        btnGuardar;

    private final EmpleadoService empleadoService = EmpleadoService.getInstance();

    /** Empleado siendo editado; null si el modal está en modo creación. */
    private Usuario empleadoEditando;

    /** Nombre del usuario en sesión, para registrar en bitácora. */
    private String realizadoPor;

    // ─── Inicialización ───────────────────────────────────────────────────────

    /**
     * Prepara el modal.
     * @param empleado     null → modo creación, Usuario → modo edición.
     * @param realizadoPor Nombre del administrador en sesión.
     */
    public void inicializar(Usuario empleado, String realizadoPor) {
        this.empleadoEditando = empleado;
        this.realizadoPor     = realizadoPor;

        ocultarError();

        if (empleado != null) {
            // Modo edición: precargar datos y hacer la contraseña opcional
            lblTitulo.setText("Editar Empleado");
            txtUsuario.setText(empleado.getUsuario());
            lblContrasenaHint.setVisible(true);
        } else {
            lblTitulo.setText("Nuevo Empleado");
            lblContrasenaHint.setVisible(false);
        }
    }

    // ─── Acciones ─────────────────────────────────────────────────────────────

    @FXML
    private void guardar() {
        ocultarError();

        String usuario      = txtUsuario.getText().trim();
        String contrasena   = txtContrasena.getText();
        String confirmacion = txtConfirmarContrasena.getText();

        // Validar coincidencia de contraseñas (siempre al crear, solo si se llenó al editar)
        boolean cambiaContrasena = !contrasena.isBlank();
        if (empleadoEditando == null || cambiaContrasena) {
            if (!contrasena.equals(confirmacion)) {
                mostrarError("Las contraseñas no coinciden.");
                return;
            }
        }

        try {
            if (empleadoEditando == null) {
                empleadoService.crearEmpleado(usuario, contrasena, realizadoPor);
            } else {
                // null → EmpleadoService conserva la contraseña actual
                String nuevaPass = cambiaContrasena ? contrasena : null;
                empleadoService.editarEmpleado(
                        empleadoEditando.getId(), usuario, nuevaPass, realizadoPor);
            }
            cerrar();

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void cancelar() {
        cerrar();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

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