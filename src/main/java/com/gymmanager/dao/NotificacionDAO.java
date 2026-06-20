package com.gymmanager.dao;

import com.gymmanager.models.Notificacion;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de persistencia para el historial de notificaciones WhatsApp.
 */
public interface NotificacionDAO {

    /** Persiste una nueva notificación y asigna el ID generado al objeto. */
    void guardar(Notificacion notificacion) throws SQLException;

    /** Historial completo de un socio, más reciente primero. */
    List<Notificacion> listarPorSocio(int socioId) throws SQLException;

    /** Notificaciones de los últimos N días, más reciente primero. */
    List<Notificacion> listarRecientes(int dias) throws SQLException;
}