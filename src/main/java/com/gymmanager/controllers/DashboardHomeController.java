package com.gymmanager.controllers;
import com.gymmanager.models.Usuario;
import com.gymmanager.services.ProductoService;
import com.gymmanager.services.PagoService;
import com.gymmanager.services.SocioService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Panel home del dashboard. Muestra 5 KPIs en tiempo real.
 * Se refresca cada vez que el usuario navega a este módulo.
 * La tarjeta de ingresos solo es visible para ADMIN.
 */
public class DashboardHomeController {

    @FXML private Label lblSociosActivos;
    @FXML private Label lblVencidos;
    @FXML private Label lblPorVencer;
    @FXML private Label lblIngresos;
    @FXML private Label lblStock;
    @FXML private Label lblMes;
    @FXML private VBox  cardIngresos;

    /** Configura la vista según el rol y carga los KPIs. */
    public void inicializar(Usuario usuario) {
        boolean esAdmin = usuario.esAdmin();
        cardIngresos.setVisible(esAdmin);
        cardIngresos.setManaged(esAdmin); // libera el espacio en el HBox
        cargarKPIs(esAdmin);
    }

    /**
     * Carga todos los KPIs desde los servicios.
     * Cada métrica falla de forma independiente para no bloquear las demás.
     * Los ingresos solo se consultan si el usuario es ADMIN.
     */
    private void cargarKPIs(boolean esAdmin) {
        LocalDate hoy = LocalDate.now();
        lblMes.setText(
                hoy.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "MX"))
                        + " " + hoy.getYear()
        );

        try {
            lblSociosActivos.setText(
                    String.valueOf(SocioService.getInstance().listarActivos().size()));
        } catch (SQLException e) {
            lblSociosActivos.setText("—");
        }

        try {
            lblVencidos.setText(
                    String.valueOf(SocioService.getInstance().listarVencidos().size()));
        } catch (SQLException e) {
            lblVencidos.setText("—");
        }

        try {
            lblPorVencer.setText(
                    String.valueOf(SocioService.getInstance().listarPorVencer(7).size()));
        } catch (SQLException e) {
            lblPorVencer.setText("—");
        }

        if (esAdmin) {
            try {
                double total = PagoService.getInstance().totalMesActual();
                lblIngresos.setText(String.format("$%,.0f", total));
            } catch (SQLException e) {
                lblIngresos.setText("—");
            }
        }

        // Stock preparado para Fase 7 — siempre 0 por ahora
        lblStock.setText(String.valueOf(ProductoService.getInstance().listarStockBajo().size()));
    }
}