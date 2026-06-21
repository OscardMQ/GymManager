package com.gymmanager.dao;

import com.gymmanager.models.DetalleVenta;
import com.gymmanager.models.Venta;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de acceso a datos para ventas.
 */
public interface VentaDAO {

    /**
     * Persiste una venta con sus detalles y descuenta el stock de los productos
     * involucrados. Todo ocurre en una sola transacción.
     *
     * @return la misma Venta con el id generado asignado
     */
    Venta guardar(Venta venta, List<DetalleVenta> detalles) throws SQLException;

    /**
     * Retorna las ventas registradas en los últimos {@code dias} días,
     * ordenadas de más reciente a más antigua.
     */
    List<Venta> listarRecientes(int dias) throws SQLException;
}