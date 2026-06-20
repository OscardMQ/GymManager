package com.gymmanager.dao;

import com.gymmanager.models.Pago;
import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de acceso a datos para pagos.
 */
public interface PagoDAO {

    /** Persiste un pago y asigna el ID generado al objeto. */
    void guardar(Pago pago) throws SQLException;

    /** Devuelve todos los pagos del mes indicado (1-12). */
    List<Pago> listarDelMes(int anio, int mes) throws SQLException;
}