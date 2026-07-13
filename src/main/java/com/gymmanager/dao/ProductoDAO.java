package com.gymmanager.dao;

import com.gymmanager.models.Producto;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia para productos del inventario.
 * Las escrituras lanzan SQLException: un fallo debe llegar a la UI,
 * nunca quedar solo en consola.
 */
public interface ProductoDAO {
    List<Producto>    listar();
    Optional<Producto> buscarPorId(int id);
    void              guardar(Producto producto) throws SQLException;
    void              actualizar(Producto producto) throws SQLException;
    void              eliminar(int id) throws SQLException;
    /** Retorna productos donde stock &lt;= stock_minimo, ordenados por stock ASC. */
    List<Producto>    listarStockBajo();
}