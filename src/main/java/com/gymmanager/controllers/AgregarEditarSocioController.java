package com.gymmanager.controllers;

import com.gymmanager.dao.MembresiaDAOImpl;
import com.gymmanager.models.Membresia;
import com.gymmanager.models.Socio;
import com.gymmanager.models.Usuario;
import com.gymmanager.services.BitacoraService;
import com.gymmanager.services.SocioService;
import com.gymmanager.utils.FechaUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class AgregarEditarSocioController {

    @FXML private Label               etiquetaTitulo;
    @FXML private TextField           campoNombre;
    @FXML private TextField           campoTelefono;
    @FXML private ComboBox<Membresia> comboMembresia;
    @FXML private DatePicker          fechaInicio;
    @FXML private TextField           campoFechaFin;
    @FXML private Label               etiquetaError;
    @FXML private Button              btnGuardar;
    @FXML private Button              btnCancelar;
    @FXML private Label               etiquetaPrecio;

    private Socio socioEditando;
    private Usuario usuarioActual;
    private final SocioService socioService = SocioService.getInstance();

    public void inicializar(Socio socio, Usuario usuario) {
        this.socioEditando = socio;
        this.usuarioActual = usuario;
        cargarMembresias();
        configurarFechaFin();

        if (socio != null) {
            etiquetaTitulo.setText("Editar Socio");
            campoNombre.setText(socio.getNombre());
            campoTelefono.setText(socio.getTelefono() != null ? socio.getTelefono() : "");
            seleccionarMembresiaPorId(socio.getTipoMembresia());
            if (socio.getFechaInicio() != null && !socio.getFechaInicio().isBlank())
                fechaInicio.setValue(LocalDate.parse(socio.getFechaInicio()));
        } else {
            etiquetaTitulo.setText("Nuevo Socio");
            fechaInicio.setValue(LocalDate.now());
        }
        limpiarError();
    }

    private void cargarMembresias() {
        try {
            List<Membresia> lista = new MembresiaDAOImpl().listar();
            comboMembresia.getItems().setAll(lista);
        } catch (SQLException e) {
            mostrarError("Error al cargar membresías: " + e.getMessage());
        }
    }

    private void configurarFechaFin() {
        comboMembresia.valueProperty().addListener((obs, v, n) -> recalcularFechaFin());
        fechaInicio.valueProperty().addListener((obs, v, n)   -> recalcularFechaFin());
    }

    private void recalcularFechaFin() {
        Membresia m = comboMembresia.getValue();
        LocalDate inicio = fechaInicio.getValue();
        if (m == null || inicio == null) { campoFechaFin.setText(""); etiquetaPrecio.setText(""); return; }
        LocalDate fin = FechaUtils.calcularFechaFin(inicio, m.getDuracionDias());
        campoFechaFin.setText(FechaUtils.aFormatoDisplay(fin.toString()));
        etiquetaPrecio.setText(String.format("Precio: $%.0f", m.getPrecio()));
    }

    private void seleccionarMembresiaPorId(int id) {
        comboMembresia.getItems().stream()
                .filter(m -> m.getId() == id).findFirst()
                .ifPresent(comboMembresia::setValue);
    }

    @FXML
    private void handleGuardar() {
        limpiarError();
        Membresia membresia = comboMembresia.getValue();
        LocalDate inicio    = fechaInicio.getValue();

        if (campoNombre.getText().isBlank()) { mostrarError("El nombre es obligatorio."); return; }
        if (membresia == null)               { mostrarError("Selecciona una membresía."); return; }
        if (inicio == null)                  { mostrarError("Selecciona la fecha de inicio."); return; }

        Socio socio = (socioEditando != null) ? socioEditando : new Socio();
        socio.setNombre(campoNombre.getText().trim());
        socio.setTelefono(campoTelefono.getText().trim());
        socio.setTipoMembresia(membresia.getId());
        socio.setFechaInicio(inicio.toString());
        socio.setFechaFin(FechaUtils.calcularFechaFin(inicio, membresia.getDuracionDias()).toString());
        socio.setActivo(true);

        try {
            if (socioEditando == null) {
                socioService.alta(socio);
                BitacoraService.getInstance().registrar(
                        usuarioActual.getUsuario(), "ALTA_SOCIO", "Alta de " + socio.getNombre());
            } else {
                socioService.actualizar(socio);
                BitacoraService.getInstance().registrar(
                        usuarioActual.getUsuario(), "EDICION_SOCIO", "Edición de " + socio.getNombre());
            }
            cerrarModal();
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (SQLException e) {
            if (socioEditando == null && socio.getId() > 0) {
                // El socio SÍ quedó guardado (falló el pago): cerrar el modal
                // para que un reintento no lo duplique, y avisar con Alert.
                cerrarModal();
                Alert alerta = new Alert(Alert.AlertType.WARNING);
                alerta.setTitle("Socio guardado con advertencia");
                alerta.setHeaderText(null);
                alerta.setContentText(e.getMessage());
                alerta.showAndWait();
            } else {
                mostrarError("Error de base de datos: " + e.getMessage());
            }
        }
    }

    @FXML private void handleCancelar() { cerrarModal(); }

    private void cerrarModal() { ((Stage) btnCancelar.getScene().getWindow()).close(); }

    private void mostrarError(String msg) {
        etiquetaError.setText(msg);
        etiquetaError.setVisible(true);
        etiquetaError.setManaged(true);
    }

    private void limpiarError() {
        etiquetaError.setText("");
        etiquetaError.setVisible(false);
        etiquetaError.setManaged(false);
    }
}