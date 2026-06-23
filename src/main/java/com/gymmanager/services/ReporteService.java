package com.gymmanager.services;

import com.gymmanager.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio Singleton para la generación de reportes y ganancias.
 *
 * Agrega SIEMPRE las dos fuentes de ingreso del negocio:
 *   - Tabla "pagos"   → ingresos por membresías
 *   - Tabla "ventas"  → ingresos del Punto de Venta (Fase 8)
 *
 * Patrón de acceso a BD: getConnection() se llama dentro de cada método,
 * nunca se guarda como campo (evita el bug de "connection closed").
 */
public class ReporteService {

    private static ReporteService instancia;

    private ReporteService() {}

    public static ReporteService getInstance() {
        if (instancia == null) {
            instancia = new ReporteService();
        }
        return instancia;
    }

    // =========================================================================
    // GANANCIAS — combinan pagos (membresías) + ventas (POS) via UNION ALL
    // =========================================================================

    /**
     * Ganancias totales agrupadas por día para un mes y año específicos.
     * Suma monto de pagos de membresías + total de ventas POS del mismo período.
     *
     * @param mes  número de mes (1 = enero … 12 = diciembre)
     * @param anio año de cuatro dígitos
     * @return lista de double[] donde [0] = día del mes, [1] = total acumulado
     */
    public List<double[]> gananciasPorDia(int mes, int anio) {
        List<double[]> resultado = new ArrayList<>();
        // strftime('%m', ...) en SQLite retorna "01"-"12", se necesita cero a la izquierda
        String mesFmt  = String.format("%02d", mes);
        String anioFmt = String.valueOf(anio);

        String sql = """
                SELECT dia, SUM(total) AS total
                FROM (
                    -- Ingresos por membresías
                    SELECT CAST(strftime('%d', fecha) AS INTEGER) AS dia,
                           monto AS total
                    FROM pagos
                    WHERE strftime('%m', fecha) = ?
                      AND strftime('%Y', fecha) = ?
                    UNION ALL
                    -- Ingresos del Punto de Venta
                    SELECT CAST(strftime('%d', fecha) AS INTEGER) AS dia,
                           total
                    FROM ventas
                    WHERE strftime('%m', fecha) = ?
                      AND strftime('%Y', fecha) = ?
                )
                GROUP BY dia
                ORDER BY dia
                """;

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, mesFmt);
            ps.setString(2, anioFmt);
            ps.setString(3, mesFmt);
            ps.setString(4, anioFmt);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultado.add(new double[]{ rs.getDouble("dia"), rs.getDouble("total") });
            }
        } catch (SQLException e) {
            System.err.println("[ReporteService] gananciasPorDia: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Ganancias totales agrupadas por semana del año (strftime('%W')).
     * La semana 00 agrupa los días anteriores al primer lunes del año.
     *
     * @param anio año de cuatro dígitos
     * @return lista de double[] donde [0] = número de semana, [1] = total acumulado
     */
    public List<double[]> gananciasPorSemana(int anio) {
        List<double[]> resultado = new ArrayList<>();
        String anioFmt = String.valueOf(anio);

        String sql = """
                SELECT semana, SUM(total) AS total
                FROM (
                    SELECT CAST(strftime('%W', fecha) AS INTEGER) AS semana,
                           monto AS total
                    FROM pagos
                    WHERE strftime('%Y', fecha) = ?
                    UNION ALL
                    SELECT CAST(strftime('%W', fecha) AS INTEGER) AS semana,
                           total
                    FROM ventas
                    WHERE strftime('%Y', fecha) = ?
                )
                GROUP BY semana
                ORDER BY semana
                """;

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, anioFmt);
            ps.setString(2, anioFmt);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultado.add(new double[]{ rs.getDouble("semana"), rs.getDouble("total") });
            }
        } catch (SQLException e) {
            System.err.println("[ReporteService] gananciasPorSemana: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Ganancias totales agrupadas por mes de un año dado.
     *
     * @param anio año de cuatro dígitos
     * @return lista de double[] donde [0] = número de mes (1-12), [1] = total acumulado
     */
    public List<double[]> gananciasPorMes(int anio) {
        List<double[]> resultado = new ArrayList<>();
        String anioFmt = String.valueOf(anio);

        String sql = """
                SELECT mes, SUM(total) AS total
                FROM (
                    SELECT CAST(strftime('%m', fecha) AS INTEGER) AS mes,
                           monto AS total
                    FROM pagos
                    WHERE strftime('%Y', fecha) = ?
                    UNION ALL
                    SELECT CAST(strftime('%m', fecha) AS INTEGER) AS mes,
                           total
                    FROM ventas
                    WHERE strftime('%Y', fecha) = ?
                )
                GROUP BY mes
                ORDER BY mes
                """;

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, anioFmt);
            ps.setString(2, anioFmt);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultado.add(new double[]{ rs.getDouble("mes"), rs.getDouble("total") });
            }
        } catch (SQLException e) {
            System.err.println("[ReporteService] gananciasPorMes: " + e.getMessage());
        }
        return resultado;
    }

    // =========================================================================
    // PRODUCTOS
    // =========================================================================

    /**
     * Top N productos más vendidos por unidades, de mayor a menor.
     *
     * @param limite cantidad máxima de filas a retornar
     * @return lista de String[] { nombre, cantidadTotal }
     */
    public List<String[]> productosMasVendidos(int limite) {
        List<String[]> resultado = new ArrayList<>();

        String sql = """
                SELECT p.nombre, SUM(dv.cantidad) AS cantidadTotal
                FROM detalle_ventas dv
                JOIN productos p ON p.id = dv.producto_id
                GROUP BY p.id, p.nombre
                ORDER BY SUM(dv.cantidad) DESC
                LIMIT ?
                """;

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultado.add(new String[]{
                        rs.getString("nombre"),
                        String.valueOf(rs.getInt("cantidadTotal"))
                });
            }
        } catch (SQLException e) {
            System.err.println("[ReporteService] productosMasVendidos: " + e.getMessage());
        }
        return resultado;
    }

    // =========================================================================
    // HISTORIAL
    // =========================================================================

    /**
     * Historial completo de pagos de membresías, del más reciente al más antiguo.
     *
     * @return lista de String[] { fecha, nombreSocio, monto, nombreMembresia }
     */
    public List<String[]> historialPagosMembresia() {
        List<String[]> resultado = new ArrayList<>();

        String sql = """
                SELECT p.fecha,
                       s.nombre AS nombreSocio,
                       p.monto,
                       m.nombre AS nombreMembresia
                FROM pagos p
                JOIN socios     s ON s.id = p.socio_id
                JOIN membresias m ON m.id = p.tipo_membresia_id
                ORDER BY p.fecha DESC
                """;

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultado.add(new String[]{
                        rs.getString("fecha"),
                        rs.getString("nombreSocio"),
                        // Formatear con 2 decimales para que el controller solo anteponga "$"
                        String.format("%.2f", rs.getDouble("monto")),
                        rs.getString("nombreMembresia")
                });
            }
        } catch (SQLException e) {
            System.err.println("[ReporteService] historialPagosMembresia: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Historial completo de ventas del POS, del más reciente al más antiguo.
     * El ID de la venta se incluye en la posición [0] (no se muestra en la tabla
     * pero el controlador lo usa para cargar el detalle al hacer doble clic).
     *
     * @return lista de String[] { id, fecha, hora, usuario, total }
     */
    public List<String[]> historialVentas() {
        List<String[]> resultado = new ArrayList<>();

        String sql = """
                SELECT id, fecha, hora, usuario, total
                FROM ventas
                ORDER BY fecha DESC, hora DESC
                """;

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultado.add(new String[]{
                        String.valueOf(rs.getInt("id")),               // [0] oculto
                        rs.getString("fecha"),                         // [1]
                        rs.getString("hora"),                          // [2]
                        rs.getString("usuario"),                       // [3]
                        String.format("%.2f", rs.getDouble("total"))   // [4]
                });
            }
        } catch (SQLException e) {
            System.err.println("[ReporteService] historialVentas: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Detalle de los productos incluidos en una venta específica.
     * Usado por el diálogo de doble clic en la tabla de ventas POS.
     *
     * @param ventaId ID de la fila en la tabla "ventas"
     * @return lista de String[] { nombreProducto, cantidad, precioUnitario, subtotal }
     */
    public List<String[]> detalleVenta(int ventaId) {
        List<String[]> resultado = new ArrayList<>();

        String sql = """
                SELECT p.nombre,
                       dv.cantidad,
                       dv.precio_unitario,
                       dv.cantidad * dv.precio_unitario AS subtotal
                FROM detalle_ventas dv
                JOIN productos p ON p.id = dv.producto_id
                WHERE dv.venta_id = ?
                """;

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultado.add(new String[]{
                        rs.getString("nombre"),
                        String.valueOf(rs.getInt("cantidad")),
                        String.format("%.2f", rs.getDouble("precio_unitario")),
                        String.format("%.2f", rs.getDouble("subtotal"))
                });
            }
        } catch (SQLException e) {
            System.err.println("[ReporteService] detalleVenta(" + ventaId + "): " + e.getMessage());
        }
        return resultado;
    }
}