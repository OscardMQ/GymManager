package com.gymmanager.controllers;

import com.gymmanager.services.PagoService;
import com.gymmanager.services.SocioService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Panel home del dashboard. Muestra 5 KPIs en tiempo real.
 * Se refresca cada vez que el usuario navega a este módulo.
 */
public class DashboardHomeController {

    @FXML private Label lblSociosActivos;
    @FXML private Label lblVencidos;
    @FXML private Label lblPorVencer;
    @FXML private Label lblIngresos;
    @FXML private Label lblStock;
    @FXML private Label lblMes;

    /**
     * Carga todos los KPIs desde los servicios.
     * Cada métrica falla de forma independiente para no bloquear las demás.
     */
    public void cargarKPIs() {
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

        try {
            double total = PagoService.getInstance().totalMesActual();
            lblIngresos.setText(String.format("$%,.0f", total));
        } catch (SQLException e) {
            lblIngresos.setText("—");
        }

        // Stock preparado para Fase 7 — siempre 0 por ahora
        lblStock.setText("0");
    }
}